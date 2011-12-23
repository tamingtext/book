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
