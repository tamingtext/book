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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.classifier.ClassifierResult;

import org.apache.mahout.classifier.bayes.Algorithm;
import org.apache.mahout.classifier.bayes.BayesAlgorithm;
import org.apache.mahout.classifier.bayes.BayesParameters;
import org.apache.mahout.classifier.bayes.ClassifierContext;
import org.apache.mahout.classifier.bayes.Datastore;
import org.apache.mahout.classifier.bayes.InMemoryBayesDatastore;
import org.apache.mahout.classifier.bayes.InvalidDatastoreException;
import org.apache.mahout.common.CommandLineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Simply Utility to demonstrate classifying a document using the Mahout Bayes classifier. Uses the Lucene
 *  StandardAnalyzer for Tokenization.
 */
public class ClassifyDocument {
  
  private static final Logger log = LoggerFactory.getLogger(ExtractTrainingData.class);
  
  public static void main(String[] args) {
    log.info("Command-line arguments: " + Arrays.toString(args));
    
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    
    Option inputOpt = obuilder.withLongName("input")
      .withRequired(true)
      .withArgument(
        abuilder.withName("input")
          .withMinimum(1)
          .withMaximum(1).create())
      .withDescription("Input file")
      .withShortName("i").create();
    
    Option modelOpt = obuilder.withLongName("model")
    .withRequired(true)
    .withArgument(
      abuilder.withName("model")
        .withMinimum(1)
        .withMaximum(1).create())
    .withDescription("Model to use when classifying data")
    .withShortName("m").create();
    
    Option helpOpt = obuilder.withLongName("help")
    .withDescription("Print out help")
    .withShortName("h").create();
    
    Group group = gbuilder.withName("Options")
    .withOption(inputOpt)
    .withOption(modelOpt)
    .withOption(helpOpt)
    .create();
    
    try {
      Parser parser = new Parser();
      parser.setGroup(group);
      CommandLine cmdLine = parser.parse(args);
      
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return;
      }
      
      File inputFile = new File(cmdLine.getValue(inputOpt).toString());
      
      if (!inputFile.isFile()) {
        throw new IllegalArgumentException(inputFile + " does not exist or is not a file");
      }
      
      File modelDir = new File(cmdLine.getValue(modelOpt).toString());
      
      if (!modelDir.isDirectory()) {
        throw new IllegalArgumentException(modelDir + " does not exist or is not a directory");
      }
      
      BayesParameters p = new BayesParameters();
      p.set("basePath", modelDir.getCanonicalPath());
      Datastore ds = new InMemoryBayesDatastore(p);
      Algorithm a  = new BayesAlgorithm();
      ClassifierContext ctx = new ClassifierContext(a,ds);
      ctx.initialize();
      
      //TODO: make the analyzer configurable
      StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
      TokenStream ts = analyzer.tokenStream(null, new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
     
      ArrayList<String> tokens = new ArrayList<String>(1000);
      while (ts.incrementToken()) {
        tokens.add(ts.getAttribute(CharTermAttribute.class).toString());
      }
      String[] document = tokens.toArray(new String[tokens.size()]);
      
      ClassifierResult[] cr = ctx.classifyDocument(document, "unknown", 5);
      
      for (ClassifierResult r: cr) {
        System.err.println(r.getLabel() + "\t" + r.getScore());
      }
    } catch (OptionException e) {
      log.error("Exception", e);
      CommandLineUtil.printHelp(group);
    } catch (IOException e) {
      log.error("IOException", e);
    } catch (InvalidDatastoreException e) {
      log.error("InvalidDataStoreException", e);
    } finally {

    }
  }
}
