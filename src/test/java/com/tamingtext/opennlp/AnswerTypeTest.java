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

package com.tamingtext.opennlp;

import com.tamingtext.TamingTextTestJ4;
import com.tamingtext.qa.AnswerTypeContextGenerator;
import com.tamingtext.qa.ChunkParser;
import opennlp.tools.lang.english.ParserTagger;
import opennlp.tools.lang.english.TreebankChunker;
import opennlp.tools.lang.english.TreebankParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import org.junit.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class AnswerTypeTest extends TamingTextTestJ4 {


  @Test
  public void test() throws IOException {

    File parserDir = getChunkerDir();

    AnswerTypeContextGenerator atcg = new AnswerTypeContextGenerator(new File(getWordNetDictionary().getAbsolutePath()));
    //<start id="answerType"/>
    TreebankChunker chunker = new TreebankChunker(parserDir.getAbsolutePath()
            + File.separator + "EnglishChunk.bin.gz");
    File posDir = getPOSDir();
    ParserTagger tagger =  new ParserTagger(posDir.getAbsolutePath() + File.separator + "tag.bin.gz",
            posDir.getAbsolutePath() + File.separator + "tagdict", true);
    Parser parser = new ChunkParser(chunker, tagger);
    Parse[] results = TreebankParser.parseLine("Who is the president of egypt ?", parser, 1);
    String[] context = atcg.getContext(results[0]);
    List<String> features = Arrays.asList(context);
    assertTrue(features.contains("qw=who"));
    assertTrue(features.contains("hw=president"));
    assertTrue(features.contains("s=1740")); //entity
    assertTrue(features.contains("s=7846")); //person
    //<end id="answerType"/>
  }

  @Test
  public void demonstrateATCG() throws Exception {
    File parserDir = getChunkerDir();

    AnswerTypeContextGenerator atcg = new AnswerTypeContextGenerator(new File(getWordNetDictionary().getAbsolutePath()));
    //<start id="answerType"/>
    TreebankChunker chunker = new TreebankChunker(parserDir.getAbsolutePath()
            + File.separator + "EnglishChunk.bin.gz");
    File posDir = getPOSDir();
    ParserTagger tagger =  new ParserTagger(posDir.getAbsolutePath() + File.separator + "tag.bin.gz",
            posDir.getAbsolutePath() + File.separator + "tagdict", true);
    Parser parser = new ChunkParser(chunker, tagger);
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("atcg-questions.txt");
    assertNotNull("input stream", is);
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line = null;
    while ((line = reader.readLine()) != null){
      Parse[] results = TreebankParser.parseLine(line, parser, 1);
      String[] context = atcg.getContext(results[0]);
      List<String> features = Arrays.asList(context);
      System.out.println("Features: " + features);
    }

  }
}