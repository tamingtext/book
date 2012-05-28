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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.stream.XMLStreamException;

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
import com.tamingtext.tagrecommender.TagRecommenderClient.ScoreTagQueue;

public class CountStackOverflowTags {
  
  private static final Logger log = LoggerFactory.getLogger(CountStackOverflowTags.class);

  
  /** the input file to read from */
  File inputFile;
  
  /** the file to write counts to */
  File countFile;
  
  /** max number of tags to emit */
  int  limit = 20;
  
  /** minimum number of occurences to emit */
  int  cutoff = 5;
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    CountStackOverflowTags cs = new CountStackOverflowTags();
    if (cs.parseArgs(args)) {
      cs.count();
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
 
    Option outputFileOpt = obuilder.withLongName("outputFile").withRequired(true).withArgument(
        abuilder.withName("outputFile").withMinimum(1).withMaximum(1).create()).withDescription(
        "The output file").withShortName("o").create();

    Option limitOpt = obuilder.withLongName("limit").withRequired(false).withArgument(
        abuilder.withName("limit").withMinimum(1).withMaximum(1).create()).withDescription(
        "Emit this many of the most frequent tags").withShortName("l").create();
    
    Option cutoffOpt = obuilder.withLongName("cutoff").withRequired(false).withArgument(
        abuilder.withName("cutoff").withMinimum(1).withMaximum(1).create()).withDescription(
        "Drop tags with a count less than this number").withShortName("c").create();
    
    Group group = gbuilder.withName("Options").withOption(inputFileOpt)
      .withOption(outputFileOpt).withOption(limitOpt)
      .withOption(cutoffOpt).create();

    
    try {
      Parser parser = new Parser();
      parser.setGroup(group);
      CommandLine cmdLine = parser.parse(args);
      
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return false;
      }
      
      inputFile = new File((String) cmdLine.getValue(inputFileOpt));
      countFile = new File((String) cmdLine.getValue(outputFileOpt));

      if (cmdLine.hasOption(limitOpt)) {
        limit = Integer.parseInt((String) cmdLine.getValue(limitOpt));
      }
      
      if (cmdLine.hasOption(cutoffOpt)) {
        cutoff = Integer.parseInt((String) cmdLine.getValue(cutoffOpt));
      }     
      
    } catch (OptionException e) {
      log.error("Command-line option Exception", e);
      CommandLineUtil.printHelp(group);
      return false;
    }
    
    validate();
    return true;
  }
  
  public void validate() {
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be 1 or more");
    }
    
    if (cutoff < 1) {
      throw new IllegalArgumentException("cutoff must be 1 or more");
    }
    
    Util.validateFileWritable(countFile);
  }
  
  public void count() throws IOException, XMLStreamException {
    StackOverflowStream stream = new StackOverflowStream();
    stream.open(inputFile.getAbsolutePath());
    
    PrintWriter out = new PrintWriter(new FileWriter(countFile));
    
    System.err.println("Counting tags...");
    OpenObjectIntHashMap<String> tagCounts =
      new OpenObjectIntHashMap<String>();
    
    int docCount = 0, tagCount = 0, uniqueCount = 0, 
      pastCutoff = 0, ret = 0;
    
    for (StackOverflowPost post: stream) {
      docCount++;
      for (String tag: post.getTags()) {
        tagCount++;
        ret = tagCounts.adjustOrPutValue(tag, 1, 1);
        if (ret == 1) uniqueCount++;
        if (ret == cutoff) pastCutoff++;
      }
    }
    stream.close();
    
    System.err.println("Processed " + docCount + " documents, " + 
        tagCount + " tags, " + uniqueCount + " unique tags");
    System.err.println(pastCutoff + " tags occur " + cutoff + " or more times");
    
    System.err.println("Ranking tags...");
    int maxTags = pastCutoff < limit ? pastCutoff : limit;
    final ScoreTagQueue queue = new ScoreTagQueue(maxTags);
    tagCounts.forEachPair(new ObjectIntProcedure<String> () {
      @Override
      public boolean apply(String first, int second) {
        if (second >= cutoff) {
          queue.insertWithOverflow(new ScoreTag(first, second));
        }
        return true;
      }
    });
    
    ScoreTag[] rankedTags = new ScoreTag[maxTags];
    int pos = maxTags;
    while (queue.size() > 0) {
      rankedTags[--pos] = queue.pop();
    }
    
    System.err.println("Least tag count " + rankedTags[maxTags-1].getCount());
    System.err.println("Dumping Ranked Tags...");
    System.err.flush();

    int rank = 0;
    for (ScoreTag tag: rankedTags) {
      out.println(++rank + "\t" + tag.getCount() + "\t" + tag.getTag());
    }
    
    out.close();
    
    System.err.println("Done!");
  }
}
