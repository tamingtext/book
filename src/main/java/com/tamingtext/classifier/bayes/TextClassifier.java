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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.mahout.classifier.ClassifierResult;
import org.apache.mahout.classifier.naivebayes.AbstractNaiveBayesClassifier;
import org.apache.mahout.classifier.naivebayes.BayesUtils;
import org.apache.mahout.classifier.naivebayes.NaiveBayesModel;
import org.apache.mahout.classifier.naivebayes.StandardNaiveBayesClassifier;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.TF;
import org.apache.mahout.vectorizer.TFIDF;
import org.apache.mahout.vectorizer.Weight;
import org.apache.solr.common.SolrException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Preconditions;

/** Classify a document by performing analysis, vectorization and classification.
 *  <p>
 *  There are hooks here to use this from the command-line or embed it in another
 *  class, e.g: BayesUpdateRequestProcessor.
 */
public class TextClassifier {

  @Parameter(names = { "-i", "--input-file" }, description = "Input File", required = true)
  String inputFile;

  @Parameter(names = { "-m", "--model-dir" }, description = "Model Directory", required = true)
  String modelDirName;

  @Parameter(names = { "-d", "--dictionary"}, description = "FST Dictionary File", required = true)
  String dictionaryName;

  @Parameter(names = { "-l", "--label-file"}, description = "Model Label File", required = true)
  String labelFileName;

  @Parameter(names = { "-w", "--term-weight-method" }, description = "Term Weight Method", required = true)
  String termWeightMethod;

  @Parameter(names = { "-dc", "--default-category" }, description = "Default Category", required = true)
  String defaultCategory;

  @Parameter(names = "--help", help = true)
  boolean help;

  Analyzer analyzer;
  
  protected AbstractNaiveBayesClassifier classifier;
  protected IncrementalVectorizer vectorizer;
  protected Map<Integer, String> labelMap;

  public static void main(String[] args) throws Exception {
    TextClassifier c = new TextClassifier();
    c.run(args);
  }

  public int run(String[] args) throws IOException {
    if (!parseArgs(args))
      return -1;

    analyzer = new BayesSchemaAnalyzer();
    init();
    
    SortedSet<ClassifierResult> result = classify("", new FileReader(inputFile));
    
    for (ClassifierResult r: result) {
      System.err.println(r);
    }
    
    return 0;
  }

  public boolean parseArgs(String[] args) {
    JCommander jc = new JCommander(this);
    try {
      jc.parse(args);
      if (help) {
        usage(null, jc);
        return false;
      }
    } catch (ParameterException ex) {
      usage(ex.getMessage(), jc);
      return false;
    }
    return true;
  }

  public static void usage(String message, JCommander jc) {
    if (message != null)
      System.err.println(message);
    ToolRunner.printGenericCommandUsage(System.err);
    StringBuilder out = new StringBuilder();
    jc.usage(out);
    System.err.println(out);
  }

  /** Initialize the components used to perform classification */
  public void init() throws IOException {
    Preconditions.checkNotNull(analyzer, "Analyzer must be set before calling init()");
    
    Configuration conf = new Configuration();
    FileSystem fs = FileSystem.get(conf);

    Path modelDir = new Path(modelDirName);
    Path labelFile = new Path(labelFileName);

    if (!fs.getFileStatus(modelDir).isDir()) {
      throw new IOException("Model directory " + modelDirName + " does not exist");
    }

    if (!fs.exists(labelFile)) {
      throw new IOException("Label file " + labelFileName + " does not exist");
    }

    Weight weight = null;

    if (termWeightMethod.equalsIgnoreCase("tfidf")) {
      weight = new TFIDF();
    }
    else if (termWeightMethod.equalsIgnoreCase("tf")) {
      weight = new TF();
    }
    else {
      throw new IllegalArgumentException("Unknown term weight method " + termWeightMethod);
    }

    FSTDictionary dictionary = null;
    try {
      Path dictionaryPath = new Path(dictionaryName);
      dictionary = new FSTDictionary(fs.open(dictionaryPath));
    }
    catch (IOException ex) {
      throw new IOException("Exception obtaining dictionary from " + dictionaryName, ex);
    }

    //<start id="mahout.bayes.setup"/>
    Map<Integer, String> labelMap = BayesUtils.readLabelIndex(conf, labelFile);
    NaiveBayesModel model = NaiveBayesModel.materialize(modelDir, conf);
    AbstractNaiveBayesClassifier classifier = new StandardNaiveBayesClassifier(model);
    IncrementalVectorizer vectorizer = new IncrementalVectorizer(analyzer, dictionary, weight);
    //<end id="mahout.bayes.setup"/>

    this.labelMap   = labelMap;
    this.classifier = classifier;
    this.vectorizer = vectorizer;
  }

  /** Classify the input provided. 
   * 
   * @param inputField
   *  the field the input originates from, used to select a tokenizer from
   *  the analyzer.
   * @param content
   *  the content to classify
   * @return
   *  the single best class for the specified content.
   * @throws IOException
   */
  public SortedSet<ClassifierResult> classify(String inputField, Reader reader) throws IOException {
    //<start id="mahout.bayes.classify"/>
    Vector termVector = vectorizer.createVector(inputField, reader);
    Vector scoreVector = classifier.classifyFull(termVector);
    SortedSet<ClassifierResult> result = vectorToResult(scoreVector);
    //<end id="mahout.bayes.classify"/>
    return result;
  }

  static final Comparator<ClassifierResult> byScoreComparator = new Comparator<ClassifierResult>() {
    @Override
    public int compare(ClassifierResult a, ClassifierResult b) {
      if (a.getScore() > b.getScore()) {
        return -1;
      }
      else if (a.getScore() < b.getScore()) {
        return 1;
      }
      return a.getLabel().compareTo(b.getLabel());
    }
  };
  
  /** Convert a scoreVector emitted from the Mahout Bayes classifier to
   *  the single best ClassifierResult
   * @param scoreVector
   * @return
   */
  protected SortedSet<ClassifierResult> vectorToResult(Vector scoreVector) {
    //<start id="mahout.bayes.label"/>
    SortedSet<ClassifierResult> results = new TreeSet<ClassifierResult>(byScoreComparator);
    for (Vector.Element e : scoreVector.nonZeroes()) {
      String category = labelMap.get(e.index());
      ClassifierResult result = new ClassifierResult(category, e.get());
      results.add(result);
    }
    return results;
    //<end id="mahout.bayes.label"/>
  }
}
