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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.StringDistance;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class MovieMatcher {

  
  
  public MovieMatcher() throws MalformedURLException {
    solr = new CommonsHttpSolrServer(new URL("http://localhost:8983/solr"));
    query = new SolrQuery();
    query.setRows(10);
  }

//<start id="record-matching.candidates"/>  
private SolrServer solr;
private SolrQuery query;

public Iterator<SolrDocument> getCandidates(String title) 
    throws SolrServerException {
    String etitle = escape(title); //<co id="co.rm.escape"/>    
    query.setQuery("title:\""+etitle+"\"");  //<co id="co.rm.quotes"/>
    QueryResponse response = solr.query(query);
    SolrDocumentList dl = response.getResults();
    return dl.iterator();
}
/*
<calloutlist>
<callout arearefs="co.rm.escape"><para>Escaped the title.</para></callout>
<callout arearefs="co.rm.quotes"><para>Title in quotes to prevent tokenization.</para></callout>
</calloutlist>
*/
//<end id="record-matching.candidates"/>

/** Replaces PrecedenceQueryParser.escape(..) -- there is likely a better source for this logic. */
public String escape(String s) {
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < s.length(); i++) {
		char c = s.charAt(i);
		// NOTE: keep this in sync with _ESCAPED_CHAR below!
		if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
			|| c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
				|| c == '*' || c == '?') {
			sb.append('\\');
		}
		sb.append(c);
	}
	return sb.toString(); 
}

//<start id="record-matching.match"/>  
  public String match(String title, int year, Set<String> cast) throws SolrServerException {
    Iterator<SolrDocument> di = getCandidates(title);
    Match bestMatch = null;
    Match secMatch = null;
    while (di.hasNext()) { //<co id="co.rm.docs"/>
      SolrDocument doc = di.next();
      String id = (String) doc.getFieldValue("imdb");
      String ititle = (String) doc.getFieldValue("title");
      Integer iyear = (Integer) doc.getFieldValue("year");
      Set<String> icast = constructCastSet(doc.getFieldValues("cast"));
      float score = score(title,year,cast,ititle,iyear,icast); //<co id="co.rm.score"/>
      if (bestMatch == null || score > bestMatch.score) { //<co id="co.rm.best"/>
        secMatch = bestMatch;
        bestMatch = new Match(score,id,title,year,cast);
      }
      else if (secMatch == null || score > secMatch.score) {//<co id="co.rm.second"/>
        secMatch = new Match(score,id,title,year,cast);
      }
    }
    if (bestMatch == null) {
      return null;
    }
    if (bestMatch.score > 0.75) { //<co id="co.rm.threshold"/>
      if (secMatch != null && secMatch.score >= 0.75) { //<co id="co.rm.second-threshold"/>
        return null;
      }
      return bestMatch.id;
    }
    return null;
  }
  
  private static Set<String> constructCastSet(Collection cast) {
    Set<String> castSet = new HashSet<String>();
    for (Object actor : cast) {
      castSet.add(actor.toString().toLowerCase());
    }
    return castSet;
  }
  
  class Match {
    public float score;
    public String id;
    public String title;
    public int year;
    public Set<String> cast;
    
    public Match(float score, String id, String title, int year, Set<String> cast) {
      this.score = score;
      this.id = id;
      this.title = title;
      this.year = year;
      this.cast = cast;
    }
  }

  /*
  <calloutlist>
  <callout arearefs="co.rm.docs"><para>Iterate through each of the documents.</para></callout>  
  <callout arearefs="co.rm.score"><para>Score each of the documents.</para></callout>    
  <callout arearefs="co.rm.best"><para>Check whether this is the best document.</para></callout>    
  <callout arearefs="co.rm.second"><para>Check whether this is the second best document.</para></callout>      
  <callout arearefs="co.rm.threshold"><para>Verify that the best match's score is larger than the threshold.</para></callout>
  <callout arearefs="co.rm.second-threshold"><para>Check that the second-best match is not larger than the threshold.</para></callout>    
  </calloutlist>
   */
//<end id="record-matching.match"/>

  
//<start id="record-matching.scoring"/>  
private StringDistance sd = new JaroWinklerDistance();
  
private float score(String title1, int year1, Set<String> cast1, 
    String title2, int year2, Set<String> cast2) {
    float titleScore = sd.getDistance(title1.toLowerCase(), //<co id="co.rm.score.jaro"/> 
        title2.toLowerCase()); 
    
    float yearScore = (float) 1/(Math.abs(year1-year2)+1); //<co id="co.rm.score.distance"/>
    
    float castScore = (float) intersectionSize(cast1,cast2)/ //<co id="co.rm.score.inter"/>
                        Math.min(cast1.size(),cast2.size()); 
    return (titleScore*.5f)+ //<co id="co.rm.score.combine"/>
           (yearScore*0.2f)+
           (castScore*0.3f); 
}
  
private int intersectionSize(Set<String> cast1, //<co id="co.rm.score.inter-equals"/>
                             Set<String> cast2) { 
    int size = 0;
    for (String actor : cast1) 
        if (cast2.contains(actor)) size++; 
    return size;
}
  
/*
<calloutlist>
<callout arearefs="co.rm.score.jaro"><para>Use the Jaro-Winkler on titles.</para></callout>
<callout arearefs="co.rm.score.distance"><para>Use the reciprocal on years.</para></callout>    
<callout arearefs="co.rm.score.inter"><para>Use cast overlap percentage.</para></callout>      
<callout arearefs="co.rm.score.combine"><para>Combine the scores into a single score.</para></callout>    
<callout arearefs="co.rm.score.inter-equals"><para>Compute intersection using exact string matching.</para></callout>      
</calloutlist>
*/
//<end id="record-matching.scoring"/>

  
  
  public static void main(String[] args) throws IOException, SolrServerException {
    MovieMatcher mm = new MovieMatcher();
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    int matches = 0;
    int lines = 0;
    for (String line = in.readLine();line != null; line = in.readLine()) {
      String[] parts = line.split("\t");
      int pi=0;
      String id = parts[pi++];
      String title = parts[pi++];
      String ys = parts[pi++];
      int year = 0;
      if (!ys.equals("NULL")) {
        year = Integer.parseInt(ys);        
      }
      Set<String> castSet = constructCastSet(Arrays.asList(parts[pi++].split(",")));
      String imdbId = mm.match(title, year, castSet);
      if (imdbId != null) {
        System.out.println(id+","+imdbId);
        matches++;
      }
      lines++;
    }
    //System.err.println(matches+"/"+lines+" "+((float) matches/lines));
  }
}

