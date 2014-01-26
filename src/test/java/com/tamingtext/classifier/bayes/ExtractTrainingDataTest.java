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
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.mahout.classifier.naivebayes.test.TestNaiveBayesDriver;
import org.apache.mahout.classifier.naivebayes.training.TrainNaiveBayesJob;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.PathFilters;
import org.apache.mahout.common.iterator.sequencefile.PathType;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileDirIterable;
import org.apache.mahout.vectorizer.SparseVectorsFromSequenceFiles;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ExtractTrainingDataTest extends SolrTestCaseJ4 {

  static {
    System.setProperty("tests.asserts.gracious", "true");
  }

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

    File luceneVectorDir = new File(baseDir, "luceneVectors");
    File dictDir   = new File(baseDir, "dictionary");
    
    String[] luceneVectorArgs = {
        "--dir", indexDir.getAbsolutePath(),
        "--idField", "id",
        "--output", luceneVectorDir.getAbsolutePath(),
        "--field", "details",
        "--dictOut", dictDir.getAbsolutePath(),
        "--maxDFPercent", "100"
    };
    
    org.apache.mahout.utils.vectors.lucene.Driver.main(luceneVectorArgs);
    
    String[] extractArgs = {
        "--dir", indexDir.getAbsolutePath(),
        "--categories", categoryFile.getAbsolutePath(),
        "--output", outputDir.getAbsolutePath(), 
        "--category-fields", "category",
        "--text-fields", "details",
        "--use-term-vectors"
    };

    ExtractTrainingData.main(extractArgs);

    Multimap<String, String> content = readVectors(outputDir);

    TestCase.assertEquals(6, content.size());
    TestCase.assertTrue("File list contains scifi", content.containsKey("/scifi"));
    TestCase.assertTrue("File list contains fantasy", content.containsKey("/fantasy"));

    Collection<String> scifiContent = content.get("/scifi");
    //TestCase.assertTrue("Sci-fi file has proper label", scifiContent.startsWith("scifi\t"));
    TestCase.assertTrue("Sci-fi file contents", stringCollectionContains(scifiContent, "star"));
    TestCase.assertTrue("Sci-fi file contents", stringCollectionContains(scifiContent, "jedi"));

    Collection<String> fantasyContent = content.get("/fantasy");
    //TestCase.assertTrue("Fantasy file has proper label", fantasyContent.startsWith("fantasy\t"));
    TestCase.assertTrue("Fantasy file contents", stringCollectionContains(fantasyContent, "lord"));
    TestCase.assertTrue("Fantasy file contents", stringCollectionContains(fantasyContent, "tower"));

    File seq2sparseDir = new File(baseDir, "vectors");
    File modelDir = new File(baseDir, "model");
    File testDir  = new File(baseDir, "test");
    File vectorDir = new File(seq2sparseDir, "tfidf-vectors");
    
    String[] seq2sparseArgs = {
        "-i", outputDir.getAbsolutePath(),
        "-o", seq2sparseDir.getAbsolutePath(),
        "-ow"
    };
    
    SparseVectorsFromSequenceFiles.main(seq2sparseArgs);
    
    String[] trainArgs = {
        "-i", vectorDir.getAbsolutePath(),
        "-o", modelDir.getAbsolutePath(),
        "-el",
        "-ow"
    };

    TrainNaiveBayesJob.main(trainArgs);

    String[] testArgs = {
        "-i", vectorDir.getAbsolutePath(),
        "-m", modelDir.getAbsolutePath(),
        "-o", testDir.getAbsolutePath(),
        "-ow",
        "-seq"
    };

    TestNaiveBayesDriver.main(testArgs);

  }

  private static final Multimap<String, String> readVectors(File outputFile) throws IOException {
    Multimap<String, String> output = HashMultimap.create();
    Configuration conf = new Configuration();
    Path toRead = new Path(outputFile.getAbsolutePath());

    for (Pair<Text, Text> pair : new SequenceFileDirIterable<Text, Text>(toRead, PathType.LIST, PathFilters.logsCRCFilter(), conf)) {
      String key = pair.getFirst().toString();
      int pos = key.indexOf("/");
      key = key.substring(pos);
      
      output.put(key, pair.getSecond().toString());
    }

    return output;
  }

  private static final boolean stringCollectionContains(Collection<String> collection, String target) {
    for (String s: collection) {
      if (s.contains(target))
        return true;
    }
    return false;
  }
}
