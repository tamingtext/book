package com.tamingtext.opennlp;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

import com.tamingtext.TamingTextTestJ4;
import junit.framework.TestCase;
import opennlp.tools.lang.english.PosTagger;
import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSTagger;
import org.junit.*;

import java.io.File;
import java.io.IOException;

/**
 * Place the OpenNLP English models (http://opennlp.sourceforge.net/models.html) in a directory and define the System property "models.dir" to point to that directory.
 * Alternatively, create a directory as a sibling of the Taming Text project named opennlp-tools-1.3.0 and place the models directory in there.
 */
public class POSTaggerTest extends TamingTextTestJ4 {
  private static File posModel;
  private static File posDictionary;


  @BeforeClass
  public static void setUpModel() {

    File posDir = getPOSDir();
    posModel = new File(posDir, "tag.bin.gz");
    assertTrue(posModel.getAbsolutePath() + " does not exist", posModel.exists());
    posDictionary = new File(posDir, "tagdict");
  }

  @Test
  public void test() throws IOException {

    //<start id="opennlpPOS"/>
    POSTagger tagger = new PosTagger(posModel.getAbsolutePath(), //<co id="opennlpPOS.co.tagger"/>
            new POSDictionary(posDictionary.getAbsolutePath()));//<co id="opennlpPOS.co.dict"/>
    String result = tagger.tag("The quick, red fox jumped over the lazy, brown dogs");//<co id="opennlpPOS.co.tag"/>
    System.out.println("Result: " + result);
    /*
<calloutlist>
<callout arearefs="opennlpPOS.co.tagger"><para>Give the path to the POS Model</para></callout>
<callout arearefs="opennlpPOS.co.dict"><para>Create a POSDictionary to map words to POS tags</para></callout>
<callout arearefs="opennlpPOS.co.tag"><para>Pass in a sentence to be tagged. The POS Tagger also has two other methods that can be called that operate on tokenized text.</para></callout>
</calloutlist>
    */

    //<end id="opennlpPOS"/>
  }
}