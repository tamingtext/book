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
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainMoreLikeThis {
  
  private static final Logger log = LoggerFactory.getLogger(TrainMoreLikeThis.class);
  
  public static final String CATEGORY_KEY = "categories";
  
  public static enum MatchMode {
    KNN,
    TFIDF
  }
  
  private int nGramSize = 1;
  
  public TrainMoreLikeThis() { }
  
  public void setNGramSize(int nGramSize) {
    this.nGramSize = nGramSize;
  }
  
  public void train(String source, String destination,  MatchMode mode) throws Exception {

    TwentyNewsgroupsCorpus c = new TwentyNewsgroupsCorpus(new File(source));
    IndexingCallback cb;
    
    switch (mode) {
      case TFIDF:
        cb = new TfidfIndexer(destination, nGramSize);
        break;
      case KNN:
        cb = new KnnIndexer(destination, nGramSize);
        break;
      default:
        throw new IllegalStateException("Unknown match mode: " + mode.toString());
    }
    
    long start = System.currentTimeMillis();
    c.process(cb);
    log.info(mode + ": Added " + c.getCategoryCount() + " categories in " + (System.currentTimeMillis() - start) + " msec.");
    cb.close();
    
  }
  
  /** 
   * Defines index management mechanisms for Lucene based classifier model.
   */
  public static abstract class IndexingCallback implements TwentyNewsgroupsCorpus.Callback {
    protected IndexWriter writer;
    protected final Set<String> categories = new HashSet<String>();
    
    protected Field idField;
    protected Field categoryField;
    protected Field contentField;
    {
      //<start id="lucene.examples.fields"/>
      FieldType contentFieldType = new FieldType();
      contentFieldType.setStored(true);
      contentFieldType.setIndexed(true);
      contentFieldType.setTokenized(true);
      contentFieldType.setStoreTermVectors(true);
      contentFieldType.setStoreTermVectorPositions(true);
      contentFieldType.setStoreTermVectorOffsets(true);
      contentFieldType.freeze();
      
      Field idField = new StringField("id", "", Store.YES);
      Field categoryField = new StringField("category", "", Store.YES);
      Field contentField  = new Field("content", "", contentFieldType);
      //<end id="lucene.examples.fields"/>
      
      // for the sake of example clarity.
      this.idField = idField;
      this.categoryField = categoryField;
      this.contentField = contentField;
    }
    
    Analyzer analyzer;

    @SuppressWarnings("resource")
    public IndexingCallback(String pathname, int nGramSize) throws IOException {
    //<start id="lucene.examples.index.setup"/>
      Directory directory //<co id="luc.index.dir"/>
        = FSDirectory.open(new File(pathname));
      Analyzer analyzer   //<co id="luc.index.analyzer"/>
        = new EnglishAnalyzer(Version.LUCENE_47);
      
      if (nGramSize > 1) { //<co id="luc.index.shingle"/>
        ShingleAnalyzerWrapper sw = new ShingleAnalyzerWrapper(analyzer,
              nGramSize, // min shingle size
              nGramSize, // max shingle size
              "-",       // token separator
              true,      // output unigrams
              true);     // output unigrams if no shingles
        analyzer = sw;
      }
      
      IndexWriterConfig config //<co id="luc.index.create"/>
        = new IndexWriterConfig(Version.LUCENE_47, analyzer);
      config.setOpenMode(OpenMode.CREATE);
      IndexWriter writer =  new IndexWriter(directory, config);
      /* <calloutlist>
      <callout arearefs="luc.index.dir">Create Index Directory</callout>
      <callout arearefs="luc.index.analyzer">Setup Analyzer</callout>
      <callout arearefs="luc.index.shingle">Setup Shingle Filter</callout>
      <callout arearefs="luc.index.create">Create <classname>IndexWriter</classname></callout>
      </calloutlist> */
      //<end id="lucene.examples.index.setup"/>
      this.writer = writer;
      this.analyzer = analyzer;
    }
    
    public void close() throws IOException {
      log.info("Starting optimize");
      
      writer.setCommitData(generateUserData(categories));
      writer.commit();
      
      // optimize and close the index.
      writer.close();
      writer = null;
      
      analyzer.close();
      
      log.info("Optimize complete, index closed"); 
    }
  }
  
  /** builds a lucene index suitable for knn based classification. Each category's content is indexed into
   *  separate documents in the index, and the category that has the haghest count in the tip N hits is 
   *  is the category that is assigned.
   * @param inputFiles
   * @param writer
   * @throws Exception
   */
  public static class KnnIndexer extends IndexingCallback {
    public KnnIndexer(String dest, int ngramSize) throws IOException {
      super(dest, ngramSize);
    }

    @Override
    public void process(String category, File inputFile) throws IOException {
      //<start id="lucene.examples.knn.train"/>
      String content = TwentyNewsgroupsCorpus.readFile(inputFile, true);
      categories.add(category);

      Document d = new Document(); //<co id="luc.knn.document"/>
      idField.setStringValue(category + "-" + inputFile.getName());
      categoryField.setStringValue(category);
      contentField.setStringValue(content);
      d.add(idField);
      d.add(categoryField);
      d.add(contentField);
      
      writer.addDocument(d); //<co id="luc.knn.index"/>

      /*<calloutlist>
      <callout arearefs="luc.knn.content">Collect Content</callout>
      <callout arearefs="luc.knn.document">Build Document</callout>
      <callout arearefs="luc.knn.index">Index Document</callout>
      </calloutlist>*/
      //<end id="lucene.examples.knn.train"/>

      log.info("KNN: Added document for category " + category + " named " + inputFile.getName());
    }
  }

  /** builds a lucene index suitable for tfidf based classification. Each categories content is indexed into
   *  a single document in the index, and the best match for a MoreLikeThis query is the category that
   *  is assigned.
   * @param inputFiles
   * @param writer
   * @throws Exception
   */
  public static class TfidfIndexer extends IndexingCallback {
    
    // holds the collected content for each category
    final Map<String, StringBuilder> content = new HashMap<String, StringBuilder>();

    public TfidfIndexer(String dest, int ngramSize) throws IOException {
      super(dest, ngramSize);
    }

    @Override
    public void process(String label, File inputFile) throws IOException {
      StringBuilder b = content.get(label);
      if (b == null) {
        b = new StringBuilder();
        content.put(label, b);
      }
      if (b.length() > 0) {
        b.append("\n");
      }
      b.append(TwentyNewsgroupsCorpus.readFile(inputFile, false));
    }

    public void close() throws IOException {
      //<start id="lucene.examples.tfidf.train"/>
      for (Map.Entry<String, StringBuilder> e: content.entrySet()) { //<co id="luc.tf.content"/>
        String category = e.getKey();
        String content  = e.getValue().toString();
        categories.add(category);
        Document d = new Document(); //<co id="luc.tf.document"/>
        idField.setStringValue(category);
        categoryField.setStringValue(category);
        contentField.setStringValue(content);
        d.add(idField);
        d.add(categoryField);
        d.add(contentField);

        writer.addDocument(d); //<co id="luc.tf.index"/>
        
        /*<calloutlist>
          <callout arearefs="luc.tf.content">Collect Content</callout>
          <callout arearefs="luc.tf.document">Build Document</callout>
          <callout arearefs="luc.tf.index">Index Document</callout>
         </calloutlist>*/
        //<end id="lucene.examples.tfidf.train"/>

        log.info("TFIDF: Added document for category " + category);
      }

      super.close();
    }
  }

  protected static Map<String, String> generateUserData(Collection<String> categories) {
    StringBuilder b = new StringBuilder();
    for (String cat: categories) {
      b.append(cat).append('|');
    }
    b.setLength(b.length()-1);
    Map<String, String> userData = new HashMap<String, String>();
    userData.put(CATEGORY_KEY, b.toString());
    log.info("Categories are: " + userData.get(CATEGORY_KEY));
    return userData;
  }

  public static void main(String[] args) throws Exception {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    
    Option helpOpt = DefaultOptionCreator.helpOption();
    
    Option inputDirOpt = obuilder.withLongName("input").withRequired(true).withArgument(
      abuilder.withName("input").withMinimum(1).withMaximum(1).create()).withDescription(
      "The input directory, containing properly formatted files: "
          + "One doc per line, first entry on the line is the label, rest is the evidence")
        .withShortName("i").create();
    
    Option outputOpt = obuilder.withLongName("output").withRequired(true).withArgument(
      abuilder.withName("output").withMinimum(1).withMaximum(1).create()).withDescription(
      "The output directory").withShortName("o").create();
    
    Option gramSizeOpt = obuilder.withLongName("gramSize").withRequired(false).withArgument(
      abuilder.withName("gramSize").withMinimum(1).withMaximum(1).create()).withDescription(
      "Size of the n-gram. Default Value: 1 ").withShortName("ng").create();
    
    Option typeOpt = obuilder.withLongName("classifierType").withRequired(false).withArgument(
      abuilder.withName("classifierType").withMinimum(1).withMaximum(1).create()).withDescription(
      "Type of classifier: knn|tfidf.").withShortName("type").create();
    
    Group group = gbuilder.withName("Options").withOption(gramSizeOpt).withOption(helpOpt).withOption(
        inputDirOpt).withOption(outputOpt).withOption(typeOpt).create();
    
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
      
      int gramSize = 1;
      if (cmdLine.hasOption(gramSizeOpt)) {
        gramSize = Integer.parseInt((String) cmdLine.getValue(gramSizeOpt));
      }

      String inputPath  = (String) cmdLine.getValue(inputDirOpt);
      String outputPath = (String) cmdLine.getValue(outputOpt);
      TrainMoreLikeThis trainer = new TrainMoreLikeThis();
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
      
      if (gramSize > 1) 
        trainer.setNGramSize(gramSize);
      
      trainer.train(inputPath, outputPath, mode);
      
      
    } catch (OptionException e) {
      log.error("Error while parsing options", e);
    }
  }
}
