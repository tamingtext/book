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

    TamingTextTestJ4.setUp();
  }

}
