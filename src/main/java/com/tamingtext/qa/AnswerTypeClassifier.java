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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.model.MaxentModel;
import opennlp.model.TwoPassDataIndexer;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

public class AnswerTypeClassifier {

  
  private MaxentModel model;
  private double[] probs;
  private AnswerTypeContextGenerator atcg;

  public AnswerTypeClassifier(MaxentModel model, double[] probs, AnswerTypeContextGenerator atcg) {
    this.model = model;
    this.probs = probs;
    this.atcg = atcg;
  }


  //<start id="atc.compute"/>
  public String computeAnswerType(Parse question) {
    double[] probs = computeAnswerTypeProbs(question);//<co id="atc.getprobs"/>
    return model.getBestOutcome(probs);//<co id="atc.outcome"/>
  }

  public double[] computeAnswerTypeProbs(Parse question) {
    String[] context = atcg.getContext(question);//<co id="atc.context"/>
    return model.eval(context, probs);//<co id="atc.evaluate"/>
  }
  /*
  <calloutlist>
      <callout arearefs="atc.getprobs"><para>Get the probabilities of an Answer Type by calling computeAnswerTypeProbs</para></callout>
      <callout arearefs="atc.outcome"><para>Given the probabilities generated, ask the model for the best outcome is.  This is a simple calculation that finds the maximum probability in the array.</para></callout>
      <callout arearefs="atc.context"><para>Ask the <classname>AnswerTypeContextGenerator</classname> for the list of features, aka the "context", that should be predictive of the answer type.</para></callout>
      <callout arearefs="atc.evaluate"><para>Evaluate the generated features to determine the probabilities for the possible answer types</para></callout>

  </calloutlist>
  */
  //<end id="atc.compute"/>

  /** Train the answer model
   * <p>
   *  Hint:
   *  <pre>
   *  mvn exec:java -Dexec.mainClass=com.tamingtext.qa.AnswerTypeClassifier \
   *    -Dexec.args="dist/data/questions-train.txt en-answer.bin" \
   *    -Dmodel.dir=../../opennlp-models \
   *    -Dwordnet.dir=../../Wordnet-3.0/dict
   *  </pre>
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Usage: AnswerTypeClassifier trainFile modelFile");
      System.exit(1);
    }
    String trainFile = args[0];
    File outFile = new File(args[1]);
    String modelsDirProp = System.getProperty("model.dir");
    File modelsDir = new File(modelsDirProp);
    String wordnetDir = System.getProperty("wordnet.dir");
    InputStream chunkerStream = new FileInputStream(
        new File(modelsDir,"en-chunker.bin"));
    ChunkerModel chunkerModel = new ChunkerModel(chunkerStream);
    ChunkerME chunker = new ChunkerME(chunkerModel);
    InputStream posStream = new FileInputStream(
        new File(modelsDir,"en-pos-maxent.bin"));
    POSModel posModel = new POSModel(posStream);
    POSTaggerME tagger =  new POSTaggerME(posModel);
    Parser parser = new ChunkParser(chunker, tagger);
    AnswerTypeContextGenerator actg = new AnswerTypeContextGenerator(new File(wordnetDir));
    //<start id="atc.train"/>
    AnswerTypeEventStream es = new AnswerTypeEventStream(trainFile,
            actg, parser);
    GISModel model = GIS.trainModel(100, new TwoPassDataIndexer(es, 3));//<co id="atc.train.do"/>
    new DoccatModel("en", model).serialize(new FileOutputStream(outFile));
    /*
    <calloutlist>
        <callout arearefs="atc.train.do"><para>Using the event stream, which feeds us training examples, do the actual training using OpenNLP's Maxent classifier.</para></callout>
    </calloutlist>
    */
    //<end id="atc.train"/>
  }
}
