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

package com.tamingtext.tagrecommender;

import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;

import org.apache.lucene.util.PriorityQueue;
import org.apache.mahout.math.function.ObjectIntProcedure;
import org.apache.mahout.math.map.OpenObjectIntHashMap;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;


public class TagRecommenderClient {
  
  
  public static final String DEFAULT_SOLR_URL = "http://localhost:8984/solr";

  SolrServer server;
  
//<start id="tag.rec.gettags"/>
  public TagRecommenderClient(String solrUrl)
          throws MalformedURLException {
    server = new HttpSolrServer(solrUrl); //<co id="trc.server"/>
  }
  
  public ScoreTag[] getTags(String content, int maxTags) 
      throws SolrServerException {
    ModifiableSolrParams  query   = new ModifiableSolrParams();//<co id="trc.query"/>
    query.set("fq", "postTypeId:1")
      .set("start", 0)
      .set("rows", 10)
      .set("fl", "*,score")
      .set("mlt.interestingTerms", "details");

    MoreLikeThisRequest request //<co id="trc.request"/>
      = new MoreLikeThisRequest(query, content);
    QueryResponse response = request.process(server);

    SolrDocumentList documents = response.getResults(); //<co id="trc.results"/>
    ScoreTag[] rankedTags = rankTags(documents, maxTags);
    return rankedTags;
  }
  /*<calloutlist>
  <callout arearefs="trc.server">Solr client</callout>
  <callout arearefs="trc.query">Query parameters</callout>
  <callout arearefs="trc.request">Create and execute request</callout>
  <callout arearefs="trc.results">Collect and rank tags</callout>
  </calloutlist>*/
  //<end id="tag.rec.gettags"/>

  //<start id="tag.rec.collecttags"/>
  protected ScoreTag[] rankTags(SolrDocumentList documents,
                                int maxTags) {
    OpenObjectIntHashMap<String> counts =
            new OpenObjectIntHashMap<String>();
    
    int size = documents.size(); //<co id="trc.count"/>
    for (int i=0; i < size; i++) {
      Collection<Object> tags = documents.get(i).getFieldValues("tags");
      for (Object o: tags) {
        counts.adjustOrPutValue(o.toString(), 1, 1);
      }
    }
    maxTags = maxTags > counts.size() ? counts.size() : maxTags;
    final ScoreTagQueue pq = new ScoreTagQueue(maxTags); //<co id="trc.rank"/>
    counts.forEachPair(new ObjectIntProcedure<String> () {
      @Override
      public boolean apply(String first, int second) {
        pq.insertWithOverflow(new ScoreTag(first, second));
        return true;
      }
    });
    ScoreTag[] rankedTags = new ScoreTag[maxTags]; //<co id="trc.collect"/>
    int index = maxTags;
    ScoreTag s;
    int m = 0;
    while (pq.size() > 0) {
      s = pq.pop();
      rankedTags[--index] = s;
      m += s.count;
    }
    for (ScoreTag t: rankedTags) { //<co id="trc.score"/>
      t.setScore(t.getCount() / (double) m);
    }
    return rankedTags;
  }
  /*<calloutlist>
  <callout arearefs="trc.count">Count Tags</callout>
  <callout arearefs="trc.rank">Rank Tags</callout>
  <callout arearefs="trc.collect">Collect Ranked Tags</callout>
  <callout arearefs="trc.score">Score Tags</callout>
  </calloutlist>*/
  //<end id="tag.rec.collecttags"/>
  

  /** A priority queue for holding only the highest ranked ScoreTags */
  public static class ScoreTagQueue extends PriorityQueue<ScoreTag> {
    public ScoreTagQueue(int size) {
      this.initialize(size);
    }
    
    @Override
    protected boolean lessThan(ScoreTag a, ScoreTag b) {
      if (a.count == b.count) {
        if (a.tag == null && b.tag == null) return false;
        if (a.tag == null)   return true;
        if (b.tag == null) return false;
        
        return a.tag.compareTo(b.tag) > 0;
      }
      else 
        return (a.count < b.count);
      
    }
  }
  
  /** Hold a tag returned by the Solr query, it's score and the
   *  relative probability of it appearing in relation to the 
   *  other results in the set.
   *
   */
  public static class ScoreTag {
    private String tag;
    private int count;
    private double score;
    
    public ScoreTag(String tag, int score) {
      this.tag = tag;
      this.count = score;
    }

    public String getTag() {
      return tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public int getCount() {
      return count;
    }

    public void setCount(int score) {
      this.count = score;
    }

    public double getScore() {
      return score;
    }

    public void setScore(double prob) {
      this.score = prob;
    }

    @Override
    public String toString() {
      return tag + " " + count + " " + score;
    }
    
  }

  /** Simple utility to read a file to a string */
  public static String readFile(String file) throws IOException {
    StringBuilder buffer = new StringBuilder();
    char[] c = new char[1024];
    int read;
    FileReader fr = new FileReader(file);
    while ((read = fr.read(c)) > 0 ) {
      buffer.append(c, 0, read);
    }
    fr.close();
    return buffer.toString();
  }

  public static void main(String[] args) throws Exception {
    String solrUrl = DEFAULT_SOLR_URL;
    String content = readFile("src/test/resources/classifier/sample-question.txt");
    TagRecommenderClient client = new TagRecommenderClient(solrUrl);
    ScoreTag[] tags = client.getTags(content,5);
    for (ScoreTag t: tags) {
      System.out.println(t);
    }
  }
}
