/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.tamingtext.qa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import opennlp.maxent.Event;
import opennlp.maxent.EventStream;
import opennlp.tools.lang.english.ParserTagger;
import opennlp.tools.lang.english.TreebankChunker;
import opennlp.tools.lang.english.TreebankParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;

public class AnswerTypeEventStream implements EventStream {

  protected BufferedReader reader;
  protected String line;
  protected AnswerTypeContextGenerator atcg;
  protected Parser parser;
  
  public AnswerTypeEventStream(String fileName, String encoding, AnswerTypeContextGenerator atcg,Parser parser) throws IOException {
    if (encoding == null) {
      reader = new BufferedReader(new FileReader(fileName));
    }
    else {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),encoding));
    }
    this.atcg = atcg;
    this.parser = parser;
  }
  
  public AnswerTypeEventStream(String fileName,AnswerTypeContextGenerator atcg,Parser parser) throws IOException {
    this(fileName,null,atcg,parser);
  }
    
  /**
   * Creates a new file event stream from the specified file.
   * @param file the file containing the events.
   * @throws IOException When the specified file can not be read.
   */
  public AnswerTypeEventStream(File file) throws IOException {
    reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF8"));
  }
  
  public boolean hasNext() {
    try {
      return (null != (line = reader.readLine()));
    }
    catch (IOException e) {
      System.err.println(e);
      return (false);
    }
  }
  
  public Event nextEvent() {
    int split = line.indexOf(' ');
    String outcome = line.substring(0,split);
    String question = line.substring(split+1);
    Parse query = TreebankParser.parseLine(question,parser,1)[0];
    return (new Event(outcome, atcg.getContext(query)));
  }
  
  /**
   * Generates a string representing the specified event.
   * @param event The event for which a string representation is needed.
   * @return A string representing the specified event.
   */
  public static String toLine(Event event) {
    StringBuffer sb = new StringBuffer();
    sb.append(event.getOutcome());
    String[] context = event.getContext();
    for (int ci=0,cl=context.length;ci<cl;ci++) {
      sb.append(" "+context[ci]);
    }
    sb.append(System.getProperty("line.separator"));
    return sb.toString();
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("Usage: AnswerTypeEventStream eventfile [iterations cutoff]");
      System.exit(1);
    }
    int ai=0;
    String eventFile = args[ai++];
    int iterations = 100;
    int cutoff = 5;
    if (ai < args.length) {
      iterations = Integer.parseInt(args[ai++]);
      cutoff = Integer.parseInt(args[ai++]);
    }
    String modelsDirProp = System.getProperty("models.dir", "book/src/main" + File.separator + "opennlp-tools-1.3.0" + File.separator + "models" +
        File.separator + "english");
    File modelsDir = new File(modelsDirProp);   
    File parserDir = new File(modelsDir, "chunker");
    File wordnetDir = new File(System.getProperty("wordnet.dir", "book/src/main" + File.separator + "WordNet-3.0" + File.separator + "dict"));
    String trainFile = args[0];
    TreebankChunker chunker = new TreebankChunker(parserDir.getAbsolutePath()
            + File.separator + "EnglishChunk.bin.gz");
    File posDir = new File(modelsDir, "postag");
    ParserTagger tagger =  new ParserTagger(posDir.getAbsolutePath() + File.separator + "tag.bin.gz",
            posDir.getAbsolutePath() + File.separator + "tagdict", true);
    Parser parser = new ChunkParser(chunker, tagger);
    AnswerTypeContextGenerator actg = new AnswerTypeContextGenerator(wordnetDir);
    EventStream es = new AnswerTypeEventStream(eventFile,actg,parser);
    while(es.hasNext()) {
      System.out.println(es.nextEvent().toString());
    }
  }
}

