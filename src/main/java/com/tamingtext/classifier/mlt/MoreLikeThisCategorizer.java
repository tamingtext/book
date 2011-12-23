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

package com.tamingtext.classifier.mlt;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tamingtext.classifier.mlt.TrainMoreLikeThis.MatchMode;

public class MoreLikeThisCategorizer {
  
  private static final Logger log = LoggerFactory.getLogger(MoreLikeThisCategorizer.class);

  MatchMode matchMode = MatchMode.TFIDF;
  IndexReader indexReader;
  IndexSearcher indexSearcher;
  MoreLikeThis moreLikeThis;
  String categoryFieldName;
  final Set<String> categories = new HashSet<String>();
  boolean captureCategories = false;
  int maxResults = 10;
  
  public MoreLikeThisCategorizer(IndexReader indexReader, String categoryFieldName) throws IOException {
    this.indexReader   = indexReader;
    this.indexSearcher = new IndexSearcher(indexReader);
    this.moreLikeThis  = new MoreLikeThis(indexReader);
    this.categoryFieldName = categoryFieldName;
    loadCategoriesFromIndex();
  }
  
  /** populate the list of categories by reading the values embedded in the index userData, falls back
   *  to scanCategories if the data is not present 
   * @throws IOException
   */
  protected void loadCategoriesFromIndex() throws IOException {
    Map<String, String> userData = indexReader.getCommitUserData();
    String categoryString = userData.get(TrainMoreLikeThis.CATEGORY_KEY);
    if (categoryString == null) {
      scanCategories();
      return;
      
    }
    
    String[] parts = categoryString.split("\\|");
    
    if (parts.length < 1) {
      scanCategories();
      return;
    }
    
    categories.addAll(Arrays.asList(parts));
    log.info("Loaded " + categories.size() + " categories from index");
  }
  
  /** populate the list of categories by reading the values from the categoryField in the index */
  protected void scanCategories() throws IOException {
    TermEnum te = indexReader.terms(new Term(categoryFieldName));
    final Set<String> c = categories;
    
    do {
      if (!te.term().field().equals(categoryFieldName)) break;
      c.add(te.term().text());
    } while (te.next());
    
    log.info("Scanned " + c.size() + " categories from index");
  }
  
  public void setMaxResults(int maxResults) {
    this.maxResults = maxResults;
  }
  
  public Collection<String> getCategories() {
    return Collections.unmodifiableSet(categories);
  }
  
  public MatchMode getMatchMode() {
    return matchMode;
  }

  public void setMatchMode(MatchMode matchMode) {
    this.matchMode = matchMode;
  }

  public void setFieldNames(String[] fieldNames) {
    moreLikeThis.setFieldNames(fieldNames);
  }

  public void setAnalyzer(Analyzer analyzer) {
    moreLikeThis.setAnalyzer(analyzer);
  }
  
  public void setNgramSize(int size) {
    if (size <= 1) return;
    
    Analyzer a = moreLikeThis.getAnalyzer();
    ShingleAnalyzerWrapper sw;
    if (a instanceof ShingleAnalyzerWrapper) {
      sw = (ShingleAnalyzerWrapper) a;
    }
    else {
      sw = new ShingleAnalyzerWrapper(a);
      moreLikeThis.setAnalyzer(sw);
    }
    
    sw.setMaxShingleSize(size);
    sw.setMinShingleSize(size);
  }
  
  public CategoryHits[] categorize(Reader reader) throws IOException {
    Query query = moreLikeThis.like(reader);

    HashMap<String, CategoryHits> categoryHash = new HashMap<String, CategoryHits>(25);
    
    for (ScoreDoc sd: indexSearcher.search(query, maxResults).scoreDocs) {
      String cat = getDocClass(sd.doc);
      if (cat == null) continue;
      CategoryHits ch = categoryHash.get(cat);
      if (ch == null) {
        ch = new CategoryHits();
        ch.setLabel(cat);
        categoryHash.put(cat, ch);
      }

      ch.incrementScore(sd.score);
    }

    SortedSet<CategoryHits> sortedCats = new TreeSet<CategoryHits>(CategoryHits.byScoreComparator());
    sortedCats.addAll(categoryHash.values());
    return sortedCats.toArray(new CategoryHits[0]);
  }
 
  protected String getDocClass(int doc) throws IOException {
    Document d = indexReader.document(doc);
    Fieldable f = d.getFieldable(categoryFieldName);
    if (f == null) return null;
    if (!f.isStored()) throw new IllegalArgumentException("Field " + f.name() + " is not stored.");
    return f.stringValue();
  }
  
  public static void main(String[] args) throws Exception {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    
    Option helpOpt = DefaultOptionCreator.helpOption();
    
    Option inputDirOpt = obuilder.withLongName("input").withRequired(true).withArgument(
      abuilder.withName("input").withMinimum(1).withMaximum(1).create()).withDescription(
      "The input file to classify")
        .withShortName("i").create();
    
    Option modelOpt = obuilder.withLongName("model").withRequired(true).withArgument(
      abuilder.withName("index").withMinimum(1).withMaximum(1).create()).withDescription(
      "The directory containing the index model").withShortName("m").create();
    
    Option categoryFieldOpt = obuilder.withLongName("categoryField").withRequired(true).withArgument(
        abuilder.withName("index").withMinimum(1).withMaximum(1).create()).withDescription(
        "Name of the field containing category information").withShortName("catf").create();

    Option contentFieldOpt = obuilder.withLongName("contentField").withRequired(true).withArgument(
        abuilder.withName("index").withMinimum(1).withMaximum(1).create()).withDescription(
        "Name of the field containing content information").withShortName("contf").create();
    
    Option maxResultsOpt = obuilder.withLongName("maxResults").withRequired(false).withArgument(
        abuilder.withName("gramSize").withMinimum(1).withMaximum(1).create()).withDescription(
        "Number of results to retrive, default: 10 ").withShortName("r").create();
    
    Option gramSizeOpt = obuilder.withLongName("gramSize").withRequired(false).withArgument(
      abuilder.withName("gramSize").withMinimum(1).withMaximum(1).create()).withDescription(
      "Size of the n-gram. Default Value: 1 ").withShortName("ng").create();
    
    Option typeOpt = obuilder.withLongName("classifierType").withRequired(false).withArgument(
      abuilder.withName("classifierType").withMinimum(1).withMaximum(1).create()).withDescription(
      "Type of classifier: knn|tfidf. Default: bayes").withShortName("type").create();
    
    Group group = gbuilder.withName("Options").withOption(gramSizeOpt).withOption(helpOpt).withOption(
        inputDirOpt).withOption(modelOpt).withOption(typeOpt).withOption(contentFieldOpt)
        .withOption(categoryFieldOpt).withOption(maxResultsOpt)
        .create();
    
    try {
      Parser parser = new Parser();
      
      parser.setGroup(group);
      parser.setHelpOption(helpOpt);
      CommandLine cmdLine = parser.parse(args);
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return;
      }
      
      String classifierType = (String) cmdLine.getValue(typeOpt);
      
      if (cmdLine.hasOption(gramSizeOpt)) {
        
      }
      
      int gramSize = 1;
      if (cmdLine.hasOption(gramSizeOpt)) {
        gramSize = Integer.parseInt((String) cmdLine.getValue(gramSizeOpt));
      }

      int maxResults = 10;
      if (cmdLine.hasOption(maxResultsOpt)) {
        maxResults = Integer.parseInt((String) cmdLine.getValue(maxResultsOpt));
      }
      
      String inputPath  = (String) cmdLine.getValue(inputDirOpt);
      String modelPath = (String) cmdLine.getValue(modelOpt);
      String categoryField = (String) cmdLine.getValue(categoryFieldOpt);
      String contentField = (String) cmdLine.getValue(contentFieldOpt);
      
      MatchMode mode;
      
      if ("knn".equalsIgnoreCase(classifierType)) {
        mode = MatchMode.KNN;
      } 
      else if ("tfidf".equalsIgnoreCase(classifierType)) {
        mode = MatchMode.TFIDF;
      }
      else {
        throw new IllegalArgumentException("Unkown classifierType: " + classifierType);
      }

      Reader reader = new FileReader(inputPath);
      Directory directory = FSDirectory.open(new File(modelPath));
      IndexReader indexReader = IndexReader.open(directory);
      MoreLikeThisCategorizer categorizer = new MoreLikeThisCategorizer(indexReader, categoryField);
      categorizer.setMatchMode(mode);
      categorizer.setFieldNames(new String[]{ contentField });
      categorizer.setMaxResults(maxResults);
      
      if (gramSize > 1) 
        categorizer.setNgramSize(gramSize);
      
      
      CategoryHits[] categories = categorizer.categorize(reader);
      for (CategoryHits c: categories) {
        System.out.println(c.getLabel()+ "\t" + c.getHits() + "\t" + c.getScore());
      }
      
    } catch (OptionException e) {
      log.error("Error while parsing options", e);
    }
  }  
}
