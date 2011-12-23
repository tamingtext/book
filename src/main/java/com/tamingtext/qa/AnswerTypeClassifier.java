package com.tamingtext.qa;

import java.io.File;
import java.io.IOException;

import loci.formats.Location;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.MaxentModel;
import opennlp.maxent.TwoPassDataIndexer;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.tools.lang.english.ParserTagger;
import opennlp.tools.lang.english.TreebankChunker;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;

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

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Usage: AnswerTypeClassifier trainFile modelFile");
      System.exit(1);
    }
    String trainFile = args[0];
    File outFile = new File(args[1]);
    String modelsDirProp = System.getProperty("model.dir");
    File modelsDir = new File(new File(modelsDirProp), "english");
    File parserDir = new File(modelsDir, "chunker");
    String wordnetDir = System.getProperty("wordnet.dir", "book/src/main" + File.separator + "WordNet-3.0"
            + File.separator + "dict");
    TreebankChunker chunker = new TreebankChunker(parserDir.getAbsolutePath()
            + File.separator + "EnglishChunk.bin.gz");
    File posDir = new File(modelsDir, "postag");
    ParserTagger tagger =  new ParserTagger(posDir.getAbsolutePath() + File.separator + "tag.bin.gz",
            posDir.getAbsolutePath() + File.separator + "tagdict", true);
    Parser parser = new ChunkParser(chunker, tagger);
    AnswerTypeContextGenerator actg = new AnswerTypeContextGenerator(new File(wordnetDir));
    //<start id="atc.train"/>
    AnswerTypeEventStream es = new AnswerTypeEventStream(trainFile, actg, parser);
    GISModel model = GIS.trainModel(100, new TwoPassDataIndexer(es, 3));//<co id="atc.train.do"/>
    new SuffixSensitiveGISModelWriter(model, outFile).persist();
    /*
    <calloutlist>
        <callout arearefs="atc.train.do"><para>Using the event stream, which feeds us training examples, do the actual training using OpenNLP's Maxent classifier.</para></callout>
    </calloutlist>
    */
    //<end id="atc.train"/>
  }
}
