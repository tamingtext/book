package com.tamingtext.classifier.bayes;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Assert;
import org.junit.Test;

public class BayesSchemaAnalyzerTest {

  String[] expected = 
    { "lord", "of", "the", "ring", "the", "two", "tower" };
  
  @SuppressWarnings("resource")
  @Test
  public void standardTokenizerFactoryTest() throws IOException {
    
    String input = "Lord of the Rings: The Two Towers";
    BayesSchemaAnalyzer a = new BayesSchemaAnalyzer();
    int pos = 0;
    
    TokenStream tok = a.tokenStream("", new StringReader(input));
    CharTermAttribute term = tok.getAttribute(CharTermAttribute.class);
    
    tok.reset();
    while (tok.incrementToken()) {
      Assert.assertEquals(expected[pos++], term.toString());
    }
    tok.close();
  }
}
