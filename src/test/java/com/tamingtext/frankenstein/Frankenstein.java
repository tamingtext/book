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

package com.tamingtext.frankenstein;


import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.poi.hwpf.usermodel.Paragraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse Frankenstein book (located in the test resources folder), identifies sentences and then
 * indexes them into Lucene.
 */
public class Frankenstein {
  protected RAMDirectory directory;
  protected IndexSearcher searcher;
  protected SentenceDetector sentenceDetector;
  protected Map<String, NameFinderME> finders;
  protected Tokenizer tokenizer;

  public static void main(String[] args) throws Exception {
    //<start id="frank.start"/>
    Frankenstein frankenstein = new Frankenstein();
    frankenstein.init();
    frankenstein.index();//<co id="frank.index"/>
    String query = null;
    while (true) {
      query = getQuery();//<co id="frank.query"/>
      if (query != null) {
        Results results = frankenstein.search(query);//<co id="frank.search"/>
        frankenstein.examineResults(results);//<co id="frank.exam"/>
        displayResults(results);
      } else {
        break;
      }
    }
    /*
    <calloutlist>
        <callout arearefs="frank.index"><para>Make the content searchable</para></callout>
        <callout arearefs="frank.query"><para>Prompt the user for a query</para></callout>
        <callout arearefs="frank.search"><para>Perform the search</para></callout>
        <callout arearefs="frank.exam"><para>Parse the results and show interesting items</para></callout>
    </calloutlist>
    */
    //<end id="frank.start"/>
  }

  private void examineResults(Results results) {
    for (Document match : results.matches) {
      //we have a paragraph, let's break sentences and then do NER
      String[] sentencesStr = sentenceDetector.sentDetect(match.get("paragraph"));

      if (sentencesStr != null && sentencesStr.length > 0) {
        Sentence[] sentences = new Sentence[sentencesStr.length];
        results.sentences.put(match.get("id"), sentences);
        //for each sentence, find named entities
        for (int i = 0; i < sentencesStr.length; i++) {
          sentences[i] = new Sentence(sentencesStr[i]);
          String[] tokens = tokenizer.tokenize(sentencesStr[i]);
          for (Map.Entry<String, NameFinderME> finder : finders.entrySet()) {
            String label = finder.getKey();
            Span[] names = finder.getValue().find(tokens);
            //spans index into the tokens array
            if (names != null && names.length > 0) {
              List<String> values = new ArrayList<String>();
              for (int j = 0; j < names.length; j++) {
                StringBuffer cb = new StringBuffer();
                for (int ti = names[j].getStart(); ti < names[j].getEnd(); ti++) {
                  cb.append(tokens[ti]).append(" ");
                }
                values.add(cb.toString());
              }
              sentences[i].names.put(label, values);
            }
          }
        }
      }
    }
  }

  /**
   * Search for the queryStr in the text
   *
   * @param queryStr The query string
   * @return The Results
   * @throws IOException
   * @throws ParseException
   */
  private Results search(String queryStr) throws IOException, ParseException {
    System.out.println("Searching for: " + queryStr);
    if (searcher == null) {
      searcher = new IndexSearcher(directory, true);
    }
    Results result = new Results();
    QueryParser qp = new QueryParser(Version.LUCENE_36, "paragraph", new StandardAnalyzer(Version.LUCENE_36));
    Query query = qp.parse(queryStr);
    TopDocs topDocs = searcher.search(query, 20);
    System.out.println("Found " + topDocs.totalHits + " total hits.");
    for (int i = 0; i < topDocs.scoreDocs.length; i++) {
      Document theDoc = searcher.doc(topDocs.scoreDocs[i].doc);
      result.matches.add(theDoc);
    }
    return result;
  }

  /**
   * Index the content of Frankenstein
   *
   * @throws IOException
   */
  private void index() throws IOException {
    System.out.println("Indexing Frankenstein");
    InputStream stream = getClass().getClassLoader().getResourceAsStream("frankenstein-gutenberg.txt");
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    //let's index paragraphs at a time
    IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_36, new StandardAnalyzer(Version.LUCENE_36));
    directory = new RAMDirectory();
    IndexWriter iw = new IndexWriter(directory, conf);
    String line;
    StringBuilder paraBuffer = new StringBuilder(2048);
    int lines = 0;
    int paragraphs = 0;
    int paragraphLines = 0;
    while ((line = reader.readLine()) != null) {
      if (line.contains("End of the Project Gutenberg")) {//we are in the license section at the end of the book
        break;
      }
      if (line.startsWith("#")) {//skip comments
        continue;
      }
      //if the line is blank, we have a paragraph, so let's index it
      if (line.matches("^\\s*$") && paraBuffer.length() > 0) {
        Document doc = new Document();
        //We can retrieve by paragraph number if we want
        String theString = paraBuffer.toString();
        theString.trim();
        if (theString.length() > 0 && theString.matches("^\\s*$") == false) {
          addMetadata(doc, lines, paragraphs, paragraphLines);
          doc.add(new Field("paragraph", theString, Field.Store.YES, Field.Index.ANALYZED));//add the main content
          iw.addDocument(doc);//Index the document
          paragraphs++;
        }
        //reset some of our state
        paraBuffer.setLength(0);//we are done w/ this paragraph
        paragraphLines = 0;
      } else {
        paraBuffer.append(line).append(' ');
      }
      lines++;
      paragraphLines++;
    }
    System.out.println("Processed " + lines + " lines.  Paragraphs: " + paragraphs);
    iw.close();
  }

  private void addMetadata(Document doc, int lines, int paragraphs, int paragraphLines) {
    doc.add(new Field("id", "frank_" + paragraphs, Field.Store.YES, Field.Index.NOT_ANALYZED));
    NumericField startLine = new NumericField("startLine", Field.Store.YES, true);
    startLine.setIntValue(lines - paragraphLines);
    doc.add(startLine);
    NumericField finishLine = new NumericField("finishLine", Field.Store.YES, true);
    finishLine.setIntValue(lines);
    doc.add(finishLine);
    NumericField paragraphNumber = new NumericField("paragraphNumber", Field.Store.YES, true);
    paragraphNumber.setIntValue(paragraphs);
    doc.add(paragraphNumber);
  }

  /**
   * Initialize OpenNLP libraries and other resources
   * @throws IOException
   */
  private void init() throws IOException {
    System.out.println("Initializing Frankenstein");
    File models = new File("./opennlp-models");
    File wordnet = new File("./WordNet-3.0");
    if (models.exists() == false) {
      throw new FileNotFoundException("./opennlp-models");
    }
    System.setProperty("model.dir", "./opennlp-models");
    System.setProperty("wordnet.dir", "./WordNet-3.0");

    File modelFile = new File(models, "en-sent.bin");
    InputStream modelStream = new FileInputStream(modelFile);
    SentenceModel model = new SentenceModel(modelStream);
    sentenceDetector = new SentenceDetectorME(model);
    finders = new HashMap<String, NameFinderME>();
    finders.put("Names", new NameFinderME(new TokenNameFinderModel(
            new FileInputStream(getPersonModel()))));
    finders.put("Dates", new NameFinderME(new TokenNameFinderModel(
            new FileInputStream(getDateModel()))));
    finders.put("Locations", new NameFinderME(new TokenNameFinderModel(
            new FileInputStream(getLocationModel()))));

    tokenizer = SimpleTokenizer.INSTANCE;
  }

  private static String getQuery() throws IOException {
    System.out.println("");
    System.out.println("Type your query.  Hit Enter to process the query (the empty string will exit the program):");
    System.out.print('>');
    System.out.flush();
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
    String line = in.readLine();

    if (line == null || line.length() == -1 || line.equals("")) {
      return null;
    }
    return line;
  }

  private static void displayResults(Results results) {
    int k = 0;
    for (Document document : results.matches) {
      System.out.println("-----------------------------------");
      System.out.println("Match: [" + k + "] Paragraph: " + document.get("paragraphNumber"));
      System.out.println("Lines: " + document.get("startLine") + "-" + document.get("finishLine"));
      System.out.println("\t" + document.get("paragraph"));
      System.out.println("\t----- Sentences ----");
      Sentence[] sentences = results.sentences.get(document.get("id"));
      for (int i = 0; i < sentences.length; i++) {
        Sentence sentence = sentences[i];
        System.out.println("\t\t[" + i + "] " + sentence.sentence);
        if (sentence.names.isEmpty() == false) {
          for (Map.Entry<String, List<String>> entry : sentence.names.entrySet()) {
            System.out.println("\t\t>>>> " + entry.getKey());
            StringBuffer buff = new StringBuffer();

            if (entry.getValue().isEmpty() == false) {
              for (String val : entry.getValue()) {
                buff.append(val.trim()).append(", ");
              }
              buff.setLength(buff.length() - 2);//drop the last comma and space
              System.out.println("\t\t\t" + buff);
            }
          }
          System.out.println("");
        }
      }
      k++;
    }
  }

  public static File getWordNetDir() {
    String wordnetDir = System.getProperty("wordnet.dir");

    return new File(wordnetDir);
  }

  public static File getWordNetDictionary() {
    return new File(getWordNetDir(), "dict");
  }

  public static File getModelDir() {
    String modelsDirProp = System.getProperty("model.dir");
    return new File(modelsDirProp);
  }

  public static File getPersonModel() {
    return new File(getModelDir(), "en-ner-person.bin");
  }

  public static File getDateModel() {
    return new File(getModelDir(), "en-ner-date.bin");
  }

  public static File getLocationModel() {
    return new File(getModelDir(), "en-ner-location.bin");
  }

}

class Results {
  public List<Document> matches = new ArrayList<Document>();
  public Map<String, Sentence[]> sentences = new HashMap<String, Sentence[]>();

}

class Sentence {
  public String sentence;
  public Map<String, List<String>> names = new HashMap<String, List<String>>();

  public Sentence(String sent) {
    sentence = sent;
  }
}
