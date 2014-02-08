package com.tamingtext.classifier.mlt;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

/** Encapsulates knowledge of the structure of to 20 newsgroups
 *  corpus.
 */
public class TwentyNewsgroupsCorpus {
  final Logger log = Logger.getLogger(TwentyNewsgroupsCorpus.class);
  
  File baseDir;
  File[] categoryDirs;
  
  /**
   * Create a reader for the 20 newsgroups data stored
   * at the specified location.
   * @param baseDir
   */
  public TwentyNewsgroupsCorpus(File baseDir) {
    this.baseDir = baseDir;
    this.categoryDirs = baseDir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    });
  }
  
  public int getCategoryCount() {
    return categoryDirs.length;
  }

  /** 
   * Read the directory structure from the 20 newsgroups
   * corpus and pass the categories and files back to 
   * a callback function that will implement processing.
   * @param cb
   */
  public void process(final Callback cb) {
    for (final File categoryDir: categoryDirs) {
      categoryDir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          try {
            cb.process(categoryDir.getName(), pathname);
          }
          catch (Exception e) {
            log.error("Exception processing file " + pathname);
          }
          return false; // just a hook for processing
        }
      });
    }
  }

  /** 
   * Implementations receive one callback for each
   * data file found in the 20 newsgroups data structure.
   */
  public static interface Callback {
    public void process(String label, File inputFile) throws IOException;
  }
  
  /** Utility method for reading a file, optionally including or
   *  excluding the header
   * @param dataFile
   * @param includeHeader
   * @return
   * @throws IOException
   */
  public static String readFile(File dataFile, boolean includeHeader) throws IOException {
    StringBuilder b = new StringBuilder();
    Reader r = new FileReader(dataFile);
    
    try {
      CharStreams.copy(r, b);
      int pos = 0;
      
      if (!includeHeader) {
        pos = b.indexOf("\n\n");
        if (pos < 0) pos = 0;
      }
      
      if (pos > 0) {
        return b.subSequence(pos, b.length()).toString().trim();
      }
      else {
        return b.toString().trim();
      }
    }
    finally {
      Closeables.closeQuietly(r);
    }
  }
}
