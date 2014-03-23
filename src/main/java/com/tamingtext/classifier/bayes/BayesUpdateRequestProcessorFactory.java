/*
 * Copyright 2008-2011 Grant Ingersoll, Thomas Morton and Drew Farris
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * -------------------
 * To purchase or learn more about Taming Text, by Grant Ingersoll, Thomas Morton and Drew Farris, visit
 * http://www.manning.com/ingersoll
 */

package com.tamingtext.classifier.bayes;

import java.io.IOException;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.slf4j.Logger;

/** A Solr <code>UpdateRequestProcessorFactory</code> that uses the Mahout Bayes
 *  Classifier to add a category label to documents at index time.
 *  <p>
 *  To use, add the following to solrconfig.xml
 * 
 * <pre>
 * &lt;updateRequestProcessorChain key=&quot;mahout&quot; default=&quot;true&quot;&gt;
 *  &lt;processor class=&quot;com.tamingtext.classifier.BayesUpdateRequestProcessorFactory&quot;&gt;
 *    &lt;str name=&quot;inputField&quot;&gt;details&lt;/str&gt;
 *    &lt;str name=&quot;outputField&quot;&gt;subject&lt;/str&gt;
 *    &lt;str name=&quot;model&quot;&gt;src/test/resources/classifier/bayes-model&lt;/str&gt;
 *  &lt;/processor&gt;
 *  &lt;processor class=&quot;solr.RunUpdateProcessorFactory&quot;/&gt;
 *  &lt;processor class=&quot;solr.LogUpdateProcessorFactory&quot;/&gt;
 * &lt;/updateRequestProcessorChain&gt;
 * </pre>
 * 
 * Text is read from the field named in the<code>inputField</code> parameter. It is processed with the Analyzer 
 * configured for that field type as specified in the schema. The Mahout Bayes Classifier is run on this tokenized text.
 * The category label it produces is written to the field named by the <code>outputField</code> parameter. 
 * 
 * By default if the category is unknown or the document is not categorizable, no category will be written. Alternately
 * a string parameter named <code>defaultCategory</code> can be provided to use as a category in the event the input
 * can not be categorized.
 * 
 * The <code>model</code> parameter points to the directory containing the bayes model produced by the Mahout
 * training process. {@link #initClassifierContext()} may be called to reload the model at any time.
 * 
 */
public class BayesUpdateRequestProcessorFactory extends UpdateRequestProcessorFactory {

  public static final Logger log = org.slf4j.LoggerFactory.getLogger(BayesUpdateRequestProcessorFactory.class);

  static final String DEFAULT_INPUT_FIELD  = "text";
  static final String DEFAULT_OUTPUT_FIELD = "bayes-class";
  static final String DEFAULT_SCORE_FIELD  = "bayes-score";
  
  static final String DEFAULT_MODEL_DIR    = "data/bayes-model";
  static final String DEFAULT_DICTIONARY_FILE = "data/bayes-dictionary.fst";
  static final String DEFAULT_LABEL_FILE = "data/bayes-labels";
  
  static final String DEFAULT_TERM_WEIGHT_METHOD = "tfidf";
  
  private final SolrCore core;
  
  private String inputFieldName = DEFAULT_INPUT_FIELD;
  private String outputFieldName = DEFAULT_OUTPUT_FIELD;
  private String scoreFieldName  = DEFAULT_SCORE_FIELD;
  private String defaultCategory = "";

  private String modelDirName = DEFAULT_MODEL_DIR;
  private String dictionaryName = DEFAULT_DICTIONARY_FILE;
  private String labelFileName = DEFAULT_LABEL_FILE;
  private String termWeightMethod = DEFAULT_TERM_WEIGHT_METHOD;
  
  TextClassifier docClassifier;
  
  // used for locking during context swap.
  private final Object swapContext = new Object();
  
  public BayesUpdateRequestProcessorFactory(SolrCore core) throws IOException {
    this.core = core;
  }
  
  @SuppressWarnings("rawtypes")
  @Override
  public void init(final NamedList args) {
    if (args != null) {
      SolrParams params = SolrParams.toSolrParams(args);
      inputFieldName   = params.get("inputField", DEFAULT_INPUT_FIELD);
      outputFieldName  = params.get("outputField", DEFAULT_OUTPUT_FIELD);
      scoreFieldName   = params.get("scoreField", DEFAULT_SCORE_FIELD);
      defaultCategory  = params.get("defaultCategory", "");
      modelDirName     = params.get("model", DEFAULT_MODEL_DIR);
      dictionaryName   = params.get("dictionary", DEFAULT_DICTIONARY_FILE);
      labelFileName    = params.get("labels", DEFAULT_LABEL_FILE);
      termWeightMethod = params.get("termWeightMethod", DEFAULT_TERM_WEIGHT_METHOD);
    }

    docClassifier = new TextClassifier();
    docClassifier.defaultCategory = defaultCategory;
    docClassifier.modelDirName = modelDirName;
    docClassifier.dictionaryName = dictionaryName;
    docClassifier.labelFileName = labelFileName;
    docClassifier.termWeightMethod = termWeightMethod;
    docClassifier.analyzer = core.getLatestSchema().getAnalyzer();
      
    try {
      docClassifier.init();
    }
    catch (IOException ex) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, ex.getMessage(), ex.getCause());
    }
  }

  @Override
  public UpdateRequestProcessor getInstance(SolrQueryRequest req,
      SolrQueryResponse rsp, UpdateRequestProcessor next) {
      synchronized (swapContext) {
        return new BayesUpdateRequestProcessor(
            docClassifier,
            inputFieldName, 
            outputFieldName,
            scoreFieldName,
            next);
      }
  }
}
