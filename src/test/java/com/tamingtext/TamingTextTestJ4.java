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
    File models = new File("../../opennlp-models");
    assertTrue(models.exists());
    File wordnet = new File("../../WordNet-3.0");
    assertTrue(wordnet.exists());
    System.setProperty("model.dir", "../../opennlp-models");
    System.setProperty("wordnet.dir", "../../WordNet-3.0");
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

  public static File getEnglishDir(){
    return new File(getModelDir(), "english");
  }

  public static File getNameFindDir(){
    return new File(getEnglishDir(), "namefind");
  }

  public static File getSentDetectDir(){
    return new File(getEnglishDir(), "sentdetect");
  }

  public static File getChunkerDir(){
    return new File(getEnglishDir(), "chunker");
  }

  public static File getParserDir(){
    return new File(getEnglishDir(), "parser");
  }

  public static File getPOSDir(){
    return new File(getEnglishDir(), "postag");
  }

  public static File getPersonModel(){
    return new File(getNameFindDir(), "person.bin.gz");
  }


}
