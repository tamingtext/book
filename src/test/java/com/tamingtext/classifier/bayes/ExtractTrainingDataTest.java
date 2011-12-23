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

package com.tamingtext.classifier.bayes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.mahout.classifier.bayes.TestClassifier;
import org.apache.mahout.classifier.bayes.TrainClassifier;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tamingtext.classifier.bayes.ExtractTrainingData;


public class ExtractTrainingDataTest extends SolrTestCaseJ4 {
  
  static File baseDir;

  @BeforeClass
  public static void beforeClass() throws Exception {
    baseDir = new File("target/test-output/extract-test");
    baseDir.delete();
    baseDir.mkdirs();
    

    initCore("bayes-update-config.xml",
        "bayes-update-schema.xml");
  }
  
  public static File createTempDirectory() throws IOException {
    File file = File.createTempFile("extract-test", "test");
    file.delete();
    file.mkdirs();
    if (!file.isDirectory()) {
      throw new IOException("Could not create temporary directory: " + file);
    }
    return file;
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
    
    assertU("Add a doc to be classified",
        adoc("id",  "1",
            "category", "scifi",
            "details", "Star Wars: A New Hope"));

    assertU("Add a doc to be classified",
        adoc("id",  "2",
            "category", "scifi",
            "details", "Star Wars: The Empire Strikes Back"));
    
    assertU("Add a doc to be classified",
        adoc("id",  "3",
            "category", "scifi",
            "details", "Star Wars: The Revenge of the Jedi"));
    
    assertU("Add a doc to be classified",
        adoc("id",  "4",
            "category", "fantasy", 
            "details", "Lord of the Rings: Fellowship of the Ring"));
    
    assertU("Add a doc to be classified",
        adoc("id",  "5",
            "category", "fantasy", 
            "details", "Lord of the Rings: The Two Towers"));
    
    assertU("Add a doc to be classified",
        adoc("id",  "6",
            "category", "fantasy", 
            "details", "Lord of the Rings: Return of the King"));
    
    assertU(commit());
    
    File outputDir = new File(baseDir, "extract");
    File indexDir = new File(dataDir, "index");
    File categoryFile = new File("src/test/resources/solr/conf/categories.txt");

    String[] extractArgs = {
        "--dir", indexDir.getAbsolutePath(),
        "--categories", categoryFile.getAbsolutePath(),
        "--output", outputDir.getAbsolutePath(), 
        "--category-fields", "category",
        "--text-fields", "details",
        "--use-term-vectors"
    };
    
    ExtractTrainingData.main(extractArgs);
    
    File[] files = outputDir.listFiles();
    Map<String,File> names = new HashMap<String,File>();
    for (File f: files) {
      names.put(f.getName(), f);
    }
    
    TestCase.assertEquals(2, files.length);
    TestCase.assertTrue("File list contains scifi", names.containsKey("scifi"));
    TestCase.assertTrue("File list contains fantasy", names.containsKey("fantasy"));

    String scifiContent = readFile(names.get("scifi"));
    TestCase.assertTrue("Sci-fi file has proper label", scifiContent.startsWith("scifi\t"));
    TestCase.assertTrue("Sci-fi file contents", scifiContent.contains("star"));
    TestCase.assertTrue("Sci-fi file contents", scifiContent.contains("jedi"));
    
    String fantasyContent = readFile(names.get("fantasy"));
    TestCase.assertTrue("Fantasy file has proper label", fantasyContent.startsWith("fantasy\t"));
    TestCase.assertTrue("Fantasy file contents", fantasyContent.contains("lord"));
    TestCase.assertTrue("Fantasy file contents", fantasyContent.contains("tower"));
    
    File modelDir = new File(baseDir, "model");
    
    String[] trainArgs = {
        "-i", outputDir.getAbsolutePath(),
        "-o", modelDir.getAbsolutePath(),
        "-ng", "1",
        "-type", "bayes",
        "-source", "hdfs"
    };
    
    TrainClassifier.main(trainArgs);
    
    String[] testArgs = {
        "-d", outputDir.getAbsolutePath(),
        "-m", modelDir.getAbsolutePath(),
        "-ng", "1",
        "-type", "bayes",
        "-source", "hdfs"
    };
    
    TestClassifier.main(testArgs);
  }
}
