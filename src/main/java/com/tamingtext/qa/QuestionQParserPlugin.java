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

package com.tamingtext.qa;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.model.MaxentModel;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.parser.Parser;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

/**
 *
 *
 **/
public class QuestionQParserPlugin extends QParserPlugin {

  private Map<String, String> answerTypeMap;
  protected MaxentModel model;
  protected double[] probs;
  protected AnswerTypeContextGenerator atcg;
  private POSTaggerME tagger;
  private ChunkerME chunker;

  //<start id="qqpp.create"/>
  @Override
  public QParser createParser(String qStr, SolrParams localParams, SolrParams params,
                              SolrQueryRequest req) {
    answerTypeMap = new HashMap<String, String>();//<co id="qqpp.atm"/>
    answerTypeMap.put("L", "NE_LOCATION");
    answerTypeMap.put("T", "NE_TIME|NE_DATE");
    answerTypeMap.put("P", "NE_PERSON");
    answerTypeMap.put("O", "NE_ORGANIZATION");
    QParser qParser;
    if (params.getBool(QAParams.COMPONENT_NAME, false) == true //<co id="qqpp.explainif"/>
            && qStr.equals("*:*") == false) {
      AnswerTypeClassifier atc =
              new AnswerTypeClassifier(model, probs, atcg);//<co id="qqpp.atc"/>
      Parser parser = new ChunkParser(chunker, tagger);//<co id="qqpp.parser"/>
      qParser = new QuestionQParser(qStr, localParams, //<co id="qqpp.construct"/>
              params, req, parser, atc, answerTypeMap);
    } else {
      //just do a regular query if qa is turned off
      qParser = req.getCore().getQueryPlugin("edismax")
              .createParser(qStr, localParams, params, req);
    }
    return qParser;
  }
  /*
  <calloutlist>
      <callout arearefs="qqpp.atm"><para>Construct a map of the answer types that we are interested in handling, for instance locations, people and times and dates.</para></callout>
      <callout arearefs="qqpp.explainif"><para>We use this if clause to create an regular Solr query parser in the cases where the user hasn't entered a question or the enter the *:* query (<classname>MatchAllDocsQuery</classname>.</para></callout>
      <callout arearefs="qqpp.atc"><para>The <classname>AnswerTypeClassifier</classname> uses the trained Answer Type model (located in the models directory) to classify the question.</para></callout>
      <callout arearefs="qqpp.parser"><para>Construct the chunker (parser) that will be responsible for parsing the user question.</para></callout>
      <callout arearefs="qqpp.construct"><para>Create the <classname>QuestionQParser</classname> by passing in the user's question as well as the pre-initialized resources from the init method.</para></callout>
  </calloutlist>
  */
  //<end id="qqpp.create"/>

  //<start id="qqpp.init"/>
  public void init(NamedList initArgs) {
    SolrParams params = SolrParams.toSolrParams(initArgs);
    String modelDirectory = params.get("modelDirectory",
            System.getProperty("model.dir"));//<co id="qqpp.model"/>
    String wordnetDirectory = params.get("wordnetDirectory",
            System.getProperty("wordnet.dir"));//<co id="qqpp.wordnet"/>
    if (modelDirectory != null) {
      File modelsDir = new File(modelDirectory);
      try {
        InputStream chunkerStream = new FileInputStream(
            new File(modelsDir,"en-chunker.bin"));
        ChunkerModel chunkerModel = new ChunkerModel(chunkerStream);
        chunker = new ChunkerME(chunkerModel); //<co id="qqpp.chunker"/>
        InputStream posStream = new FileInputStream(
            new File(modelsDir,"en-pos-maxent.bin"));
        POSModel posModel = new POSModel(posStream);
        tagger =  new POSTaggerME(posModel); //<co id="qqpp.tagger"/>
        model = new DoccatModel(new FileInputStream( //<co id="qqpp.theModel"/>
            new File(modelDirectory,"en-answer.bin")))
            .getChunkerModel();
        probs = new double[model.getNumOutcomes()];
        atcg = new AnswerTypeContextGenerator(
                new File(wordnetDirectory, "dict"));//<co id="qqpp.context"/>
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  /*
  <calloutlist>
      <callout arearefs="qqpp.model"><para>The model directory contains all of the OpenNLP models that we use throughout the book.</para></callout>
      <callout arearefs="qqpp.wordnet"><para>WordNet is a lexical resource used to assist in the Answer Type identification process.</para></callout>
      <callout arearefs="qqpp.chunker"><para>The Treebank Chunker works with a Parser to do shallow parsing of questions</para></callout>
      <callout arearefs="qqpp.tagger"><para>The tagger is responsible for Part of Speech Tagging</para></callout>
      <callout arearefs="qqpp.theModel"><para>Create the actual model and save it for reuse, as it is thread safe, but the containing class is not.</para></callout>
      <callout arearefs="qqpp.context"><para>Create the AnswerTypeContextGenerator, which is responsible for feature selection.</para></callout>

  </calloutlist>
  */
  //<end id="qqpp.init"/>
}
