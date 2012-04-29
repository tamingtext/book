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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import org.junit.Test;

import com.tamingtext.TamingTextTestJ4;

/**
 * Place the OpenNLP English models (http://opennlp.sourceforge.net/models.html) in a directory and define the System property "models.dir" to point to that directory.
 * Alternatively, create a directory as a sibling of the Taming Text project named opennlp-tools-1.3.0 and place the models directory in there.
 */
public class POSTaggerTest extends TamingTextTestJ4 {

  @Test
  public void test() throws IOException {

    //<start id="opennlpPOS"/>
    File posModelFile = new File( //<co id="opennlpPOS.co.tagger"/>
        getModelDir(), "en-pos-maxent.bin"); 
    FileInputStream posModelStream = new FileInputStream(posModelFile);
    POSModel model = new POSModel(posModelStream);
    
    POSTaggerME tagger = new POSTaggerME(model);
    String[] words = SimpleTokenizer.INSTANCE.tokenize( //<co id="opennlpPOS.co.tokenize"/>
        "The quick, red fox jumped over the lazy, brown dogs.");
    String[] result = tagger.tag(words);//<co id="opennlpPOS.co.dotag"/>
    for (int i=0 ; i < words.length; i++) {
      System.err.print(words[i] + "/" + result[i] + " ");
    }
    System.err.println("\n");
    /*
<calloutlist>
<callout arearefs="opennlpPOS.co.tagger"><para>Give the path to the POS Model</para></callout>
<callout arearefs="opennlpPOS.co.tokenize"><para>Tokenize the sentence into words</para></callout>
<callout arearefs="opennlpPOS.co.dotag"><para>Pass in a tokenized sentence to be tagged.</para></callout>
</calloutlist>
    */

    //<end id="opennlpPOS"/>
  }
}