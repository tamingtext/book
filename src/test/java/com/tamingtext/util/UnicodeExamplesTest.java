package com.tamingtext.util;

import java.io.StringReader;
import java.text.Normalizer;

import junit.framework.Assert;

import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.junit.Test;

public class UnicodeExamplesTest {

  @Test
  public void testUnicodeNormalization() {
    String precomposed = "\u00E9";
    String decomposed =  "\u0065\u0301";
    
    System.out.println("Precomposed LATIN SMALL LETTER E WITH ACUTE: '" + precomposed + "'");
    System.out.println("Precomposed CODE POINTS: " + toCodePoints(precomposed));
    
    System.out.println("Decomposed LETTER SMALL LETTER E FOLLOWED BY COMBINING ACCUTE ACCENT: '" + decomposed + "'");
    System.out.println("Decomposed CODE POINTS: " + toCodePoints(decomposed));
    
    System.out.println("Are they equal? " + precomposed.equals(decomposed) + "\n");
    
    String precomposedNFC = Normalizer.normalize(precomposed, Normalizer.Form.NFC);
    String decomposedNFC = Normalizer.normalize(decomposed, Normalizer.Form.NFC);
    
    System.out.println("NFC Normalized Precomposed LATIN SMALL LETTER E WITH ACUTE: '" + precomposedNFC + "'");
    System.out.println("NFC Normalized Precomposed CODE POINTS: " + toCodePoints(precomposedNFC));
    
    System.out.println("NFC Normalized Decomposed LETTER SMALL LETTER E FOLLOWED BY COMBINING ACCUTE ACCENT: '" + decomposedNFC + "'");
    System.out.println("NFC Normalized Decomposed CODE POINTS: " + toCodePoints(decomposedNFC));
    
    System.out.println("Are they equal? " + precomposedNFC.equals(decomposedNFC) + "\n");
    
    String precomposedNFD = Normalizer.normalize(precomposed, Normalizer.Form.NFD);
    String decomposedNFD = Normalizer.normalize(decomposed, Normalizer.Form.NFD);
    
    System.out.println("NFD Normalized Precomposed LATIN SMALL LETTER E WITH ACUTE: '" + precomposedNFD + "'");
    System.out.println("NFD NormalizedPrecomposed CODE POINTS: " + toCodePoints(precomposedNFD));
    
    System.out.println("NFD Normalized Decomposed LETTER SMALL LETTER E FOLLOWED BY COMBINING ACCUTE ACCENT: '" + decomposedNFD + "'");
    System.out.println("NFD Normalized Decomposed CODE POINTS: " + toCodePoints(decomposedNFD));
    
    System.out.println("Are they equal? " + precomposedNFD.equals(decomposedNFD) + "\n");
    
    Assert.assertFalse(precomposed.equals(decomposed));
    Assert.assertTrue(precomposedNFC.equals(decomposedNFC));
    Assert.assertTrue(precomposedNFD.equals(decomposedNFD));
    
  }

  @Test
  public void testASCIIFoldingFilter() throws Exception {
    //<start id="foldingFilter"/>
    String input = "Résumé";
    StandardTokenizer t = new StandardTokenizer(Version.LUCENE_47, new StringReader(input));
    ASCIIFoldingFilter tf = new ASCIIFoldingFilter(t);
    tf.reset();
    while (tf.incrementToken()) {
      String term = tf.getAttribute(CharTermAttribute.class).toString();
      System.err.println(input + " -> " + term);
    }
    tf.close();
    //<end id="foldingFilter"/>
  }
  
  public static String toCodePoints(String s) {
    StringBuilder b = new StringBuilder();
    char[] c = s.toCharArray();
    for (int i=0; i < c.length; i++) {
      int cp = Character.codePointAt(c, i, c.length);
      b.append(Integer.toHexString(cp) + " ");
    }
    return b.toString();
  }
}
