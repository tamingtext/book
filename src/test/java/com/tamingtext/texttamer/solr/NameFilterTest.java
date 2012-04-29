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

package com.tamingtext.texttamer.solr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tamingtext.TamingTextTestJ4;


public class NameFilterTest extends TamingTextTestJ4 {
  private static final String input = 
  "The quick brown fox jumped over William Taft the President. " + 
  "There once was a man from New York City who had to catch the bus at 10:30 " +
  "in the morning of December 21, 1992 ";
  

  
  private static String[] modelName = {
      "date", "location", "money", "organization", 
      "percentage", "person", "time"
  };
  
  private static SentenceDetector detector;
  private static NameFinderME[] finder;

  @BeforeClass 
  public static void setupModels() throws IOException {

    File modelDir = getModelDir();
     
    finder = new NameFinderME[modelName.length];
    for (int i=0; i < modelName.length; i++) {
      finder[i] = new NameFinderME(new TokenNameFinderModel(
          new FileInputStream(
              new File(modelDir, "en-ner-" + modelName[i] + ".bin")
              )));
    }

    File modelFile = new File(modelDir, "en-sent.bin");
    InputStream modelStream = new FileInputStream(modelFile);
    SentenceModel model = new SentenceModel(modelStream);
    detector = new SentenceDetectorME(model);
  }
  
  String[] tokenStrings = {
      "The", "quick", "brown", "fox", "jumped", "over", "NE_person", "William",
      "NE_person", "Taft", "the", "President", ".", "There", "once", "was", "a",
      "man", "from", "NE_location", "New", "NE_location", "York", "NE_location", "City",
      "who", "had", "to", "catch", "the", "bus", "at", "NE_time", "10", "NE_time", ":",
      "NE_time", "30", "in", "the", "morning", "of", "NE_date", "December", "NE_date",
      "21", "NE_date", ",", "NE_date", "1992"
  };
  
  int[] positionIncrements = {
    1, 1, 1, 1, 1, 1, 1, 0, 
    1, 0, 1, 1, 1, 1, 1, 1, 1, 
    1, 1, 1, 0, 1, 0, 1, 0, 
    1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 
    1, 0, 1, 1, 1, 1, 1, 0, 1, 
    0, 1, 0, 1, 0
  };

  @Test public void testNameFilter() throws IOException {
    Reader in = new StringReader(input);
    Tokenizer tok = new SentenceTokenizer(in, detector);
    NameFilter nf = new NameFilter(tok, modelName, finder);

    CharTermAttribute cta;
    PositionIncrementAttribute pta;
    OffsetAttribute oa; 
    
    int pass = 0;
    
    while (pass < 2) { // test reuse.
      int pos = 0;
      int lastStart = 0;
      int lastEnd   = 0;
      
      while (nf.incrementToken()) {
        cta = (CharTermAttribute) nf.getAttribute(CharTermAttribute.class);
        pta = (PositionIncrementAttribute) nf.getAttribute(PositionIncrementAttribute.class);
        oa  = (OffsetAttribute) nf.getAttribute(OffsetAttribute.class);
        
        System.err.println("'" + cta.toString() + "'");
        System.err.println(pta.toString());
        System.err.println(oa.toString());
        System.err.println("--- pass: " + pass);
        
        TestCase.assertEquals(tokenStrings[pos], cta.toString());
        TestCase.assertEquals(positionIncrements[pos], pta.getPositionIncrement());
        
        if (pta.getPositionIncrement() == 0) {
          TestCase.assertEquals(lastStart, oa.startOffset());
          TestCase.assertEquals(lastEnd, oa.endOffset());
        }
        
        if (!cta.toString().startsWith("NE_")) {
          TestCase.assertEquals(input.substring(oa.startOffset(), oa.endOffset()), cta.toString());
        }
        
        lastStart = oa.startOffset();
        lastEnd   = oa.endOffset();
        
        pos++;
      }
      
      //if (pass == 1) nf.dumpState();
      nf.end();
      
      in.close();
      in = new StringReader(input);
      tok.reset(in);
      pass++;
    }
  }
}
