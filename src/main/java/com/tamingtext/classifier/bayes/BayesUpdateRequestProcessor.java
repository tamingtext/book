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
import java.util.SortedSet;

import org.apache.mahout.classifier.ClassifierResult;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

/** A Solr <code>UpdateRequestProcessor</code> that uses the 
 *  TextClassifier to add a category label to documents at index time.
 *  <p/>
 *  @see com.tamingtext.classifier.bayes.BayesUpdateRequestProcessorFactory
 *
 */
public class BayesUpdateRequestProcessor extends UpdateRequestProcessor {

  final TextClassifier docClassifier;
  final String inputField;
  final String outputField;
  final String scoreField;

  public BayesUpdateRequestProcessor(
      TextClassifier docClassifier,
      String inputField, 
      String outputField,
      String scoreField,
      UpdateRequestProcessor next) {

    super(next);

    this.docClassifier = docClassifier;
    this.inputField = inputField;
    this.outputField = outputField;
    this.scoreField = scoreField;
  }

  @Override
  public void processAdd(AddUpdateCommand cmd) throws IOException {
    SolrInputDocument doc = cmd.getSolrInputDocument();
    SolrInputField field = doc.getField(inputField);
    StringReader reader = new StringReader((String) field.getFirstValue());
    SortedSet<ClassifierResult> results = docClassifier.classify(inputField, reader);
    ClassifierResult result = results.first();
    doc.addField(outputField, result.getLabel());
    doc.addField(scoreField,  result.getScore());
    super.processAdd(cmd);
  }
}
