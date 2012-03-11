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

package com.tamingtext;


import junit.framework.Assert;
import org.junit.BeforeClass;

import java.io.File;

/**
 *
 *
 **/
public class TamingTextTestJ4 extends Assert {
  @BeforeClass
  public static void setUp() {
    File models = new File("opennlp-models");
    assertTrue(models.exists());
    File wordnet = new File("WordNet-3.0");
    assertTrue(wordnet.exists());
    System.setProperty("model.dir", "opennlp-models");
    System.setProperty("wordnet.dir", "WordNet-3.0");
  }

  public static File getWordNetDir(){
    String wordnetDir = System.getProperty("wordnet.dir");

    return new File(wordnetDir);
  }

  public static File getWordNetDictionary(){
    return new File(getWordNetDir(), "dict");
  }

  public static File getModelDir(){
    String modelsDirProp = System.getProperty("model.dir");

    return new File(modelsDirProp);
  }

  //public static File getEnglishDir(){
  //  return new File(getModelDir(), "english");
  //}

  //public static File getNameFindDir(){
  //  return new File(getModelDir(), "namefind");
  //}

  //public static File getSentDetectDir(){
  //  return new File(getModelDir(), "sentdetect");
  //}

  //public static File getChunkerDir(){
  //  return new File(getModelDir(), "chunker");
  //}

  //public static File getParserDir(){
  //  return new File(getModelDir(), "parser");
  //}

  //public static File getPOSDir(){
  //  return new File(getModelDir(), "postag");
  //}

  public static File getPersonModel(){
    return new File(getModelDir(), "en-ner-person.bin");
  }


}
