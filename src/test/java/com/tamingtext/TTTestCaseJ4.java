package com.tamingtext;


import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;

import java.io.File;
import java.util.Locale;

/**
 *
 *
 **/
public class TTTestCaseJ4 extends SolrTestCaseJ4 {
  @BeforeClass
  public static void initTTTestCase(){
    Locale.setDefault(localeForName("en_us"));

    File models = new File("../opennlp-models");
    assertTrue(models.exists());
    File wordnet = new File("../WordNet-3.0");
    assertTrue(wordnet.exists());
    System.setProperty("model.dir", "../opennlp-models");
    System.setProperty("wordnet.dir", "../WordNet-3.0");
  }

}
