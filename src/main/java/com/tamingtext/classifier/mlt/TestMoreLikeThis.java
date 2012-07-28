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

package com.tamingtext.classifier.mlt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.mahout.classifier.ClassifierResult;
import org.apache.mahout.classifier.ResultAnalyzer;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tamingtext.classifier.mlt.TrainMoreLikeThis.MatchMode;
import com.tamingtext.util.FileUtil;


public class TestMoreLikeThis {
  
  private static final Logger log = LoggerFactory.getLogger(TestMoreLikeThis.class);
 
  public static void main(String[] args) throws Exception {
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
    
    Option categoryFieldOpt = obuilder.withLongName("categoryField").withRequired(true).withArgument(
        abuilder.withName("index").withMinimum(1).withMaximum(1).create()).withDescription(
        "Name of the field containing category information").withShortName("catf").create();

    Option contentFieldOpt = obuilder.withLongName("contentField").withRequired(true).withArgument(
        abuilder.withName("index").withMinimum(1).withMaximum(1).create()).withDescription(
        "Name of the field containing content information").withShortName("contf").create();
    
    Option maxResultsOpt = obuilder.withLongName("maxResults").withRequired(false).withArgument(
        abuilder.withName("gramSize").withMinimum(1).withMaximum(1).create()).withDescription(
        "Number of results to retrive, default: 10 ").withShortName("r").create();
    
    Option gramSizeOpt = obuilder.withLongName("gramSize").withRequired(false).withArgument(
      abuilder.withName("gramSize").withMinimum(1).withMaximum(1).create()).withDescription(
      "Size of the n-gram. Default Value: 1 ").withShortName("ng").create();
    
    Option typeOpt = obuilder.withLongName("classifierType").withRequired(false).withArgument(
      abuilder.withName("classifierType").withMinimum(1).withMaximum(1).create()).withDescription(
      "Type of classifier: knn|tfidf. Default: bayes").withShortName("type").create();
    
    Group group = gbuilder.withName("Options").withOption(gramSizeOpt).withOption(helpOpt).withOption(
        inputDirOpt).withOption(modelOpt).withOption(typeOpt).withOption(contentFieldOpt)
        .withOption(categoryFieldOpt).withOption(maxResultsOpt)
        .create();
    
    try {
      Parser parser = new Parser();
      
      parser.setGroup(group);
      parser.setHelpOption(helpOpt);
      CommandLine cmdLine = parser.parse(args);
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return;
      }
      
      String classifierType = (String) cmdLine.getValue(typeOpt);

      int gramSize = 1;
      if (cmdLine.hasOption(gramSizeOpt)) {
        gramSize = Integer.parseInt((String) cmdLine.getValue(gramSizeOpt));
      }

      int maxResults = 10;
      if (cmdLine.hasOption(maxResultsOpt)) {
        maxResults = Integer.parseInt((String) cmdLine.getValue(maxResultsOpt));
      }
      
      String inputPath  = (String) cmdLine.getValue(inputDirOpt);
      String modelPath = (String) cmdLine.getValue(modelOpt);
      String categoryField = (String) cmdLine.getValue(categoryFieldOpt);
      String contentField = (String) cmdLine.getValue(contentFieldOpt);
      
      MatchMode mode;
      
      if ("knn".equalsIgnoreCase(classifierType)) {
        mode = MatchMode.KNN;
      } 
      else if ("tfidf".equalsIgnoreCase(classifierType)) {
        mode = MatchMode.TFIDF;
      }
      else {
        throw new IllegalArgumentException("Unkown classifierType: " + classifierType);
      }

      Directory directory = FSDirectory.open(new File(modelPath));
      IndexReader indexReader = IndexReader.open(directory);
      Analyzer analyzer   //<co id="mlt.analyzersetup"/>
        = new EnglishAnalyzer(Version.LUCENE_36);
      
      MoreLikeThisCategorizer categorizer 
        = new MoreLikeThisCategorizer(indexReader, categoryField);
      categorizer.setAnalyzer(analyzer);
      categorizer.setMatchMode(mode);
      categorizer.setFieldNames(new String[]{ contentField });
      categorizer.setMaxResults(maxResults);
      categorizer.setNgramSize(gramSize);

      File f = new File(inputPath);
      if (!f.isDirectory()) {
        throw new IllegalArgumentException(f + " is not a directory or does not exit");
      }
      
      File[] inputFiles = FileUtil.buildFileList(f);
      
      String line = null;
      //<start id="lucene.examples.mlt.test"/>
      final ClassifierResult UNKNOWN = new ClassifierResult("unknown",
              1.0);
      
      ResultAnalyzer resultAnalyzer = //<co id="co.mlt.ra"/>
        new ResultAnalyzer(categorizer.getCategories(), 
            UNKNOWN.getLabel());

      for (File ff: inputFiles) { //<co id="co.mlt.read"/>
        BufferedReader in = 
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(ff), 
                    "UTF-8"));
        while ((line = in.readLine()) != null) {
          String[] parts = line.split("\t");
          if (parts.length != 2) {
            continue;
          }
          
          CategoryHits[] hits //<co id="co.mlt.cat"/>
            = categorizer.categorize(new StringReader(parts[1]));
          ClassifierResult result = hits.length > 0 ? hits[0] : UNKNOWN;
          resultAnalyzer.addInstance(parts[0], result); //<co id="co.mlt.an"/>
        }
        
        in.close();
      }

      System.out.println(resultAnalyzer.toString());//<co id="co.mlt.print"/>
      /*
      <calloutlist>
        <callout arearefs="co.mlt.ra">Create <classname>ResultAnalyzer</classname></callout>
        <callout arearefs="co.mlt.read">Read Test data</callout>
        <callout arearefs="co.mlt.cat">Categorize</callout>
        <callout arearefs="co.mlt.an">Collect Results</callout>
        <callout arearefs="co.mlt.print">Display Results</callout>
      </calloutlist>
      */
      //<end id="lucene.examples.mlt.test"/>
    } catch (OptionException e) {
      log.error("Error while parsing options", e);
    }
  }
}
