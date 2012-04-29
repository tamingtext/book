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

package com.tamingtext.tagrecommender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;

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
import org.apache.mahout.math.function.ObjectIntProcedure;
import org.apache.mahout.math.map.OpenObjectIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tamingtext.tagrecommender.TagRecommenderClient.ScoreTag;

public class TestStackOverflowTagger {
   
  private static final Logger log = LoggerFactory.getLogger(TestStackOverflowTagger.class);

  private final NumberFormat nf = new DecimalFormat("##.##");
  private TagRecommenderClient client;
  private File   inputFile;
  private File   countFile;
  private File   outputFile;
  
  private String solrUrl;
  private int    maxTags = 5;
 
  public static void main(String[] args) {
    TestStackOverflowTagger t = new TestStackOverflowTagger();
    if (t.parseArgs(args)) {
      t.execute();
    }
  }
  
  public boolean parseArgs(String[] args) {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    Option helpOpt = DefaultOptionCreator.helpOption();
    
    Option inputFileOpt = obuilder.withLongName("inputFile").withRequired(true).withArgument(
        abuilder.withName("inputFile").withMinimum(1).withMaximum(1).create()).withDescription(
        "The input file").withShortName("i").create();
    
    Option countFileOpt = obuilder.withLongName("countFile").withRequired(true).withArgument(
        abuilder.withName("countFile").withMinimum(1).withMaximum(1).create()).withDescription(
        "The tag count file").withShortName("c").create();
    
    Option outputFileOpt = obuilder.withLongName("outputFile").withRequired(true).withArgument(
        abuilder.withName("outputFile").withMinimum(1).withMaximum(1).create()).withDescription(
        "The output file").withShortName("c").create();
    
    Option solrUrlOpt = obuilder.withLongName("solrUrl").withRequired(true).withArgument(
        abuilder.withName("solrUrl").withMinimum(1).withMaximum(1).create()).withDescription(
        "URL of the solr server").withShortName("s").create();
    
    Group group = gbuilder.withName("Options")
      .withOption(inputFileOpt)
      .withOption(countFileOpt)
      .withOption(outputFileOpt)
      .withOption(solrUrlOpt).create();
    
    try {
      Parser parser = new Parser();
      parser.setGroup(group);
      CommandLine cmdLine = parser.parse(args);
      
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return false;
      }
      
      inputFile  = new File((String) cmdLine.getValue(inputFileOpt));
      countFile  = new File((String) cmdLine.getValue(countFileOpt));
      outputFile = new File((String) cmdLine.getValue(outputFileOpt));
      solrUrl    = (String) cmdLine.getValue(solrUrlOpt);
      client     = new TagRecommenderClient(solrUrl);
    } catch (OptionException e) {
      log.error("Command-line option Exception", e);
      CommandLineUtil.printHelp(group);
      return false;
    } catch (MalformedURLException e) {
      log.error("MalformedURLException", e);
      return false;
    }
    
    validate();
    return true;
  }
  

  
  public void validate() {
    Util.validateFileWritable(outputFile);
  }
  
  public void loadTags(OpenObjectIntHashMap<String> tags) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(countFile));
    String line;
    while ((line = reader.readLine()) != null) {
      int pos = line.lastIndexOf('\t');
      String tag = new String(line.substring(pos+1));
      tags.adjustOrPutValue(tag, 0, 0);
    }
  }
  
  public void execute() {
    PrintStream out = null;
    
    try {
      OpenObjectIntHashMap<String> tagCounts = new OpenObjectIntHashMap<String>();
      OpenObjectIntHashMap<String> tagCorrect = new OpenObjectIntHashMap<String>();
      loadTags(tagCounts);
      
      StackOverflowStream stream = new StackOverflowStream();
      stream.open(inputFile.getAbsolutePath());
      
      out = new PrintStream(new FileOutputStream(outputFile));
      
      int correctTagCount  = 0;
      int postCount        = 0;
      
      HashSet<String> postTags = new HashSet<String>();
      float postPctCorrect;
      
      int totalSingleCorrect = 0;
      int totalHalfCorrect   = 0;
      
      for (StackOverflowPost post: stream) {
        correctTagCount = 0;
        postCount++;
        
        postTags.clear();
        postTags.addAll(post.getTags());
        for (String tag: post.getTags()) {
          if (tagCounts.containsKey(tag)) {
            tagCounts.adjustOrPutValue(tag, 1, 1);
          }
        }
        
        ScoreTag[] tags = client.getTags(post.getTitle() + "\n" + post.getBody(), maxTags);
        
        for (ScoreTag tag: tags) {
          if (postTags.contains(tag.getTag())) {
            correctTagCount += 1;
            tagCorrect.adjustOrPutValue(tag.getTag(), 1, 1);
          }
        }
        
        if (correctTagCount > 0) {
          totalSingleCorrect += 1;
        }
        
        postPctCorrect = correctTagCount / (float) postTags.size();
        if (postPctCorrect >= 0.50f) {
          totalHalfCorrect += 1;
        }
        
        if ((postCount % 100) == 0 ) {
          dumpStats(System.err, postCount, totalSingleCorrect, totalHalfCorrect);
        }
        
      }
      
      dumpStats(System.err, postCount, totalSingleCorrect, totalHalfCorrect);
      dumpStats(out, postCount, totalSingleCorrect, totalHalfCorrect);
      dumpTags(out, tagCounts, tagCorrect);
    }
    catch (Exception ex) {
      throw (RuntimeException) new RuntimeException().initCause(ex);
    } 
    finally {
      if (out != null) {
        out.close();
      }
    }
  }
  

  /** Dump the tag metrics */
  public void dumpTags(final PrintStream out,
      final OpenObjectIntHashMap<String> tagCounts,
      final OpenObjectIntHashMap<String> tagCorrect) {
    
    out.println("-- tag\ttotal\tcorrect\tpct-correct --");
    
    tagCounts.forEachPair(new ObjectIntProcedure<String>() {
      @Override
      public boolean apply(String tag, int total) {
        int correct = tagCorrect.get(tag);
        
        out.println(tag + "\t" + total + "\t" + correct + "\t"
            + nf.format(((correct * 100) / (float) total)));
        return true;
      }
    });
    
    out.println();
    out.flush();
  }
  
  /** Dump the overall metrics */
  public void dumpStats(PrintStream out, int postCount, int totalSingleCorrect, int totalHalfCorrect) {
    out.println("evaluated " + postCount + " posts; " 
        + totalSingleCorrect + " with one correct tag, " 
        + totalHalfCorrect + " with half correct");

    out.print("\t %single correct: " + nf.format((totalSingleCorrect * 100) / (float) postCount));
    out.println(", %half correct: " + nf.format((totalHalfCorrect * 100) / (float) postCount));
    out.println();
    out.flush();
  }
}
