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

package com.tamingtext.classifier.maxent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
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
      this.tokenizer = SimpleTokenizer.INSTANCE;
    
  }
  public void train(String source, String destination) throws IOException {
    //<start id="maxent.examples.train.setup"/> 
    File[] inputFiles = FileUtil.buildFileList(new File(source));
    File modelFile = new File(destination);
    
    Tokenizer tokenizer = SimpleTokenizer.INSTANCE; //<co id="tm.tok"/>
    CategoryDataStream ds = new CategoryDataStream(inputFiles, tokenizer);

    int cutoff = 5;
    int iterations = 100;
    NameFinderFeatureGenerator nffg //<co id="tm.fg"/>
      = new NameFinderFeatureGenerator();
    BagOfWordsFeatureGenerator bowfg 
      = new BagOfWordsFeatureGenerator();

    DoccatModel model = DocumentCategorizerME.train("en", 
        ds, cutoff, iterations, nffg, bowfg); //<co id="tm.train"/>
    model.serialize(new FileOutputStream(modelFile));
    
/*<calloutlist>
<callout arearefs="tm.tok">Create data stream</callout>
<callout arearefs="tm.fg">Set up features generators</callout> 
<callout arearefs="tm.train">Train categorizer</callout>  
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
