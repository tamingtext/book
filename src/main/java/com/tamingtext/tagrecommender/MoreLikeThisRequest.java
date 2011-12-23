package com.tamingtext.tagrecommender;

import java.util.Collection;

import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;

/** Variant of QueryRequest that POSTs data to the Solr MoreLikeThis
 *  handler.
 */
public class MoreLikeThisRequest extends QueryRequest {
  
  public static final String TEXT_PLAIN = "text/plain; charset=utf-8";
  
  private static final long serialVersionUID = -7448773819651705952L;

  private String content;
  
  public MoreLikeThisRequest() {
    super();
  }
  
  public MoreLikeThisRequest(SolrParams q, String content) {
    super(q, METHOD.POST);
    this.setContent(content);
  }

  @Override
  public String getPath() {
    return "/mlt";
  }

  @Override
  public Collection<ContentStream> getContentStreams() {
    return ClientUtils.toContentStreams(getContent(), TEXT_PLAIN);
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
