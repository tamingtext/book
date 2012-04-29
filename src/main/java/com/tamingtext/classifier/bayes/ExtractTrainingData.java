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

package com.tamingtext.classifier.bayes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.math.map.OpenObjectIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A utility to extract training data from a Lucene index using document term vectors to recreate the list of terms
 *  found in each document. Writes output in Mahout Bayes classifier input format */
public class ExtractTrainingData {
  
  private static final Logger log = LoggerFactory.getLogger(ExtractTrainingData.class);
  
  static final Map<String, PrintWriter> trainingWriters = new HashMap<String, PrintWriter>();
  
  public static void main(String[] args) {
    
    log.info("Command-line arguments: " + Arrays.toString(args));
    
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    
    Option inputOpt = obuilder.withLongName("dir")
      .withRequired(true)
      .withArgument(
        abuilder.withName("dir")
          .withMinimum(1)
          .withMaximum(1).create())
      .withDescription("Lucene index directory containing input data")
      .withShortName("d").create();

    Option categoryOpt = obuilder.withLongName("categories")
    .withRequired(true)
    .withArgument(
      abuilder.withName("file")
        .withMinimum(1)
        .withMaximum(1).create())
    .withDescription("File containing a list of categories")
    .withShortName("c").create();
    
    Option outputOpt = obuilder.withLongName("output")
      .withRequired(false)
      .withArgument(
        abuilder.withName("output")
          .withMinimum(1)
          .withMaximum(1).create())
      .withDescription("Output directory")
      .withShortName("o").create();

    Option categoryFieldsOpt = 
      obuilder.withLongName("category-fields")
      .withRequired(true)
      .withArgument(
        abuilder.withName("fields")
          .withMinimum(1)
          .withMaximum(1)
          .create())
      .withDescription("Fields to match categories against (comma-delimited)")
      .withShortName("cf").create();
    
    Option textFieldsOpt = 
      obuilder.withLongName("text-fields")
      .withRequired(true)
      .withArgument(
        abuilder.withName("fields")
          .withMinimum(1)
          .withMaximum(1)
          .create())
      .withDescription("Fields from which to extract training text (comma-delimited)")
      .withShortName("tf").create();
    
    Option useTermVectorsOpt = obuilder.withLongName("use-term-vectors")
      .withDescription("Extract term vectors containing preprocessed data " +
          "instead of unprocessed, stored text values")
      .withShortName("tv").create();
    
    Option helpOpt = obuilder.withLongName("help")
      .withDescription("Print out help")
      .withShortName("h").create();
    
    Group group = gbuilder.withName("Options")
      .withOption(inputOpt)
      .withOption(categoryOpt)
      .withOption(outputOpt)
      .withOption(categoryFieldsOpt)
      .withOption(textFieldsOpt)
      .withOption(useTermVectorsOpt)
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
      
      File categoryFile = new File(cmdLine.getValue(categoryOpt).toString());
      
      if (!categoryFile.isFile()) {
        throw new IllegalArgumentException(categoryFile + " does not exist or is not a directory");
      }
      
      File outputDir = new File(cmdLine.getValue(outputOpt).toString());
      
      outputDir.mkdirs();
      
      if (!outputDir.isDirectory()) {
        throw new IllegalArgumentException(outputDir + " is not a directory or could not be created");
      }

      Collection<String> categoryFields = stringToList(cmdLine.getValue(categoryFieldsOpt).toString());
      
      if (categoryFields.size() < 1) {
        throw new IllegalArgumentException("At least one category field must be spcified.");
      }
      
      Collection<String> textFields = stringToList(cmdLine.getValue(textFieldsOpt).toString());

      if (categoryFields.size() < 1) {
        throw new IllegalArgumentException("At least one text field must be spcified.");
      }
      
      boolean useTermVectors = cmdLine.hasOption(useTermVectorsOpt);
      
      extractTraininingData(inputDir, categoryFile, categoryFields, textFields, outputDir, useTermVectors);
      
    } catch (OptionException e) {
      log.error("Exception", e);
      CommandLineUtil.printHelp(group);
    } catch (IOException e) {
      log.error("IOException", e);
    } finally {
      closeWriters();
    }
  }

  /**
   * Extract training data from a lucene index. 
   * <p>
   * Iterates over documents in the lucene index, the values in the categoryFields are inspected and if found to 
   * contain any of the strings found in the category file, a training data item will be emitted, assigned to the
   * matching category and containing the terms found in the fields listed in textFields. Output is written to
   * the output directory with one file per category.
   * <p>
   * The category file contains one line per category, each line contains a number of whitespace delimited strings. 
   * The first string on each line is the category name, while subsequent strings will be used to identify documents
   * that belong in that category.
   * <p>
   * 'Technology Computers Macintosh' will cause documents that contain either 'Technology', 'Computers' or 'Machintosh'
   * in one of their categoryFields to be assigned to the 'Technology' category.
   * 
   * 
   * @param indexDir 
   *   directory of lucene index to extract from
   *   
   * @param maxDocs
   *   the maximum number of documents to process.
   *   
   * @param categoryFile
   *   file containing category strings to extract
   *   
   * @param categoryFields
   *   list of fields to match against category data
   *   
   * @param textFields
   *   list of fields containing terms to extract
   *   
   * @param outputDir
   *   directory to write output to
   *   
   * @throws IOException
   */
  public static void extractTraininingData(File indexDir, File categoryFile, 
      Collection<String> categoryFields, Collection<String> textFields, File outputDir, boolean useTermVectors) throws IOException {
    
    log.info("Index dir: " + indexDir);
    log.info("Category file: " + categoryFile);
    log.info("Output dir: " + outputDir);
    log.info("Category fields: " + categoryFields.toString());
    log.info("Text fields: " + textFields.toString());
    log.info("Use Term Vectors?: " + useTermVectors);
    OpenObjectIntHashMap<String> categoryCounts = new OpenObjectIntHashMap<String>();
    Map<String, List<String>> categories = readCategoryFile(categoryFile);
    
    Directory dir = FSDirectory.open(indexDir);
    IndexReader reader = IndexReader.open(dir, true);
    int max = reader.maxDoc();
    
    StringBuilder buf = new StringBuilder();
    
    for (int i=0; i < max; i++) {
      if (!reader.isDeleted(i)) {
        Document d = reader.document(i);
        String category = null;
        
        // determine whether any of the fields in this document contain a 
        // category in the category list
        fields: for (String field: categoryFields) {
          for (Field f: d.getFields(field)) {
            if (f.isStored() && !f.isBinary()) {
              String fieldValue = f.stringValue().toLowerCase();
              for (String cat: categories.keySet()) {
                List<String> cats = categories.get(cat);
                for (String c: cats) {
                  if (fieldValue.contains(c)) {
                    category = cat;
                    break fields;
                  }
                }
              }
            }
          }
        }
        
        if (category == null) continue;
        
        // append the terms from each of the textFields to the training data for this document.
        buf.setLength(0);
        for (String field: textFields) {
          if (useTermVectors) {
            appendVectorTerms(buf, reader.getTermFreqVector(i, field));
          }
          else {
            appendFieldText(buf, d.getField(field));
          }
        }
        getWriterForCategory(outputDir, category).printf("%s\t%s\n", category, buf.toString());
        categoryCounts.adjustOrPutValue(category, 1, 1);
      }
    }
    
    if (log.isInfoEnabled()) {
      StringBuilder b = new StringBuilder();
      b.append("\nCatagory document counts:\n");
      LinkedList<String> keyList = new LinkedList<String>();
      categoryCounts.keysSortedByValue(keyList);
      String key;
      while (!keyList.isEmpty()) {
        key = keyList.removeLast();
        b.append(categoryCounts.get(key)).append('\t').append(key).append('\n');
      }
      log.info(b.toString());
    }
  }

  /** Read the category file from disk, see {@link #extractTraininingData(File, File, Collection, Collection, File)}
   *  for a description of the format.
   * 
   * @param categoryFile
   * @return
   * @throws IOException
   */
  public static Map<String,List<String>> readCategoryFile(File categoryFile) throws IOException {
    Map<String,List<String>> categoryMap = new HashMap<String, List<String>>();
    BufferedReader rin = new BufferedReader(new InputStreamReader(new FileInputStream(categoryFile), "UTF-8"));
    String line;
    while ((line = rin.readLine()) != null) {
      String[] parts = line.trim().toLowerCase().split("\\s+");
      if (parts.length > 0) {
        String key = parts[0];
        for (String e: parts) {
          List<String> entries = categoryMap.get(key);
          if (entries == null) {
            entries = new LinkedList<String>();
            categoryMap.put(key, entries);
          }
          entries.add(e);
        }
      }
    }
    rin.close();
    return categoryMap;
  }
  /** Obtain a writer for the training data assigned to the the specified category.
   * <p>
   * Maintains an internal hash of writers used for a category which must be closed by {@link #closeWriters()}.
   * <p>
   * 
   * @param outputDir
   * @param category
   * @return
   * @throws IOException
   */
  protected static PrintWriter getWriterForCategory(File outputDir, String category) throws IOException {
    PrintWriter out = trainingWriters.get(category);
    if (out == null) {
      out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outputDir, category))));
      trainingWriters.put(category, out);
    }
    return out;
  }
  
  /** Close writers opened by {@link #getWriterForCategory(File, String)} */
  protected static void closeWriters() {
    for (PrintWriter p: trainingWriters.values()) {
        p.close();
    }
  }
  
  /** Append the contents of the specified termVector to a buffer containing a list of terms
   * 
   * @param buf
   * @param tv
   */
  protected static void appendVectorTerms(StringBuilder buf, TermFreqVector tv) {
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

  /** Append the contents of the specified field to buffer containing text,
   *  normalizing whitespace in the process.
   *  
   * @param buf
   * @param f
   */
  protected static void appendFieldText(StringBuilder buf, Field f) {
    if (f == null) return;
    if (f.isBinary()) return;
    if (!f.isStored()) return;
    if (buf.length() > 0) buf.append(' ');
    
    String s = f.stringValue();
    s = s.replaceAll("\\s+", " "); // normalize whitespace.
    buf.append(s);
  }
  
  /** Split a comma-delimited set of strings into a list
   * 
   * @param input
   * @return
   */
  private static Collection<String> stringToList(String input) {
    if (input == null || input.equals("")) return Collections.emptyList();
    String[] parts = input.split("\\s*,\\s*");
    return Arrays.asList(parts);
  }
  
}
