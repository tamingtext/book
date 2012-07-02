/*
 * Copyright 2008-2011 Grant Ingersoll, Thomas Morton and Drew Farris
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * -------------------
 * To purchase or learn more about Taming Text, by Grant Ingersoll, Thomas Morton and Drew Farris, visit
 * http://www.manning.com/ingersoll
 */

package com.tamingtext.tagging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.IOUtils;
import org.apache.mahout.utils.vectors.lucene.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuceneTagExtractor {
  private static final Logger log = LoggerFactory.getLogger(Driver.class);
  
  public static void main(String[] args) throws IOException {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    
    Option inputOpt = obuilder.withLongName("dir")
      .withRequired(true)
      .withArgument(
        abuilder.withName("dir")
          .withMinimum(1)
          .withMaximum(1).create())
      .withDescription("The Lucene directory")
      .withShortName("d").create();
    
    Option outputOpt = obuilder.withLongName("output")
      .withRequired(false)
      .withArgument(
        abuilder.withName("output")
          .withMinimum(1)
          .withMaximum(1).create())
      .withDescription("The output directory")
      .withShortName("o").create();
    
    Option maxOpt = obuilder.withLongName("max")
      .withRequired(false)
      .withArgument(
        abuilder.withName("max")
          .withMinimum(1)
          .withMaximum(1)
          .create())
      .withDescription("The maximum number of vectors to output.  If not specified, then it will loop over all docs")
      .withShortName("m").create();
    
    Option fieldOpt = 
      obuilder.withLongName("field")
      .withRequired(true)
      .withArgument(
        abuilder.withName("field")
          .withMinimum(1)
          .withMaximum(1)
          .create())
      .withDescription("The field in the index")
      .withShortName("f").create();
    
    Option helpOpt = obuilder.withLongName("help")
      .withDescription("Print out help")
      .withShortName("h").create();
    
    Group group = gbuilder.withName("Options")
      .withOption(inputOpt)
      .withOption(outputOpt)
      .withOption(maxOpt)
      .withOption(fieldOpt)
      .create();
    
    try {
      Parser parser = new Parser();
      parser.setGroup(group);
      CommandLine cmdLine = parser.parse(args);
      
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return;
      }

      File file = new File(cmdLine.getValue(inputOpt).toString());
      
      if (!file.isDirectory()) {
        throw new IllegalArgumentException(file + " does not exist or is not a directory");
      }
      
      
      
      long maxDocs = Long.MAX_VALUE;
      if (cmdLine.hasOption(maxOpt)) {
        maxDocs = Long.parseLong(cmdLine.getValue(maxOpt).toString());
      }
      
      if (maxDocs < 0) {
        throw new IllegalArgumentException("maxDocs must be >= 0");
      }

      String field = cmdLine.getValue(fieldOpt).toString();

      PrintWriter out = null;
      if (cmdLine.hasOption(outputOpt)) {
        out = new PrintWriter(new FileWriter(cmdLine.getValue(outputOpt).toString()));
      }
      else {
        out = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
      }
      
      File output = new File("/home/drew/taming-text/delicious/training");
      output.mkdirs();
      
      emitTextForTags(file, output);

      IOUtils.close(Collections.singleton(out));
    } catch (OptionException e) {
      log.error("Exception", e);
      CommandLineUtil.printHelp(group);
    }
    
  }
  
  public static void dumpTags(File file, String field, long maxDocs) throws IOException {
    Directory dir = FSDirectory.open(file);
    IndexReader reader = IndexReader.open(dir, true);
    TermEnum te = reader.terms(new Term(field, ""));
    do {
      Term term = te.term();
      if (term == null || term.field().equals(field) == false) {
        break;
      }
      System.err.printf("%s %d\n", term.text(), te.docFreq());
    } while (te.next());
    te.close();
  }
  
  public static void emitTextForTags(File file, File output) throws IOException {
    String field = "tag";
    
    Directory dir = FSDirectory.open(file);
    IndexReader reader = IndexReader.open(dir, true);
    TermEnum te = reader.terms(new Term(field, ""));
    StringBuilder buf = new StringBuilder();
    do {
      Term term = te.term();
      if (term == null || term.field().equals(field) == false) {
        break;
      }
      
      if (te.docFreq() > 30) {
        File f = new File(output, term.text() + ".txt");
        PrintWriter pw = new PrintWriter(new FileWriter(f));
        System.err.printf("%s %d\n", term.text(), te.docFreq());
        
        TermDocs td = reader.termDocs(term);
        while (td.next()) {
          int doc = td.doc();
          buf.setLength(0);
          appendVectorTerms(buf, reader.getTermFreqVector(doc, "description-clustering"));
          appendVectorTerms(buf, reader.getTermFreqVector(doc, "extended-clustering"));
          emitTagDoc(term, pw, buf);
        }
        
        pw.close();
      }
    } while (te.next());
    te.close();
  }
  
  public static void emitTagDoc(Term term, PrintWriter pw, StringBuilder b) {
    
    if (b.length() < 100) {
      return;
    }
    
    pw.printf("%s\t%s\n", term.text(), b);
  }
  
  public static void appendVectorTerms(StringBuilder buf, TermFreqVector tv) {
    if (tv == null) return;
    
    String[] terms = tv.getTerms();
    int[] frequencies = tv.getTermFrequencies();
    
    for (int j=0; j < terms.length; j++) {
      int freq = frequencies[j];
      String term = terms[j];
      for (int k=0; k < freq; k++) {
        buf.append(term).append(' ');
      }
    }
  }
  
  public static void emitTermsForTags(PrintWriter out, StringBuilder buf, IndexReader reader, TermFreqVector tv) {
    if (tv == null) return;
    
    String[] terms = tv.getTerms();
      
    for (int j=0; j < terms.length; j++) {
      out.printf("%s\t%s\n", terms[j], buf.toString());
    }
  }
  
  public static void dumpDocs(File indexDir, PrintWriter out, long maxDocs) throws IOException {
    Directory dir = FSDirectory.open(indexDir);
    IndexReader reader = IndexReader.open(dir, true);
    int max = reader.maxDoc();
    
    StringBuilder buf = new StringBuilder();
    
    for (int i=0; i < max; i++) {
      if (!reader.isDeleted(i)) {
        buf.setLength(0);
        appendVectorTerms(buf, reader.getTermFreqVector(i, "description-clustering"));
        appendVectorTerms(buf, reader.getTermFreqVector(i, "extended-clustering"));
        emitTermsForTags(out, buf, reader, reader.getTermFreqVector(i, "tag"));
      }
    }
  }
}
