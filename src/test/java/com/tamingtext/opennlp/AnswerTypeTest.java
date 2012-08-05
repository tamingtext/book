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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import org.junit.Test;

import com.tamingtext.TamingTextTestJ4;
import com.tamingtext.qa.AnswerTypeContextGenerator;
import com.tamingtext.qa.ChunkParser;

public class AnswerTypeTest extends TamingTextTestJ4 {


  @Test
  public void test() throws IOException {

    File modelDir = getModelDir();

    AnswerTypeContextGenerator atcg = new AnswerTypeContextGenerator(new File(getWordNetDictionary().getAbsolutePath()));
    //<start id="answerType"/>
    FileInputStream chunkerStream = new FileInputStream(
        new File(modelDir,"en-chunker.bin"));
    ChunkerModel chunkerModel = new ChunkerModel(chunkerStream);
    ChunkerME chunker = new ChunkerME(chunkerModel);
    FileInputStream posStream = new FileInputStream(
        new File(modelDir,"en-pos-maxent.bin"));
    POSModel posModel = new POSModel(posStream);
    POSTaggerME tagger =  new POSTaggerME(posModel);
    Parser parser = new ChunkParser(chunker, tagger);
    Parse[] results = ParserTool.parseLine("Who is the president of egypt ?", parser, 1);
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
    File modelDir = getModelDir();
    InputStream chunkerStream = new FileInputStream(
            new File(modelDir,"en-chunker.bin"));
    ChunkerModel chunkerModel = new ChunkerModel(chunkerStream);
    ChunkerME chunker = new ChunkerME(chunkerModel);
    InputStream posStream = new FileInputStream(
            new File(modelDir,"en-pos-maxent.bin"));
    POSModel posModel = new POSModel(posStream);
    POSTaggerME tagger =  new POSTaggerME(posModel);
    Parser parser = new ChunkParser(chunker, tagger);
    //<start id="att.answerTypeDemo"/>
    AnswerTypeContextGenerator atcg =
            new AnswerTypeContextGenerator(
                    new File(getWordNetDictionary().getAbsolutePath()));
    InputStream is = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("atcg-questions.txt");
    assertNotNull("input stream", is);
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line = null;
    while ((line = reader.readLine()) != null){
      System.out.println("Question: " + line);
      Parse[] results = ParserTool.parseLine(line, parser, 1);
      String[] context = atcg.getContext(results[0]);
      List<String> features = Arrays.asList(context);
      System.out.println("Features: " + features);
    }
    //<end id="att.answerTypeDemo"/>
  }
}