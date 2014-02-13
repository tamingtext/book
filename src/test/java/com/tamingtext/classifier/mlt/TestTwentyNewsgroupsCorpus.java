package com.tamingtext.classifier.mlt;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.junit.Test;

public class TestTwentyNewsgroupsCorpus {

  File headerOnlyFile = new File("src/test/resources/classifier/mlt/sample-data/sci.med/59634");
  File fullFile       = new File("src/test/resources/classifier/mlt/sample-data/comp.windows.x/68231");
  @Test
  public void testReadFileWithHeader() throws IOException {
    String content = TwentyNewsgroupsCorpus.readFile(headerOnlyFile, true);
    TestCase.assertEquals(156, content.length());
  }

  @Test
  public void testReadHeaderOnlyFileNoHeader() throws IOException {
    String content = TwentyNewsgroupsCorpus.readFile(headerOnlyFile, false);
    TestCase.assertEquals(0, content.length());
  }
  
  @Test
  public void testReadFileNoHeader() throws IOException {
    String content = TwentyNewsgroupsCorpus.readFile(fullFile, false);
    TestCase.assertEquals(131, content.length());
  }
}
