package com.tamingtext.classifier.bayes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.mahout.classifier.naivebayes.test.TestNaiveBayesDriver;
import org.apache.mahout.classifier.naivebayes.training.TrainNaiveBayesJob;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterator;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.math.map.OpenObjectIntHashMap;
import org.apache.mahout.utils.SplitInput;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.tamingtext.TTTestCaseJ4;

public class BayesWorkflowTest extends TTTestCaseJ4 {

  static File baseDir;
  static File solrDir;

  @BeforeClass
  public static void beforeClass() throws Exception {
    baseDir = new File("target/test-output/extract-test");
    if (!FileUtils.deleteQuietly(baseDir)) {
      Assert.fail("Could not delete " + baseDir);
    }

    if (!baseDir.mkdirs()) {
      Assert.fail("Count not create " + baseDir);
    }

    solrDir = new File(baseDir, "solr");
    solrDir.mkdirs();

    initCore(
        "solr/conf/bayes-extract-config.xml",
        "solr/conf/bayes-schema.xml", 
        "");
  }

  @Test
  public void testWorkflow() throws Exception {

    loadDocuments();

    File outputDir = new File(baseDir, "extract");
    File vectorOutputDir  = new File(outputDir, "vectors");
    File dictFile    = new File(outputDir, "dictionary.txt");
    File seqDictFile = new File(outputDir, "dictionary.seq");
    File indexDir    = new File(dataDir, "index");

    vectorOutputDir.mkdirs();

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

    // read the vectors and make some assertions about them.
    validateVectors("extracted" , vectorOutputDir, "fantasy", 0, 6);
    validateVectors("extracted" , vectorOutputDir, "scifi", 6, 0);
    
    File fstOutputFile = new File(outputDir, "dictionary.fst");
    
    String[] args = {
        "--input",  dictFile.getAbsolutePath(),
        "--vector-dir", vectorOutputDir.getAbsolutePath(),
        "--output", fstOutputFile.getAbsolutePath()
    };

    FSTDictionaryBuilder.main(args);
    
    validateFstDictionary(fstOutputFile, 25, 12);
    
    File trainingOutputDir = new File(outputDir, "training");
    File testOutputDir     = new File(outputDir, "test");

    String[] splitArgs = {
        "--input", vectorOutputDir.getAbsolutePath(),
        "--testSplitSize", "2",
        "--trainingOutput", trainingOutputDir.getAbsolutePath(),
        "--testOutput", testOutputDir.getAbsolutePath(),
        "--method", "sequential",
        "--splitLocation", "0", // unexpected, but necessary to split properly.
        "--sequenceFiles"
    };

    SplitInput.main(splitArgs);

    // expect 4 document for each training vector category, 2 for each
    // test category.
    validateVectors("training", trainingOutputDir, "scifi", 4, 0);
    validateVectors("training", trainingOutputDir, "fantasy", 0, 4);
    
    validateVectors("test",testOutputDir, "scifi", 2, 0);
    validateVectors("test",testOutputDir, "fantasy", 0, 2);

    File modelOutputDir = new File(outputDir, "model");
    File labelIndexOutputFile = new File(outputDir, "label-index");

    String[] trainArgs = {
        "--input", trainingOutputDir.getAbsolutePath(),
        "--output", modelOutputDir.getAbsolutePath(),
        "--extractLabels", 
        "--labelIndex", labelIndexOutputFile.getAbsolutePath(),
        "--overwrite",
    };

    TrainNaiveBayesJob.main(trainArgs);

    File testResultsOutputDir = new File(outputDir, "test-results");

    String[] testArgs = {
        "--input", testOutputDir.getAbsolutePath(),
        "--model", modelOutputDir.getAbsolutePath(),
        "--output", testResultsOutputDir.getAbsolutePath(),
        "--labelIndex", labelIndexOutputFile.getAbsolutePath()

    };

    TestNaiveBayesDriver.main(testArgs);
  }

  /** Add documents to be extracted from the index */
  protected void loadDocuments() throws IOException {
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
            "category", "scifi",
            "details", "Star Wars: The Phantom Menace"));

    assertU("Add a doc to be extracted",
        adoc("id",  "5",
            "category", "scifi",
            "details", "Star Wars: The Clone Wars"));

    assertU("Add a doc to be extracted",
        adoc("id",  "6",
            "category", "scifi",
            "details", "Star Wars: The Revenge of the Sith"));

    assertU("Add a doc to be extracted",
        adoc("id",  "7",
            "category", "fantasy", 
            "details", "Lord of the Rings: Fellowship of the Ring"));

    assertU("Add a doc to be extracted",
        adoc("id",  "8",
            "category", "fantasy", 
            "details", "Lord of the Rings: The Two Towers"));

    assertU("Add a doc to be extracted",
        adoc("id",  "9",
            "category", "fantasy", 
            "details", "Lord of the Rings: Return of the King"));

    assertU("Add a doc to be extracted",
        adoc("id",  "10",
            "category", "fantasy", 
            "details", "The Hobbit: An Unexpected Journey"));

    assertU("Add a doc to be extracted",
        adoc("id",  "11",
            "category", "fantasy", 
            "details", "The Hobbit: The Desolation of Smaug"));

    assertU("Add a doc to be extracted",
        adoc("id",  "12",
            "category", "fantasy", 
            "details", "The Hobbit: There and Back Again"));

    assertU(commit());
  }

  /** Validate the vector output from the extraction or split operations */
  protected void validateVectors(String setName, File f, String name, int expectedScifi, int expectedFantasy) throws IOException {
    Configuration conf = new Configuration();
    Path path = new Path(new File(f, name).getAbsolutePath());

    SequenceFileIterator<Text, VectorWritable> iterator =
        new SequenceFileIterator<Text, VectorWritable>(path, true, conf);

    OpenObjectIntHashMap<String> categories = new OpenObjectIntHashMap<String>();
    Set<String> identifiers = new HashSet<String>();

    while (iterator.hasNext()) {
      Pair<Text, VectorWritable> pair = iterator.next();
      String keyName    = pair.getFirst().toString();
      Vector v = pair.getSecond().get();

      validateVectorData(keyName, v, categories, identifiers);
    }

    validateCategories(categories, expectedScifi, expectedFantasy);

    Assert.assertEquals("Unexpected number of 'scifi' documents",
        expectedScifi, categories.get("scifi"));
    
    Assert.assertEquals("Unexpected number of 'fantasy' documents", 
        expectedFantasy, categories.get("fantasy"));
    
    iterator.close();
  }

  /** Validate that both categories exist in the data
   * 
   * @param categories
   */
  protected void validateCategories(OpenObjectIntHashMap<String> categories, int expectedScifi, int expectedFantasy) {
    Set<String> expectedCategories  = new TreeSet<String>();
    
    if (expectedScifi > 0) {
      expectedCategories.add("scifi");
    }
    
    if (expectedFantasy > 0) {
      expectedCategories.add("fantasy");
    }
    
    Set<String> remainingUnexpected = new TreeSet<String>(categories.keys());

    Set<String> remainingExpected   = new TreeSet<String>(expectedCategories);
    remainingExpected.removeAll(categories.keys());
    remainingUnexpected.removeAll(expectedCategories);

    Assert.assertTrue("Observed unexpected categories: " + remainingUnexpected.toString(), remainingUnexpected.isEmpty());
    Assert.assertTrue("Did not observe expected categories: " + remainingExpected.toString(), remainingExpected.isEmpty());
  }

  /** Validate basic vector properties
   * @param key
   * @param vector
   * @param categories
   *   current category state, populated by the code in this method.
   * @param identifiers
   *   current identifier state, populated by the code in this method.
   */
  protected void validateVectorData(String key, Vector vector,
      OpenObjectIntHashMap<String> categories, Set<String> identifiers) {
    Assert.assertTrue("Expected VectorWritable to contain a NamedVector", vector instanceof NamedVector);
    NamedVector nv = (NamedVector) vector;
    String vectorName = nv.getName();
    Assert.assertEquals("Expected key and vector name to be equal", key, vectorName);
    List<String> a = new ArrayList<String>();
    for (String s: Splitter.on("/").split(vectorName)) {
      a.add(s);
    }
    Assert.assertEquals("Vector name is the expected number of parts", 3, a.size());
    categories.adjustOrPutValue(a.get(1), 1, 1);
    Assert.assertTrue("Expected identifier to be unique", identifiers.add(a.get(2)));
  }
  
  protected void validateFstDictionary(File f, long expectedTermCount, long expectedDocCount) throws IOException {
    FSTDictionary dict = new FSTDictionary(new FileInputStream(f));
    Assert.assertEquals("Expected term count did not match actual term count", expectedTermCount, dict.getTermCount());
    Assert.assertEquals("Expected doc count did not match actual count", expectedDocCount, dict.getDocumentCount());
  }
}
