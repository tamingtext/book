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
import java.io.Reader;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;


public class MoreLikeThisQueryTest {
  int nGramSize;
  String inputPath;
  String modelPath;
  int maxResults;
  String categoryFieldName;
  
  @Before
  public void setup() {
    nGramSize = 1;
    inputPath = "src/test/resources/classifier/mlt/sample-input.txt";
    modelPath = "src/test/resources/classifier/mlt/sample-model";
    maxResults = 100;
    categoryFieldName = "category";
  }
  @Test
  public void testMoreLikeThisQuery() throws Exception {
    //<start id="lucene.examples.mlt.setup"/>
    Directory directory = FSDirectory.open(new File(modelPath));
    
    IndexReader indexReader = IndexReader.open(directory); //<co id="mlt.indexsetup"/>
    IndexSearcher indexSearcher = new IndexSearcher(indexReader);

    Analyzer analyzer //<co id="mlt.analyzersetup"/>
      = new EnglishAnalyzer(Version.LUCENE_36);
    
    if (nGramSize > 1) { //<co id="mlt.ngramsetup"/>
      analyzer = new ShingleAnalyzerWrapper(analyzer, nGramSize,
              nGramSize);
    }
    
    MoreLikeThis moreLikeThis  = new MoreLikeThis(indexReader); //<co id="mlt.configure"/>
    moreLikeThis.setAnalyzer(analyzer);
    moreLikeThis.setFieldNames(new String[] {
      "content"
    });
    
/*<calloutlist>
<callout arearefs="mlt.indexsetup">Open Index</callout>
<callout arearefs="mlt.analyzersetup">Setup Analyzer</callout>
<callout arearefs="mlt.ngramsetup">Setup NGrams</callout>
<callout arearefs="mlt.configure">Create <classname>MoreLikeThis</classname></callout>
</calloutlist>*/
    //<end id="lucene.examples.mlt.setup"/>
    
    // for testing against the same corpus
    moreLikeThis.setMinTermFreq(1);
    moreLikeThis.setMinDocFreq(1);
    
    //<start id="lucene.examples.mlt.query"/>
    Reader reader = new FileReader(inputPath); //<co id="mlt.query"/>
    Query query = moreLikeThis.like(reader); 

    TopDocs results 
      = indexSearcher.search(query, maxResults); //<co id="mlt.search"/>
    
    HashMap<String, CategoryHits> categoryHash 
      = new HashMap<String, CategoryHits>();
    
    for (ScoreDoc sd: results.scoreDocs) { //<co id="mlt.collect"/>
      Document d = indexReader.document(sd.doc);
      Fieldable f = d.getFieldable(categoryFieldName);
      String cat = f.stringValue();
      CategoryHits ch = categoryHash.get(cat);
      if (ch == null) {
        ch = new CategoryHits();
        ch.setLabel(cat);
        categoryHash.put(cat, ch);
      }
      ch.incrementScore(sd.score);
    }

    SortedSet<CategoryHits> sortedCats //<co id="mlt.rank"/>
      = new TreeSet<CategoryHits>(CategoryHits.byScoreComparator());
    sortedCats.addAll(categoryHash.values());
    
    for (CategoryHits c: sortedCats) { //<co id="mlt.display"/>
      System.out.println(
          c.getLabel() + "\t" + c.getScore());
    }
    /*<calloutlist>
    <callout arearefs="mlt.query">Create Query</callout>
    <callout arearefs="mlt.search">Perform Search</callout>
    <callout arearefs="mlt.collect">Collect Results</callout>
    <callout arearefs="mlt.rank">Rank Categories</callout>
    <callout arearefs="mlt.display">Display Categories</callout>
    </calloutlist>*/
    //<end id="lucene.examples.mlt.query"/>
    
    
    
  }
}
