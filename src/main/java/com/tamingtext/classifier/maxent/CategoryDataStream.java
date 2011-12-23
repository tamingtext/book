package com.tamingtext.classifier.maxent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import opennlp.maxent.DataStream;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

import org.slf4j.Logger;

public class CategoryDataStream implements DataStream {

  public static final Logger log = 
    org.slf4j.LoggerFactory.getLogger(CategoryDataStream.class);

  File[] inputFiles;
  int inputFilesIndex = 0;
  
  String encoding;
  BufferedReader reader;
  Tokenizer tokenizer;
  
  String line;
  File currentFile;
  
  public CategoryDataStream(File[] inputFiles, Tokenizer tokenizer) {
    this(inputFiles, "UTF-8", tokenizer);
  }
  
  public CategoryDataStream(File[] inputFiles, String encoding, Tokenizer tokenizer) {
    this.inputFiles = inputFiles;
    this.encoding   = encoding;
    
    if (tokenizer == null) {
      this.tokenizer = new SimpleTokenizer();
    }
    else {
      this.tokenizer = tokenizer;
    }
  }
  
  public CategoryDataStream(String fileName, Tokenizer tokenizer) {
    this(fileName, "UTF-8", tokenizer);
  }
  
  public CategoryDataStream(String fileName, String encoding, Tokenizer tokenizer) {
    this((File[]) null, encoding, tokenizer);
    
    inputFiles = new File[1];
    inputFiles[0] = new File(fileName);
  }

  /** Set the current buffered line to null, and attempt to obtain the next 
   *  line of training data, if we run out of lines in one file, move on to
   *  the next. If we are out of files, line will remain null when this
   *  method returns.
   *  
   *  @throws RuntimeException if there's a problem reading any of the input
   *    files.
   */
  protected void getNextLine() {
    line = null;
    
    try {
      while (line == null) {
        if (reader == null) {
          // no more files to read;
          if (inputFilesIndex >= inputFiles.length) break;
          
          // open the next file.
          currentFile = inputFiles[inputFilesIndex];
          reader = new BufferedReader(
              new InputStreamReader(
                  new FileInputStream(currentFile), 
                  encoding));
        }
        
        line = reader.readLine();
        
        if (line == null) {
          // done with this reader, move to the next file.
          reader = null;
          inputFilesIndex++;
        }
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Error reading input from: " + currentFile, e);
    }
  }
  
  
  
  public boolean hasNext() { 
    getNextLine();
    return line != null;
  }
  
//<start id="maxent.examples.train.event"/>
  public DocumentSample nextToken() {  
    int split = line.indexOf('\t');
    if (split < 0) 
      throw new RuntimeException("Invalid line in " 
          + inputFiles[inputFilesIndex]);
    String category = line.substring(0,split);
    String document = line.substring(split+1);
    String[] tokens = tokenizer.tokenize(document);
    return new DocumentSample(category, tokens);
  }
//<end id="maxent.examples.train.event"/>
}