package com.tamingtext.classifier.mlt;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class MoreLikeThisWorkflowTest {

  String dataDir  = "src/test/resources/classifier/mlt/sample-data";
  String knnModelDir = "target/mlt-workflow/knn-model";
  String tfidfModelDir = "target/mlt-workflow/tfidf-model";
  
  // ensure we have a clean model directory.
  @Before
  public void setUp() throws IOException {
    {
      File f = new File(knnModelDir);
      f.mkdirs();
      FileUtils.cleanDirectory(f);
    }
    
    {
      File f = new File(tfidfModelDir);
      f.mkdirs();
      FileUtils.cleanDirectory(f);
    }
  }
  
  @Test
  public void testKnnWorkflow() throws Exception {

    String[] trainArgs = {
        "--input", dataDir,
        "--output", knnModelDir,
        "--gramSize", "2",
        "--classifierType", "knn"
    };
    TrainMoreLikeThis.main(trainArgs);
    
    String[] testArgs = {
        "--input", dataDir,
        "--model", knnModelDir,
        "--gramSize", "2",
        "--classifierType", "knn",
    };
    TestMoreLikeThis.main(testArgs);
  }
  
  @Test
  public void testTfIdfWorkflow() throws Exception {
    String[] trainArgs = {
        "--input", "src/test/resources/classifier/mlt/sample-data",
        "--output", "target/mlt-workflow/tfidf-model",
        "--gramSize", "2",
        "--classifierType", "knn"
    };
    TrainMoreLikeThis.main(trainArgs);
    
    String[] testArgs = {
        "--input", "src/test/resources/classifier/mlt/sample-data",
        "--model", "target/mlt-workflow/tfidf-model",
        "--gramSize", "2",
        "--classifierType", "knn",
        "--contentField", "content",
        "--categoryField", "category"
    };
    TestMoreLikeThis.main(testArgs);
  }
}
