package com.tamingtext.classifier.bayes;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;


public class BayesUpdateRequestProcessorTest extends SolrTestCaseJ4 {
  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("bayes-update-config.xml", 
        "bayes-update-schema.xml");
  }
  
  @Test
  public void testUpdate() {
    assertU("Add a doc to be classified",
        adoc("id",  "1234",
            "details", "Star Wars: The Empire Strikes Back"));
    
    assertU("Add a doc to be classified",
        adoc("id",  "1235",
            "details", "Lord of the Rings: The Two Towers"));
    
    assertU(commit());

    assertQ("Couldn't find indexed scifi instance",
        req("details:Empire"), "//result[@numFound=1]", "//str[@name='id'][.='1234']");

    
    assertQ("Couldn't find indexed fantasy instance",
        req("details:Towers"), "//result[@numFound=1]", "//str[@name='id'][.='1235']");
    
    
    assertQ("Couldn't find classified scifi instance",
        req("subject:scifi"), "//result[@numFound=1]", "//str[@name='id'][.='1234']");

    assertQ("Couldn't find classified fantasy instance",
        req("subject:fantasy"), "//result[@numFound=1]", "//str[@name='id'][.='1235']");
  }
}
