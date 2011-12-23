package com.tamingtext.fuzzy;

import org.apache.lucene.document.Document;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.QueryResponseWriter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.response.SolrQueryResponse;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

//<start id="type-ahead-response-writer"/>
public class TypeAheadResponseWriter implements QueryResponseWriter {

  private Set<String> fields;

  @Override
  public String getContentType(SolrQueryRequest req, SolrQueryResponse solrQueryResponse) {
    return "text/html;charset=UTF-8";
  }


  public void init(NamedList n) {
    fields = new HashSet<String>();
    fields.add("word");  //<co id="co.fuzzy.type-ahead.field"/>
  }

  @Override
  public void write(Writer w, SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
    SolrIndexSearcher searcher = req.getSearcher();
    NamedList nl = rsp.getValues();
    int sz = nl.size();
    for (int li = 0; li < sz; li++) {
      Object val = nl.getVal(li);
      if (val instanceof DocList) { //<co id="co.fuzzy.type-ahead.doclist"/>
        DocList dl = (DocList) val;
        DocIterator iterator = dl.iterator();
        w.append("<ul>\n");
        while (iterator.hasNext()) {
          int id = iterator.nextDoc(); 
          Document doc = searcher.doc(id, fields); //<co id="co.fuzzy.type-ahead.search"/>
          String name = doc.get("word");
          w.append("<li>" + name + "</li>\n");
        }
        w.append("</ul>\n");
      }
    }
  }
}
/*
<calloutlist>
<callout arearefs="co.fuzzy.type-ahead.field"><para>Specify field displayed by response writer.</para></callout>
<callout arearefs="co.fuzzy.type-ahead.doclist"><para>Find document list.</para></callout>    
<callout arearefs="co.fuzzy.type-ahead.search"><para>Retrieve document with the specified field.</para></callout>
</calloutlist>
 */
//<end id="type-ahead-response-writer"/>
