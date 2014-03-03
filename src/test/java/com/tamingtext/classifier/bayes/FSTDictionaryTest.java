package com.tamingtext.classifier.bayes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.junit.Test;

import com.tamingtext.classifier.bayes.FSTDictionary.Entry;
import com.tamingtext.classifier.bayes.FSTDictionaryBuilder.TextDictionaryIterable;

public class FSTDictionaryTest {

  String[][] expected = {
      {"a","1","0"},
      {"back","1","1"},
      {"empir","1","2"},
      {"fellowship","1","3"},
      {"hope","1","4"},
      {"jedi","1","5"},
      {"king","1","6"},
      {"lord","3","7"},
      {"new","1","8"},
      {"of","4","9"},
      {"return","1","10"},
      {"reveng","1","11"},
      {"ring","3","12"},
      {"star","3","13"},
      {"strike","1","14"},
      {"the","5","15"},
      {"tower","1","16"},
      {"two","1","17"},
      {"war","3","18"},
  };
  
  String dictionaryFile = "src/test/resources/classifier/bayes/sample-dictionary.txt";
  String fstOutputFile  = "target/test-output/fst-test/dictionary.fst";
  
  /**
   * Test that we can read the proper values from a text dictionary.
   * @throws IOException
   */
  @Test
  public void testDictionaryIterable() throws IOException {
    Reader r = new FileReader(dictionaryFile);
    int pos = 0;
    for (FSTDictionary.Entry entry: new TextDictionaryIterable(r)) {
      assertEquals("Term mismatch", expected[pos][0], entry.term);
      assertEquals("Term document frequency mismatch", expected[pos][1], String.valueOf(entry.df));
      assertEquals("Term index mismatch", expected[pos][2], String.valueOf(entry.index));
      pos++;
    }
    assertEquals("Number of terms in dictionary", expected.length, pos);
  }
  
  /** Test that we can create an FST from the sample dictionary
   *  and retrieve the proper values from it.
   * @throws IOException
   */
  @Test
  public void testBuildAndReadFST() throws Exception {
    String[] args = {
        "--input",  dictionaryFile,
        "--output", fstOutputFile
    };

    FSTDictionaryBuilder.main(args);
    
    FSTDictionary dict = new FSTDictionary(new FileInputStream(fstOutputFile));
    
    for (String[] e: expected) {
      String term = e[0];
      Entry entry = dict.getEntry(term);
      assertNotNull("An entry was not returned", entry);
      assertEquals("Term document freqency mismatch", e[1], String.valueOf(entry.df));
      assertEquals("Term index mismatch", e[2], String.valueOf(entry.index));
    }
  }
}
