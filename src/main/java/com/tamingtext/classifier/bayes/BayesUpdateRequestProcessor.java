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
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.mahout.classifier.ClassifierResult;
import org.apache.mahout.classifier.bayes.ClassifierContext;
import org.apache.mahout.classifier.bayes.InvalidDatastoreException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

/** A Solr <code>UpdateRequestProcessor</code> that uses the Mahout Bayes
 *  Classifier to add a category label to documents at index time.
 *  <p/>
 *  @see com.tamingtext.classifier.bayes.BayesUpdateRequestProcessorFactory
 *
 */
public class BayesUpdateRequestProcessor extends UpdateRequestProcessor {

  public static final String NO_LABEL = "nullDefault";
  
  ClassifierContext ctx;
  String inputField;
  String outputField;
  String defaultCategory;
  Analyzer analyzer;
  
  public BayesUpdateRequestProcessor(ClassifierContext ctx, Analyzer analyzer, 
      String inputField, String outputField, String defaultCategory, UpdateRequestProcessor next) {
    super(next);
    this.ctx = ctx;
    this.analyzer = analyzer;
    this.inputField = inputField;
    this.outputField = outputField;
    this.defaultCategory = defaultCategory;
    
    if (this.defaultCategory == null) {
      this.defaultCategory = NO_LABEL;
    }
  }

  @Override
  public void processAdd(AddUpdateCommand cmd) throws IOException {
    SolrInputDocument doc = cmd.getSolrInputDocument();
    classifyDocument(doc);
    super.processAdd(cmd);
  }

  public void classifyDocument(SolrInputDocument doc) throws IOException {
    try {
      //<start id="mahout.bayes.classify"/>
      SolrInputField field = doc.getField(inputField);
      String[] tokens = tokenizeField(inputField, field);
      ClassifierResult result = ctx.classifyDocument(tokens,
              defaultCategory);
      if (result != null && result.getLabel() != NO_LABEL) {
        doc.addField(outputField, result.getLabel());
      }
      //<end id="mahout.bayes.classify"/>
    }
    catch (InvalidDatastoreException e) {
      throw new IOException("Invalid Classifier Datastore", e);
    }
  }
  
  public String[] tokenizeField(String fieldName, SolrInputField field) throws IOException {
    if (field == null) return new String[0];
    if (!(field.getValue() instanceof String)) return new String[0];
    //<start id="mahout.bayes.tokenize"/>
    String input = (String) field.getValue();
    
    ArrayList<String> tokenList = new ArrayList<String>();
    TokenStream ts = analyzer.tokenStream(inputField,
            new StringReader(input));
    while (ts.incrementToken()) {
      tokenList.add(ts.getAttribute(CharTermAttribute.class).toString());
    }
    String[] tokens = tokenList.toArray(new String[tokenList.size()]);
    //<end id="mahout.bayes.tokenize"/>
    return tokens;
  }
}
