package com.tamingtext.classifier.maxent;

import java.io.File;
import java.io.IOException;

import opennlp.maxent.GISModel;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DocumentCategorizerEventStream;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.slf4j.Logger;

import com.tamingtext.util.FileUtil;

public class TrainMaxent {
  
  public static final Logger log = 
    org.slf4j.LoggerFactory.getLogger(TrainMaxent.class);

  Tokenizer tokenizer; 
  
  public TrainMaxent() {
    this(null);
  }
  
  public TrainMaxent(Tokenizer tokenizer) {
    if (tokenizer == null) 
      this.tokenizer = new SimpleTokenizer();
    
  }
  public void train(String source, String destination) throws IOException {
//<start id="maxent.examples.train.setup"/> 
    File[] inputFiles = FileUtil.buildFileList(new File(source));
    File modelFile = new File(destination);
    
    Tokenizer tokenizer = new SimpleTokenizer(); //<co id="tm.tok"/>
    CategoryDataStream ds = new CategoryDataStream(inputFiles, tokenizer);
    
    NameFinderFeatureGenerator nffg //<co id="tm.fg"/>
      = new NameFinderFeatureGenerator();
    BagOfWordsFeatureGenerator bowfg 
      = new BagOfWordsFeatureGenerator();
    FeatureGenerator[] gens = new FeatureGenerator[2];
    gens[0] = nffg; gens[1] = bowfg;
    DocumentCategorizerEventStream es 
      = new DocumentCategorizerEventStream(ds, gens);
    
    GISModel model = DocumentCategorizerME.train(es);//<co id="tm.train"/>
    new SuffixSensitiveGISModelWriter(model, modelFile).persist();
    
/*<calloutlist>
<callout arearefs="tm.tok">Setup Data Stream</callout>
<callout arearefs="tm.fg">Setup Event Stream</callout> 
<callout arearefs="tm.train">Train Categorizer</callout>  
</calloutlist>*/
//<end id="maxent.examples.train.setup"/>
  }

  public static void main(String[] args) throws Exception {

    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    
    Option helpOpt = DefaultOptionCreator.helpOption();
    
    Option inputDirOpt = obuilder.withLongName("input").withRequired(true).withArgument(
      abuilder.withName("input").withMinimum(1).withMaximum(1).create()).withDescription(
      "The input directory, containing properly formatted files: "
          + "One doc per line, first entry on the line is the label, rest is the evidence")
        .withShortName("i").create();
    
    Option outputOpt = obuilder.withLongName("output").withRequired(true).withArgument(
      abuilder.withName("output").withMinimum(1).withMaximum(1).create()).withDescription(
      "The output directory").withShortName("o").create();
    
    
    Group group = gbuilder.withName("Options").withOption(helpOpt).withOption(
        inputDirOpt).withOption(outputOpt).create();
    
    //.withOption(gramSizeOpt).withOption(typeOpt)
    
    try {
      Parser parser = new Parser();
      
      parser.setGroup(group);
      parser.setHelpOption(helpOpt);
      CommandLine cmdLine = parser.parse(args);
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return;
      }
      
      String inputPath  = (String) cmdLine.getValue(inputDirOpt);
      String outputPath = (String) cmdLine.getValue(outputOpt);
      TrainMaxent trainer = new TrainMaxent();
      trainer.train(inputPath, outputPath);
    }
    catch (OptionException e) {
      log.error("Error while parsing options", e);
    }
  }
}
