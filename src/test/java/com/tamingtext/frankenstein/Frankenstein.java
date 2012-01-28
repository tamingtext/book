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


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse Frankenstein book (located in the test resources folder), identifies sentences and then
 * indexes them into Lucene.
 */
public class Frankenstein {
  private RAMDirectory directory;
  private IndexSearcher searcher;

  public static void main(String[] args) throws Exception {
    //Index the book
    Frankenstein frankenstein = new Frankenstein();
    //<start id="frank.start"/>
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
  }

  /**
   * Search for the queryStr in the text
   * @param queryStr The query string
   * @return
   * @throws IOException
   * @throws ParseException
   */
  private Results search(String queryStr) throws IOException, ParseException {
    System.out.println("Searching for: " + queryStr);
    if (searcher == null){
      searcher = new IndexSearcher(directory, true);
    }
    Results result = new Results();
    QueryParser qp = new QueryParser(Version.LUCENE_34, "paragraph", new StandardAnalyzer(Version.LUCENE_34));
    org.apache.lucene.search.Query query = qp.parse(queryStr);
    TopDocs topDocs = searcher.search(query, 10);
    System.out.println("Found " + topDocs.totalHits + " total hits.");
    for (int i = 0; i < topDocs.scoreDocs.length; i++){
      Document theDoc = searcher.doc(topDocs.scoreDocs[i].doc);
      String paragraph = theDoc.get("paragraph");
      result.matches.add(paragraph);
    }

    return result;
  }

  /**
   * Index the content of Frankenstein
   * @throws IOException
   */
  private void index() throws IOException {
    InputStream stream = getClass().getClassLoader().getResourceAsStream("frankenstein-gutenberg.txt");
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    //let's index paragraphs at a time
    IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_34, new StandardAnalyzer(Version.LUCENE_34));
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
        addMetadata(doc, paraBuffer, lines, paragraphs, paragraphLines);
        doc.add(new Field("paragraph", paraBuffer.toString(), Field.Store.YES, Field.Index.ANALYZED));//add the main content
        iw.addDocument(doc);//Index the document
        paragraphs++;
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

  private void addMetadata(Document doc, StringBuilder paraBuffer, int lines, int paragraphs, int paragraphLines) {
    doc.add(new Field("frank_" + paragraphs, paraBuffer.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    NumericField startLine = new NumericField("startLine", Field.Store.YES, true);
    startLine.setIntValue(lines - paragraphLines);
    doc.add(startLine);
    NumericField finishLine = new NumericField("finishLine", Field.Store.YES, true);
    finishLine.setIntValue(lines);
    doc.add(startLine);
    NumericField paragraphNumber = new NumericField("paragraph", Field.Store.YES, true);
    finishLine.setIntValue(paragraphs);
    doc.add(paragraphNumber);
  }

  private static void displayResults(Results results) {
  }

  private static String getQuery() throws IOException {
    System.out.println("Type your query.  Hit Enter to process the query (the empty string will exit the program): ");
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
    String line = in.readLine();

    if (line == null || line.length() == -1 || line.equals("")) {
      return null;
    }
    return line;
  }

}

class Results {
  public List<String> matches = new ArrayList<String>();

}
