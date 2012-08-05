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

package com.tamingtext.qa;


import com.tamingtext.texttamer.solr.NameFilter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.PriorityQueue;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Given a SpanQuery, get windows around the matches and rank those results
 */
public class PassageRankingComponent extends SearchComponent implements PluginInfoInitialized, SolrCoreAware, QAParams {
  private transient static Logger log = LoggerFactory.getLogger(PassageRankingComponent.class);

  static final String NE_PREFIX_LOWER = NameFilter.NE_PREFIX.toLowerCase();

  public static final int DEFAULT_PRIMARY_WINDOW_SIZE = 25;
  public static final int DEFAULT_ADJACENT_WINDOW_SIZE = 25;
  public static final int DEFAULT_SECONDARY_WINDOW_SIZE = 25;

  public static final float DEFAULT_ADJACENT_WEIGHT = 0.5f;
  public static final float DEFAULT_SECOND_ADJACENT_WEIGHT = 0.25f;
  public static final float DEFAULT_BIGRAM_WEIGHT = 1.0f;

  @Override
  public void init(PluginInfo pluginInfo) {

  }

  @Override
  public void inform(SolrCore solrCore) {

  }


  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    SolrParams params = rb.req.getParams();
    if (!params.getBool(COMPONENT_NAME, false)) {
      return;
    }


  }

  @Override
  public void process(ResponseBuilder rb) throws IOException {
    SolrParams params = rb.req.getParams();
    if (!params.getBool(COMPONENT_NAME, false)) {
      return;
    }
    Query origQuery = rb.getQuery();
    //TODO: longer term, we don't have to be a span query, we could re-analyze the document
    if (origQuery != null) {
      if (origQuery instanceof SpanNearQuery == false) {
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Illegal query type.  The incoming query must be a Lucene SpanNearQuery and it was a " + origQuery.getClass().getName());
      }
      SpanNearQuery sQuery = (SpanNearQuery) origQuery;
      SolrIndexSearcher searcher = rb.req.getSearcher();
      IndexReader reader = searcher.getIndexReader();
      Spans spans = sQuery.getSpans(reader);
      //Assumes the query is a SpanQuery
      //Build up the query term weight map and the bi-gram
      Map<String, Float> termWeights = new HashMap<String, Float>();
      Map<String, Float> bigramWeights = new HashMap<String, Float>();
      createWeights(params.get(CommonParams.Q), sQuery, termWeights, bigramWeights, reader);
      float adjWeight = params.getFloat(ADJACENT_WEIGHT, DEFAULT_ADJACENT_WEIGHT);
      float secondAdjWeight = params.getFloat(SECOND_ADJ_WEIGHT, DEFAULT_SECOND_ADJACENT_WEIGHT);
      float bigramWeight = params.getFloat(BIGRAM_WEIGHT, DEFAULT_BIGRAM_WEIGHT);
      //get the passages
      int primaryWindowSize = params.getInt(QAParams.PRIMARY_WINDOW_SIZE, DEFAULT_PRIMARY_WINDOW_SIZE);
      int adjacentWindowSize = params.getInt(QAParams.ADJACENT_WINDOW_SIZE, DEFAULT_ADJACENT_WINDOW_SIZE);
      int secondaryWindowSize = params.getInt(QAParams.SECONDARY_WINDOW_SIZE, DEFAULT_SECONDARY_WINDOW_SIZE);
      WindowBuildingTVM tvm = new WindowBuildingTVM(primaryWindowSize, adjacentWindowSize, secondaryWindowSize);
      PassagePriorityQueue rankedPassages = new PassagePriorityQueue();
      //intersect w/ doclist
      DocList docList = rb.getResults().docList;
      while (spans.next() == true) {
        //build up the window
        if (docList.exists(spans.doc())) {
          tvm.spanStart = spans.start();
          tvm.spanEnd = spans.end();
          reader.getTermFreqVector(spans.doc(), sQuery.getField(), tvm);
          //The entries map contains the window, do some ranking of it
          if (tvm.passage.terms.isEmpty() == false) {
            log.debug("Candidate: Doc: {} Start: {} End: {} ",
                    new Object[]{spans.doc(), spans.start(), spans.end()});
          }
          tvm.passage.lDocId = spans.doc();
          tvm.passage.field = sQuery.getField();
          //score this window
          try {
            addPassage(tvm.passage, rankedPassages, termWeights, bigramWeights, adjWeight, secondAdjWeight, bigramWeight);
          } catch (CloneNotSupportedException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Internal error cloning Passage", e);
          }
          //clear out the entries for the next round
          tvm.passage.clear();
        }
      }
      NamedList qaResp = new NamedList();
      rb.rsp.add("qaResponse", qaResp);
      int rows = params.getInt(QA_ROWS, 5);

      SchemaField uniqField = rb.req.getSchema().getUniqueKeyField();
      if (rankedPassages.size() > 0) {
        int size = Math.min(rows, rankedPassages.size());
        Set<String> fields = new HashSet<String>();
        for (int i = size - 1; i >= 0; i--) {
          Passage passage = rankedPassages.pop();
          if (passage != null) {
            NamedList passNL = new NamedList();
            qaResp.add(("answer"), passNL);
            String idName;
            String idValue;
            if (uniqField != null) {
              idName = uniqField.getName();
              fields.add(idName);
              fields.add(passage.field);//prefetch this now, so that it is cached
              idValue = searcher.doc(passage.lDocId, fields).get(idName);
            } else {
              idName = "luceneDocId";
              idValue = String.valueOf(passage.lDocId);
            }
            passNL.add(idName, idValue);
            passNL.add("field", passage.field);
            //get the window
            String fldValue = searcher.doc(passage.lDocId, fields).get(passage.field);
            if (fldValue != null) {
              //get the window of words to display, we don't use the passage window, as that is based on the term vector
              int start = passage.terms.first().start;//use the offsets
              int end = passage.terms.last().end;
              if (start >= 0 && start < fldValue.length() &&
                      end >= 0 && end < fldValue.length()) {
                passNL.add("window", fldValue.substring(start, end + passage.terms.last().term.length()));
              } else {
                log.debug("Passage does not have correct offset information");
                passNL.add("window", fldValue);//we don't have offsets, or they are incorrect, return the whole field value
              }
            }
          } else {
            break;
          }
        }
      }
    }


  }


  protected float scoreTerms(SortedSet<WindowTerm> terms, Map<String, Float> termWeights, Set<String> covered) {
    float score = 0f;
    for (WindowTerm wTerm : terms) {
      Float tw = (Float) termWeights.get(wTerm.term);
      if (tw != null && !covered.contains(wTerm.term)) {
        score += tw.floatValue();
        covered.add(wTerm.term);
      }
    }

    return (score);
  }

  protected float scoreBigrams(SortedSet<WindowTerm> bigrams, Map<String, Float> bigramWeights, Set<String> covered) {
    float result = 0;
    for (WindowTerm bigram : bigrams) {
      Float tw = (Float) bigramWeights.get(bigram.term);
      if (tw != null && !covered.contains(bigram.term)) {
        result += tw.floatValue();
        covered.add(bigram.term);
      }
    }
    return result;
  }

  /**
   * A fairly straightforward and simple scoring approach based on http://trec.nist.gov/pubs/trec8/papers/att-trec8.pdf.
   * <br/>
   * Score the {@link com.tamingtext.qa.PassageRankingComponent.Passage} as the sum of:
   * <ul>
   * <li>The sum of the IDF values for the primary window terms ({@link com.tamingtext.qa.PassageRankingComponent.Passage#terms}</li>
   * <li>The sum of the weights of the terms of the adjacent window ({@link com.tamingtext.qa.PassageRankingComponent.Passage#prevTerms} and {@link com.tamingtext.qa.PassageRankingComponent.Passage#followTerms}) * adjWeight</li>
   * <li>The sum of the weights terms of the second adjacent window ({@link com.tamingtext.qa.PassageRankingComponent.Passage#secPrevTerms} and {@link com.tamingtext.qa.PassageRankingComponent.Passage#secFollowTerms}) * secondAdjWeight</li>
   * <li>The sum of the weights of any bigram matches for the primary window * biWeight</li>
   * </ul>
   * In laymen's terms, this is a decay function that gives higher scores to matching terms that are closer to the anchor
   * term  (where the query matched, in the middle of the window) than those that are further away.
   *
   * @param p               The {@link com.tamingtext.qa.PassageRankingComponent.Passage} to score
   * @param termWeights     The weights of the terms, key is the term, value is the inverse doc frequency (or other weight)
   * @param bigramWeights   The weights of the bigrams, key is the bigram, value is the weight
   * @param adjWeight       The weight to be applied to the adjacent window score
   * @param secondAdjWeight The weight to be applied to the secondary adjacent window score
   * @param biWeight        The weight to be applied to the bigram window score
   * @return The score of passage
   */
  //<start id="qa.scorePassage"/>
  protected float scorePassage(Passage p, Map<String, Float> termWeights,
                               Map<String, Float> bigramWeights,
                               float adjWeight, float secondAdjWeight,
                               float biWeight) {
    Set<String> covered = new HashSet<String>();
    float termScore = scoreTerms(p.terms, termWeights, covered);//<co id="prc.main"/>
    float adjScore = scoreTerms(p.prevTerms, termWeights, covered) +
            scoreTerms(p.followTerms, termWeights, covered);//<co id="prc.adj"/>
    float secondScore = scoreTerms(p.secPrevTerms, termWeights, covered)
            + scoreTerms(p.secFollowTerms, termWeights, covered);//<co id="prc.sec"/>
    //Give a bonus for bigram matches in the main window, could also
    float bigramScore = scoreBigrams(p.bigrams, bigramWeights, covered);//<co id="prc.bigrams"/>
    float score = termScore + (adjWeight * adjScore) +
            (secondAdjWeight * secondScore)
            + (biWeight * bigramScore);//<co id="prc.score"/>
    return (score);
  }
  /*
  <calloutlist>
      <callout arearefs="prc.main"><para>Score the terms in the main window</para></callout>
      <callout arearefs="prc.adj"><para>Score the terms in the window immediately to the left and right of the main window</para></callout>
      <callout arearefs="prc.sec"><para>Score the terms in the windows adjacent to the previous and following windows</para></callout>
      <callout arearefs="prc.bigrams"><para>Score any bigrams in the passage</para></callout>
      <callout arearefs="prc.score"><para>The final score for the passage is a combination of all the scores, each weighted separately.  A bonus is given for any bigram matches.</para></callout>
      
  </calloutlist>
  */
  //<end id="qa.scorePassage"/>


  /**
   * Potentially add the passage to the PriorityQueue.
   *
   * @param p               The passage to add
   * @param pq              The {@link org.apache.lucene.util.PriorityQueue} to add the passage to if it ranks high enough
   * @param termWeights     The weights of the terms
   * @param bigramWeights   The weights of the bigrams
   * @param adjWeight       The weight to be applied to the score of the adjacent window
   * @param secondAdjWeight The weight to be applied to the score of the second adjacent window
   * @param biWeight        The weight to be applied to the score of the bigrams
   * @throws CloneNotSupportedException if not cloneable
   */
  private void addPassage(Passage p, PassagePriorityQueue pq, Map<String, Float> termWeights,
                          Map<String, Float> bigramWeights,
                          float adjWeight, float secondAdjWeight, float biWeight) throws CloneNotSupportedException {
    p.score = scorePassage(p, termWeights, bigramWeights, adjWeight, secondAdjWeight, biWeight);
    Passage lowest = pq.top();
    if (lowest == null || pq.lessThan(p, lowest) == false || pq.size() < pq.capacity()) {
      //by doing this, we can re-use the Passage object
      Passage cloned = (Passage) p.clone();
      //TODO: Do we care about the overflow?
      pq.insertWithOverflow(cloned);
    }

  }

  protected void createWeights(String origQuery, SpanNearQuery parsedQuery,
                               Map<String, Float> termWeights,
                               Map<String, Float> bigramWeights, IndexReader reader) throws IOException {

    SpanQuery[] clauses = parsedQuery.getClauses();
    //we need to recurse through the clauses until we get to SpanTermQuery
    Term lastTerm = null;
    Float lastWeight = null;
    for (int i = 0; i < clauses.length; i++) {
      SpanQuery clause = clauses[i];
      if (clause instanceof SpanTermQuery) {
        Term term = ((SpanTermQuery) clause).getTerm();
        Float weight = calculateWeight(term, reader);
        termWeights.put(term.text(), weight);
        if (lastTerm != null) {//calculate the bi-grams
          //use the smaller of the two weights
          if (lastWeight.floatValue() < weight.floatValue()) {
            bigramWeights.put(lastTerm + "," + term.text(), new Float(lastWeight.floatValue() * 0.25));
          } else {
            bigramWeights.put(lastTerm + "," + term.text(), new Float(weight.floatValue() * 0.25));
          }
        }
        //last
        lastTerm = term;
        lastWeight = weight;
      } else {
        //TODO: handle the other types
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unhandled query type: " + clause.getClass().getName());
      }
    }


  }

  protected float calculateWeight(Term term, IndexReader reader) throws IOException {
    //if a term is not in the index, then it's weight is 0
    TermEnum termEnum = reader.terms(term);
    if (termEnum != null && termEnum.term() != null && termEnum.term().equals(term)) {
      return 1.0f / termEnum.docFreq();
    } else {
      log.warn("Couldn't find doc freq for term {}", term);
      return 0;
    }

  }

  class Passage implements Cloneable {
    int lDocId;
    String field;

    float score;
    SortedSet<WindowTerm> terms = new TreeSet<WindowTerm>();
    SortedSet<WindowTerm> prevTerms = new TreeSet<WindowTerm>();
    SortedSet<WindowTerm> followTerms = new TreeSet<WindowTerm>();
    SortedSet<WindowTerm> secPrevTerms = new TreeSet<WindowTerm>();
    SortedSet<WindowTerm> secFollowTerms = new TreeSet<WindowTerm>();
    SortedSet<WindowTerm> bigrams = new TreeSet<WindowTerm>();

    Passage() {
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
      Passage result = (Passage) super.clone();
      result.terms = new TreeSet<WindowTerm>();
      for (WindowTerm term : terms) {
        result.terms.add((WindowTerm) term.clone());
      }
      result.prevTerms = new TreeSet<WindowTerm>();
      for (WindowTerm term : prevTerms) {
        result.prevTerms.add((WindowTerm) term.clone());
      }
      result.followTerms = new TreeSet<WindowTerm>();
      for (WindowTerm term : followTerms) {
        result.followTerms.add((WindowTerm) term.clone());
      }
      result.secPrevTerms = new TreeSet<WindowTerm>();
      for (WindowTerm term : secPrevTerms) {
        result.secPrevTerms.add((WindowTerm) term.clone());
      }
      result.secFollowTerms = new TreeSet<WindowTerm>();
      for (WindowTerm term : secFollowTerms) {
        result.secFollowTerms.add((WindowTerm) term.clone());
      }
      result.bigrams = new TreeSet<WindowTerm>();
      for (WindowTerm term : bigrams) {
        result.bigrams.add((WindowTerm) term.clone());
      }

      return result;
    }


    public void clear() {
      terms.clear();
      prevTerms.clear();
      followTerms.clear();
      secPrevTerms.clear();
      secPrevTerms.clear();
      bigrams.clear();
    }


  }

  class PassagePriorityQueue extends PriorityQueue<Passage> {

    PassagePriorityQueue() {
      initialize(10);
    }

    PassagePriorityQueue(int maxSize) {
      initialize(maxSize);
    }

    public int capacity() {
      return getHeapArray().length;
    }

    @Override
    public boolean lessThan(Passage passageA, Passage passageB) {
      if (passageA.score == passageB.score)
        return passageA.lDocId > passageB.lDocId;
      else
        return passageA.score < passageB.score;
    }
  }


  //Not thread-safe, but should be lightweight to build

  /**
   * The PassageRankingTVM is a Lucene TermVectorMapper that builds a five different windows around a matching term.
   * This Window can then be used to rank the passages
   */
  class WindowBuildingTVM extends TermVectorMapper {
    //spanStart and spanEnd are the start and positions of where the match occurred in the document
    //from these values, we can calculate the windows
    int spanStart, spanEnd;
    Passage passage;
    private int primaryWS, adjWS, secWS;


    public WindowBuildingTVM(int primaryWindowSize, int adjacentWindowSize, int secondaryWindowSize) {
      this.primaryWS = primaryWindowSize;
      this.adjWS = adjacentWindowSize;
      this.secWS = secondaryWindowSize;
      passage = new Passage();//reuse the passage, since it will be cloned if it makes it onto the priority queue
    }

    public void map(String term, int frequency, TermVectorOffsetInfo[] offsets, int[] positions) {
      if (positions.length > 0 && term.startsWith(NameFilter.NE_PREFIX) == false && term.startsWith(NE_PREFIX_LOWER) == false) {//filter out the types, as we don't need them here
        //construct the windows, which means we need a bunch of bracketing variables to know what window we are in

        //start and end of the primary window
        int primStart = spanStart - primaryWS;
        int primEnd = spanEnd + primaryWS;
        // stores the start and end of the adjacent previous and following
        int adjLBStart = primStart - adjWS;
        int adjLBEnd = primStart - 1;//don't overlap
        int adjUBStart = primEnd + 1;//don't o
        int adjUBEnd = primEnd + adjWS;
        //stores the start and end of the secondary previous and the secondary following
        int secLBStart = adjLBStart - secWS;
        int secLBEnd = adjLBStart - 1; //don't overlap the adjacent window
        int secUBStart = adjUBEnd + 1;
        int secUBEnd = adjUBEnd + secWS;
        WindowTerm lastWT = null;
        for (int i = 0; i < positions.length; i++) {//unfortunately, we still have to loop over the positions
          //we'll make this inclusive of the boundaries, do an upfront check here so we can skip over anything that is outside of all windows
          if (positions[i] >= secLBStart && positions[i] <= secUBEnd) {
            //fill in the windows
            WindowTerm wt;
            //offsets aren't required, but they are nice to have
            if (offsets != null){
              wt = new WindowTerm(term, positions[i], offsets[i].getStartOffset(), offsets[i].getEndOffset());
            } else {
              wt = new WindowTerm(term, positions[i]);
            }
            if (positions[i] >= primStart && positions[i] <= primEnd) {//are we in the primary window
              passage.terms.add(wt);
              //we are only going to keep bigrams for the primary window.  You could do it for the other windows, too
              if (lastWT != null) {
                WindowTerm bigramWT = new WindowTerm(lastWT.term + "," + term, lastWT.position);//we don't care about offsets for bigrams
                passage.bigrams.add(bigramWT);
              }
              lastWT = wt;
            } else if (positions[i] >= secLBStart && positions[i] <= secLBEnd) {//are we in the secondary previous window?
              passage.secPrevTerms.add(wt);
            } else if (positions[i] >= secUBStart && positions[i] <= secUBEnd) {//are we in the secondary following window?
              passage.secFollowTerms.add(wt);
            } else if (positions[i] >= adjLBStart && positions[i] <= adjLBEnd) {//are we in the adjacent previous window?
              passage.prevTerms.add(wt);
            } else if (positions[i] >= adjUBStart && positions[i] <= adjUBEnd) {//are we in the adjacent following window?
              passage.followTerms.add(wt);
            }
          }
        }
      }
    }



    public void setExpectations(String field, int numTerms, boolean storeOffsets, boolean storePositions) {
      // do nothing for this example
      //See also the PositionBasedTermVectorMapper.
    }

  }

  class WindowTerm implements Cloneable, Comparable<WindowTerm> {
    String term;
    int position;
    int start, end = -1;

    WindowTerm(String term, int position, int startOffset, int endOffset) {
      this.term = term;
      this.position = position;
      this.start = startOffset;
      this.end = endOffset;
    }

    public WindowTerm(String s, int position) {
      this.term = s;
      this.position = position;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
      return super.clone();
    }

    @Override
    public int compareTo(WindowTerm other) {
      int result = position - other.position;
      if (result == 0) {
        result = term.compareTo(other.term);
      }
      return result;
    }

    @Override
    public String toString() {
      return "WindowEntry{" +
              "term='" + term + '\'' +
              ", position=" + position +
              '}';
    }
  }

  @Override
  public String getDescription() {
    return "Question Answering PassageRanking";
  }

  @Override
  public String getVersion() {
    return "$Revision:$";
  }

  @Override
  public String getSourceId() {
    return "$Id:$";
  }

  @Override
  public String getSource() {
    return "$URL:$";
  }
}
