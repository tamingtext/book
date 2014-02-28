package com.tamingtext.classifier.bayes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.BeforeClass;
import org.junit.Test;

import com.tamingtext.TTTestCaseJ4;

public class BayesVectorsFromLuceneTest extends TTTestCaseJ4 {

  static File baseDir;
  static File solrDir;
  
  @BeforeClass
  public static void beforeClass() throws Exception {
    baseDir = new File("target/test-output/extract-test");
    baseDir.delete();
    baseDir.mkdirs();

    solrDir = new File(baseDir, "solr");
    solrDir.mkdirs();
    
    initCore(
        "solr/conf/bayes-extract-config.xml",
        "solr/conf/bayes-schema.xml", 
        "");
  }

  public static String readFile(File file) throws IOException {
    StringBuilder buf = new StringBuilder();
    String line;
    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    while ((line = r.readLine()) != null) {
      buf.append(line); 
    }
    return buf.toString();
  }

  @Test
  public void testExtract() throws Exception {
    
    assertU("Add a doc to be extracted",
        adoc("id",  "1",
            "category", "scifi",
            "details", "Star Wars: A New Hope"));

    assertU("Add a doc to be extracted",
        adoc("id",  "2",
            "category", "scifi",
            "details", "Star Wars: The Empire Strikes Back"));
    
    assertU("Add a doc to be extracted",
        adoc("id",  "3",
            "category", "scifi",
            "details", "Star Wars: The Revenge of the Jedi"));
    
    assertU("Add a doc to be extracted",
        adoc("id",  "4",
            "category", "fantasy", 
            "details", "Lord of the Rings: Fellowship of the Ring"));
    
    assertU("Add a doc to be extracted",
        adoc("id",  "5",
            "category", "fantasy", 
            "details", "Lord of the Rings: The Two Towers"));
    
    assertU("Add a doc to be extracted",
        adoc("id",  "6",
            "category", "fantasy", 
            "details", "Lord of the Rings: Return of the King"));
    
    assertU(commit());
    
    File outputDir = new File(baseDir, "extract");
    File vectorOutputDir = new File(outputDir, "vectors");
    File dictFile = new File(outputDir, "dictionary.txt");
    File seqDictFile = new File(outputDir, "dictionary.seq");
    File indexDir = new File(dataDir, "index");
    
    String[] extractArgs = {
        "--dir", indexDir.getAbsolutePath(),
        "--output", vectorOutputDir.getAbsolutePath(),
        "--field", "details",
        "--idField", "id",
        "--categoryField", "category",
        "--dictOut", dictFile.getAbsolutePath(),
        "--seqDictOut", seqDictFile.getAbsolutePath(),
        "--weight", "TFIDF",
    };
    
    BayesVectorsFromLucene.main(extractArgs);
  }
  
}
