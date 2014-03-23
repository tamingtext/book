package com.tamingtext.classifier.bayes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.Assert;

import org.apache.lucene.analysis.Analyzer;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.math.map.OpenObjectDoubleHashMap;
import org.apache.mahout.vectorizer.TFIDF;
import org.apache.mahout.vectorizer.Weight;
import org.junit.Test;

public class IncrementalVectorizerTest {

  String[][] expected = {
      {"29", "2.791759490966797" },
      {"28", "2.791759490966797" },
      {"26", "1.4142135381698607" },
      {"21", "2.0986123085021973" },
      {"17", "1.5389964580535889" },
      {"14", "2.098612308502197" },
  };
  
  @Test
  public void testIncrementalVectorizer() throws IOException {
    File f = new File("src/test/resources/classifier/bayes/sample-dictionary.fst");
    Weight w = new TFIDF();
    Analyzer a = new BayesSchemaAnalyzer();
    FSTDictionary d = new FSTDictionary(new FileInputStream(f));
    
    OpenObjectDoubleHashMap<Integer> expectedElements = new
        OpenObjectDoubleHashMap<Integer>();
    
    for (String[] s: expected) {
      Integer key = Integer.valueOf(s[0]);
      double  val = Double.parseDouble(s[1]);
      expectedElements.put(key, val);
    }
    
    String input = "Lord of the Rings: The Two Towers";
    
    // should be pretty tight.
    final double precisionTolerance = 0.000000000000001;
    IncrementalVectorizer v = new IncrementalVectorizer(a, d, w);
    Vector vector = v.createVector("fieldName", new StringReader(input));
    for (Element e: vector.nonZeroes()) {
      Integer key = Integer.valueOf(e.index());
      Assert.assertTrue("Unexpected index set in vector: " + key, expectedElements.containsKey(key));

      double expectedVal = expectedElements.get(key);
      
      Assert.assertEquals("Unexpected value present in vector index: " + e.index(), expectedVal, e.get(), precisionTolerance);
      expectedElements.removeKey(key);
    }
    
    Assert.assertTrue("Did not find all expected elements in vector", 
        expectedElements.isEmpty());
  }

}
