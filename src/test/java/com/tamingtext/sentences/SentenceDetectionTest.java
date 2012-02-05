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

package com.tamingtext.sentences;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import org.junit.Test;

import com.tamingtext.TamingTextTestJ4;

public class SentenceDetectionTest extends TamingTextTestJ4 {

  @Test
  public void testBreakIterator() {
    //<start id="sentDetect"/>
    BreakIterator sentIterator = BreakIterator.getSentenceInstance(Locale.US);
    String testString = "This is a sentence.  It has fruits, vegetables," +
            " etc. but does not have meat.  Mr. Smith went to Washington.";
    sentIterator.setText(testString);
    int start = sentIterator.first();
    int end = -1;
    List<String> sentences = new ArrayList<String>();
    while ((end = sentIterator.next()) != BreakIterator.DONE) {
      String sentence = testString.substring(start, end);
      start = end;
      sentences.add(sentence);
      System.out.println("Sentence: " + sentence);
    }
    //<end id="sentDetect"/>
  }
  @Test
  public void testOpenNLP() throws Exception {

    File modelDir = getModelDir();

    //<start id="openSentDetect"/>
    //... Setup the models
    File modelFile = new File(modelDir, "en-sent.bin");
    InputStream modelStream = new FileInputStream(modelFile);
    SentenceModel model = new SentenceModel(modelStream);
    SentenceDetector detector = //<co id="openSentDetect.co.detect"/>
      new SentenceDetectorME(model);
    String testString = "This is a sentence. It has fruits, vegetables," +
      " etc. but does not have meat. Mr. Smith went to Washington.";
    String[] result = detector.sentDetect(testString); //<co id="openSentDetect.co.run"/>
    for (int i = 0; i < result.length; i++) {
      System.out.println("Sentence: " + result[i]);
    }
    /*<calloutlist>
        <callout arearefs="openSentDetect.co.detect"><para>Create the <command>SentenceDetector</command> with the en-sent.bin model</para></callout>
        <callout arearefs="openSentDetect.co.run"><para>Invoke the detection process</para></callout>
    </calloutlist>*/
    //<end id="openSentDetect"/>
    assertTrue("result Size: " + result.length + " is not: " + 3, result.length == 3);
  }

}