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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.IOUtils;
import org.apache.mahout.utils.vectors.lucene.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Extract categories from the TamingText cluster schema */
public class LuceneCategoryExtractor {
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
      .withDescription("The maximum number of documents to analyze.  If not specified, then it will loop over all docs")
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

      File inputDir = new File(cmdLine.getValue(inputOpt).toString());
      
      if (!inputDir.isDirectory()) {
        throw new IllegalArgumentException(inputDir + " does not exist or is not a directory");
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

      dumpDocumentFields(inputDir, field, maxDocs, out);

      IOUtils.close(Collections.singleton(out));
    } catch (OptionException e) {
      log.error("Exception", e);
      CommandLineUtil.printHelp(group);
    }
    
  }
  
  /** dump the terms found in the field specified to the specified writer in the form:
   * 
   *  <pre>term(tab)document_frequency</pre>
   *  
   * @param indexDir the index to read.
   * @param field the name of the field.
   * @param out the print writer output will be written to
   * @throws IOException
   */
  public static void dumpTerms(File indexDir, String field, PrintWriter out) throws IOException {
    Directory dir = FSDirectory.open(indexDir);
    IndexReader reader = IndexReader.open(dir, true);
    TermEnum te = reader.terms(new Term(field, ""));
    do {
      Term term = te.term();
      if (term == null || term.field().equals(field) == false) {
        break;
      }
      out.printf("%s %d\n", term.text(), te.docFreq());
    } while (te.next());
    te.close();
  }
  
  /** dump the values stored in the specified field for each document.
   * 
   *  <pre>term(tab)document_frequency</pre>
   *  
   * @param indexDir the index to read.
   * @param field the name of the field.
   * @param out the print writer output will be written to
   * @throws IOException
   */
  public static void dumpDocumentFields(File indexDir, String field, long maxDocs, PrintWriter out) throws IOException {
    Directory dir = FSDirectory.open(indexDir);
    IndexReader reader = IndexReader.open(dir, true);
    int max = reader.maxDoc();
    for (int i=0; i < max; i++) {
      if (!reader.isDeleted(i)) {
        Document d = reader.document(i);
        for (Field f: d.getFields(field)) {
          if (f.isStored() && !f.isBinary()) {
            String value = f.stringValue();
            if (value != null) {
              out.printf("%s\n", value);
            }
          }
        }
      }
    }
  }
}
