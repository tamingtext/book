package com.tamingtext.classifier.bayes;

import java.io.StringReader;
import java.util.SortedSet;

import org.apache.mahout.classifier.ClassifierResult;
import org.junit.Assert;
import org.junit.Test;

public class TextClassifierTest {

  @Test
  public void test() throws Exception {
    String prefix = "src/test/resources/classifier/bayes";
    String input = "Lord of the Rings: The Two Towers";
    TextClassifier c = new TextClassifier();
    c.labelFileName = prefix + "/sample-label-index";
    c.dictionaryName = prefix + "/sample-dictionary.fst";
    c.modelDirName = prefix + "/sample-model";
    c.termWeightMethod = "TFIDF";
    c.analyzer = new BayesSchemaAnalyzer();
    c.init();
    
    SortedSet<ClassifierResult> result = c.classify("", new StringReader(input));
    ClassifierResult r = result.first();
    
    Assert.assertEquals("Unexpected classifier result", "fantasy", r.getLabel());
  }
}
