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


import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

/**
 *
 *
 **/
public class MockQuestionQParserPlugin extends QParserPlugin{

  @Override
  public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
    return new MockQParser(qstr, localParams, params, req);
  }

  @Override
  public void init(NamedList args) {

  }

  class MockQParser extends QParser{


    /**
     * Constructor for the QParser
     *
     * @param qstr        The part of the query string specific to this parser
     * @param localParams The set of parameters that are specific to this QParser.  See http://wiki.apache.org/solr/LocalParams
     * @param params      The rest of the {@link org.apache.solr.common.params.SolrParams}
     * @param req         The original {@link org.apache.solr.request.SolrQueryRequest}.
     */
    public MockQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
      super(qstr, localParams, params, req);
    }

    @Override
    public Query parse() throws ParseException {
      SpanQuery[] clauses = null;
      if (qstr.indexOf("hockey") != -1){
        clauses = new SpanQuery[2];
        clauses[0] = new SpanTermQuery(new Term("details", "hockei"));
        clauses[1] = new SpanTermQuery(new Term("details", "player"));
      } else if (qstr.indexOf("basketball") != -1){
        clauses = new SpanQuery[2];
        clauses[0] = new SpanTermQuery(new Term("details", "basketbal"));
        clauses[1] = new SpanTermQuery(new Term("details", "player"));
      }
      return new SpanNearQuery(clauses, 1, true);
    }
  }
}
