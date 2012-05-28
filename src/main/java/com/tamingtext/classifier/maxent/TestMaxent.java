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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizer;
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
import org.apache.mahout.classifier.ClassifierResult;
import org.apache.mahout.classifier.ResultAnalyzer;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tamingtext.util.FileUtil;

public class TestMaxent {
  
  private static final Logger log = LoggerFactory.getLogger(TestMaxent.class);
  
  /**
   * @param args
   */
  public static void main(String[] args) throws IOException {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    
    Option helpOpt = DefaultOptionCreator.helpOption();
    
    Option inputDirOpt = obuilder.withLongName("input").withRequired(true).withArgument(
      abuilder.withName("input").withMinimum(1).withMaximum(1).create()).withDescription(
      "The input directory")
        .withShortName("i").create();
    
    Option modelOpt = obuilder.withLongName("model").withRequired(true).withArgument(
      abuilder.withName("index").withMinimum(1).withMaximum(1).create()).withDescription(
      "The directory containing the index model").withShortName("m").create();

    Group group = gbuilder.withName("Options").withOption(helpOpt)
        .withOption(inputDirOpt).withOption(modelOpt).create();
    
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
      File f = new File(inputPath);
      if (!f.isDirectory()) {
        throw new IllegalArgumentException(f + " is not a directory or does not exit");
      }
      File[] inputFiles = FileUtil.buildFileList(f);
      
      File   modelDir  = new File((String) cmdLine.getValue(modelOpt));
      execute(inputFiles, modelDir);
    } catch (OptionException e) {
      log.error("Error while parsing options", e);
    }
    
  }

  private static void execute(File[] inputFiles, File modelFile)
      throws IOException, FileNotFoundException {
    //<start id="maxent.examples.test.setup"/> 
    NameFinderFeatureGenerator nffg //<co id="tmx.feature"/>
      = new NameFinderFeatureGenerator(); 
    BagOfWordsFeatureGenerator bowfg 
      = new BagOfWordsFeatureGenerator(); 

    InputStream modelStream = //<co id="tmx.modelreader"/>
        new FileInputStream(modelFile);
    DoccatModel model = new DoccatModel(modelStream);
    DocumentCategorizer categorizer //<co id="tmx.categorizer"/>
      = new DocumentCategorizerME(model, nffg, bowfg);
    Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
   
    int catCount = categorizer.getNumberOfCategories();
    Collection<String> categories 
      = new ArrayList<String>(catCount);
    for (int i=0; i < catCount; i++) {
      categories.add(categorizer.getCategory(i));
    }
    ResultAnalyzer resultAnalyzer = //<co id="tmx.results"/>
        new ResultAnalyzer(categories, "unknown");
    runTest(inputFiles, categorizer, tokenizer, resultAnalyzer); //<co id="tmx.run"/>
    /*<calloutlist>
    <callout arearefs="tmx.feature">Setup Feature Generators</callout>
    <callout arearefs="tmx.modelreader">Load Model</callout>
    <callout arearefs="tmx.categorizer">Create Categorizer</callout>
    <callout arearefs="tmx.results">Prepare Result Analyzer</callout>
    <callout arearefs="tmx.run">Execute Test</callout>
    </calloutlist>*/
    //<end id="maxent.examples.test.setup"/>
  }

  private static void runTest(File[] inputFiles, 
      DocumentCategorizer categorizer,
      Tokenizer tokenizer, ResultAnalyzer resultAnalyzer)
      throws FileNotFoundException, IOException {
    String line;
    //<start id="maxent.examples.test.execute"/>
    for (File ff: inputFiles) { 
      BufferedReader in = new BufferedReader(new FileReader(ff));
      while ((line = in.readLine()) != null) { 
        String[] parts = line.split("\t");
        if (parts.length != 2) continue;
        
        String docText   = parts[1]; //<co id="tmt.preprocess"/>
        String[] tokens  = tokenizer.tokenize(docText); 
        
        double[] probs   = categorizer.categorize(tokens); //<co id="tmt.categorize"/>
        String label     = categorizer.getBestCategory(probs);
        int    bestIndex = categorizer.getIndex(label);
        double score     = probs[bestIndex];

        ClassifierResult result //<co id="tmt.collect"/>
          = new ClassifierResult(label, score);
        resultAnalyzer.addInstance(parts[0], result);
      }
      in.close();
    }
    
    System.err.println(resultAnalyzer.toString()); //<co id="tmt.summarize"/>
    /*<calloutlist>
     * <callout arearefs="tmt.preprocess">Preprocess text</callout>
     * <callout arearefs="tmt.categorize">Categorize</callout>
     * <callout arearefs="tmt.collect">Analyze Results</callout>
     * <callout arearefs="tmt.summarize">Present Results</callout>
     * </calloutlist>*/
    //<end id="maxent.examples.test.execute"/>
  }
}
