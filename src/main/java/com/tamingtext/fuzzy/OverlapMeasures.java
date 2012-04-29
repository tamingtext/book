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

package com.tamingtext.fuzzy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.PatternAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.solr.analysis.PatternTokenizer;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.regex.Pattern;

public class OverlapMeasures {
  
  //<start id="jaccard_end"/>
  public float jaccard(char[] s, char[] t) {
    int intersection = 0;
    int union = s.length+t.length;
    boolean[] sdup = new boolean[s.length];
    union -= findDuplicates(s,sdup);   //<co id="co_fuzzy_jaccard_dups1"/>
    boolean[] tdup = new boolean[t.length]; 
    union -= findDuplicates(t,tdup);
    for (int si=0;si<s.length;si++) { 
      if (!sdup[si]) {   //<co id="co_fuzzy_jaccard_skip1"/>
        for (int ti=0;ti<t.length;ti++) {
          if (!tdup[ti]) {  
            if (s[si] == t[ti]) {   //<co id="co_fuzzy_jaccard_intersection" />
              intersection++; 
              break;
            }
          }
        }
      }
    }
    union-=intersection; 
    return (float) intersection/union; //<co id="co_fuzzy_jaccard_return"/>
  }
  
  private int findDuplicates(char[] s, boolean[] sdup) {
    int ndup =0;
    for (int si=0;si<s.length;si++) {
      if (sdup[si]) {
        ndup++;
      }
      else {
        for (int si2=si+1;si2<s.length;si2++) {
          if (!sdup[si2]) {
            sdup[si2] = s[si] == s[si2];
          }
        }
      }
    }
    return ndup;
  }
  /*
  <calloutlist>
  <callout arearefs="co_fuzzy_jaccard_dups1"><para>Find duplicates and subtract from union.</para></callout>
  <callout arearefs="co_fuzzy_jaccard_skip1"><para>Skip duplicates.</para></callout>
  <callout arearefs="co_fuzzy_jaccard_intersection"><para>Find intersection.</para></callout>
  <callout arearefs="co_fuzzy_jaccard_return"><para>Return Jaccard distance.</para></callout>
  </calloutlist>
   */
  //<end id="jaccard_end"/>

  public TopDocs cosine(String queryTerm, int n, String... terms) throws IOException, ParseException {
    Directory directory = new RAMDirectory();
    final Pattern pattern = Pattern.compile(".");
    Analyzer analyzer = new Analyzer() {
      @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream result = null;
        try {
          result = new PatternTokenizer(reader, pattern, 0);
        } catch (IOException e) {
        }
        return result;
      }
    };
    IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_36, analyzer);
    IndexWriter writer = new IndexWriter(directory, conf);
    for (String term : terms) {
      Document doc = new Document();
      doc.add(new Field("chars", term, Field.Store.YES, Field.Index.ANALYZED));
      writer.addDocument(doc);
    }
    writer.close();
    IndexReader reader = IndexReader.open(directory);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), terms.length);
    for (int i = 0; i < topDocs.scoreDocs.length; i++){
        System.out.println("Id: " + topDocs.scoreDocs[i].doc + " Val: " + searcher.doc(topDocs.scoreDocs[i].doc).get("chars"));
    }
    QueryParser qp = new QueryParser(Version.LUCENE_36, "chars", analyzer);
    Query query = qp.parse(queryTerm);
    return searcher.search(query, n);
  }
}
