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

import java.io.File;


import org.apache.mahout.classifier.bayes.Algorithm;
import org.apache.mahout.classifier.bayes.BayesAlgorithm;
import org.apache.mahout.classifier.bayes.BayesParameters;
import org.apache.mahout.classifier.bayes.ClassifierContext;
import org.apache.mahout.classifier.bayes.Datastore;
import org.apache.mahout.classifier.bayes.InMemoryBayesDatastore;
import org.apache.solr.common.SolrException;
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

  static final String DEFAULT_MODEL_DIR    = "data/bayes-model";
  static final String DEFAULT_INPUT_FIELD  = "text";
  static final String DEFAULT_OUTPUT_FIELD = "bayes-class";
  
  private SolrCore core;
  private String inputFieldName;
  private String outputFieldName;
  private String defaultCategory;
  private File modelDir;
  private ClassifierContext ctx;
  private boolean enabled;
  
  // used for locking during context swap.
  private final Object swapContext = new Object();
  
  public BayesUpdateRequestProcessorFactory(SolrCore core) {
    this.core = core;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void init(NamedList args) {
    super.init(args);
    Object o;
    
    enabled = Boolean.valueOf((String) args.get("enabled"));
  
    o = args.get("inputField");
    inputFieldName = DEFAULT_INPUT_FIELD;
    if (o != null && o instanceof String) {
      inputFieldName = (String) o;
    }
    
    o = args.get("outputField");
    outputFieldName = DEFAULT_OUTPUT_FIELD;
    if (o != null && o instanceof String) {
      outputFieldName = (String) o;
    }
    
    o = args.get("defaultCategory");
    if (o != null && o instanceof String) {
      defaultCategory = (String) o;
    }
    
    o = args.get("model");
    String modelDirName = DEFAULT_MODEL_DIR;
    if (o != null && o instanceof String) {
      modelDirName = (String) o;
    }
    
    modelDir = new File(modelDirName);
    
    if (!modelDir.isDirectory()) {
      log.warn("WARNING: model directory " + modelDir.getAbsolutePath() + " does not exist. Classification disabled");
      enabled = false;
    }
    
    initClassifierContext();
  }

  public void initClassifierContext() {
    try {
      //<start id="mahout.bayes.setup"/>
      BayesParameters p = new BayesParameters();
      p.set("basePath", modelDir.getCanonicalPath());
      Datastore ds = new InMemoryBayesDatastore(p);
      Algorithm a  = new BayesAlgorithm();
      ClassifierContext ctx = new ClassifierContext(a,ds);
      ctx.initialize();
      //<end id="mahout.bayes.setup"/>
      synchronized (swapContext) {
          this.ctx = ctx; // swap upon successful load.
      }
      enabled = true;
    }
    catch (Exception e) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,"Error initializing clasifier context", e);
    }
  }
  
  @Override
  public UpdateRequestProcessor getInstance(SolrQueryRequest req,
      SolrQueryResponse rsp, UpdateRequestProcessor next) {
    if (enabled) {
      synchronized (swapContext) {
        return new BayesUpdateRequestProcessor(ctx, core.getSchema().getAnalyzer(), inputFieldName, outputFieldName, 
            defaultCategory, next);
      }
    }
    else {
      return next;
    }
  }

}
