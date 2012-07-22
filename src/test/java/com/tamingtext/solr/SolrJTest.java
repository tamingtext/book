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

package com.tamingtext.solr;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.BaseDistributedSearchTestCase;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettySolrRunner;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolrJTest extends BaseDistributedSearchTestCase {
  private transient static Log log = LogFactory.getLog(SolrJTest.class);

  static int port;
  /*
  @BeforeClass
  
  public static void beforeTest() throws Exception {
    File solrHome = new File("target/tamingText-src/solr");
    String solrData = "solrjtest-data";
    
    
    JettySolrRunner runner = createJetty(solrHome, solrData);
    port = runner.getLocalPort();
    
  }
   
   */

  /*
  @AfterClass
  public static void afterSolrJettyTest() throws Exception {
    SolrJettyTestBase.afterSolrJettyTestBase();
  }
  */
  
  JettySolrRunner runner;
  SolrServer      solr;
  
  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    File home = new File(getSolrHome());
    runner = createJetty(home, "target/solrjtest-data");
    port = runner.getLocalPort();
    
    solr = new CommonsHttpSolrServer(new URL("http://localhost:" + port + "/solr"));
    //clean out all the docs
    solr.deleteByQuery("*:*");
    solr.commit();

    System.out.println("NOTE: The Solr Server must be running on port 8983!!!!!!!!");
  }

  @After
  @Override
  public void tearDown() throws Exception {
    runner.stop();
    super.tearDown();
  }
  
  @Override
  public String getSolrHome() {
    //need to handle original source and packaging source
    File tmp = new File("target/tamingText-src/solr");
    if (tmp.exists()){
      return tmp.getAbsolutePath();
    }
    return "solr";
  }


  public void doTest() throws IOException, SolrServerException {
    log.info("--------------------test()---------------------");
    String description = "In a stunning defeat for rabbits " +
            "everywhere, Fred Q. Tortoise capitalized on John " +
            "Hare's brazen laziness to win the annual Tortoise-Hare 5K Road Race.  " +
            "The lazy Mr. Hare, in hopes of saving face, immediately " +
            "acccused Mr. Tortoise of doping, in what has become an " +
            "all too familiar scene in sporting events today.  " +
            "Fans everywhere were appalled by the revelations, with " +
            "allegiances falling roughly along species lines.";
    //<start id="solrj"/>
    SolrServer solr = new CommonsHttpSolrServer(new URL("http://localhost:" + port + "/solr"));//<co id="co.solrj.server"/>
    SolrInputDocument doc = new SolrInputDocument();
    doc.addField("id", "http://tortoisehare5k.tamingtext.com");//<co id="co.solrj.unique"/>
    doc.addField("mimeType", "text/plain");
    doc.addField("title", "Tortoise beats Hare!  Hare wants rematch.", 5);//<co id="co.solrj.title"/>
    Date now = new Date();
    doc.addField("date", DateUtil.getThreadLocalDateFormat().format(now));//<co id="co.solrj.date"/>
    doc.addField("description", description);
    doc.addField("categories_t", "Fairy Tale, Sports");//<co id="co.solrj.dynamic.field"/>
    solr.add(doc);//<co id="co.solrj.add"/>
    solr.commit();//<co id="co.solrj.commit"/>
    /*
    <calloutlist>
    <callout arearefs="co.solrj.server"><para>Create a HTTP-based Solr Server connection.</para></callout>
      <callout arearefs="co.solrj.unique"><para>The schema used for this instance of Solr requires a unique field named "id"</para></callout>
      <callout arearefs="co.solrj.title"><para>Add a Title field to our document and boost it to be 5 times as important as the other fields</para></callout>
      <callout arearefs="co.solrj.date"><para>Dates must be formatted in a specific way for Solr.</para></callout>
      <callout arearefs="co.solrj.dynamic.field"><para>A dynamic field allows for the addition of unknown fields to Solr.  The "_t" tells Solr this should be treated as a text field.</para></callout>
      <callout arearefs="co.solrj.add"><para>Send the newly created document to Solr.  Solr takes care of creating a correctly formatted XML message and sending it to Solr using Apache Jakarta Commons HTTPClient.</para></callout>
      <callout arearefs="co.solrj.commit"><para>After you have added all your documents and wish to make them available for searching, send a commit message to Solr</para></callout>
    </calloutlist>
    */
    //<end id="solrj"/>
    //Add some more docs
    doc = new SolrInputDocument();//<co id="co.solrj.doc"/>
    doc.addField("id", "http://redfox.tamingtext.com");//<co id="co.solrj.unique"/>
    doc.addField("mimeType", "text/plain");//<co id="co.solrj.mime"/>
    doc.addField("title", "Red Fox mocks Lazy Brown Dogs!", 5);//<co id="co.solrj.title"/>
    now = new Date();
    doc.addField("date", DateUtil.getThreadLocalDateFormat().format(now));
    doc.addField("description", "Once again, the Red Fox outshined the" +
            " lazy Brown Dogs to win the vaunted Tally Ho Cup, in what" +
            " has become an annual ritual.  The lazy brown " +
            "dogs just don't seem to have the drive that Red Fox does." +
            "  The lazy Brown Dogs vow to avenge their latest defeat, " +
            "but the group's supporters claim to have heard it all before.");
    doc.addField("categories_t", "Fairy Tale, Sports");
    solr.add(doc);//<co id="co.solrj.add"/>
    doc = new SolrInputDocument();//<co id="co.solrj.doc"/>
    doc.addField("id", "http://maryLambs.tamingtext.com");//<co id="co.solrj.unique"/>
    doc.addField("mimeType", "text/plain");//<co id="co.solrj.mime"/>

    now = new Date(10000);
    doc.addField("date", DateUtil.getThreadLocalDateFormat().format(now));
    doc.addField("title", "Mary's Little Lamb Stolen!.", 5);//<co id="co.solrj.title"/>
    doc.addField("description", "In an affront to all that is good and" +
            " decent in this world, criminals made off with Mary's " +
            "little Lamb in a late night raid on Mary's farm." +
            "  Early suspects include a Ms. Bo Peep who weeks earlier " +
            "reported losing several sheep.  Police suspect Ms. Peep " +
            "was going to use the lamb to bring her flock's numbers back up." +
            "  The spokesman for Ms. Peep declined comment at this " +
            "time.  Mary, however, was outraged and promises revenge " +
            "on the insensitive clod who stole her precious animals.");
    doc.addField("categories_t", "Fairy Tale, crime, sheep");
    solr.add(doc);
    //add in some random other docs
    Collection<SolrInputDocument> docs = new HashSet<SolrInputDocument>();
    for (int i = 0; i < 100; i++) {
      doc = new SolrInputDocument();
      doc.addField("id", "http://www.tamingtext.com/" + i + ".html");
      doc.addField("mimeType", "text/html");
      doc.addField("title", "This is title " + i);
      now = new Date(150 * i + i + 20);//create some dates
      doc.addField("date", DateUtil.getThreadLocalDateFormat().format(now));
      doc.addField("description", "This is description number: " + i
              + " of something that is very important.");
      docs.add(doc);
    }

    solr.add(docs);
    solr.commit();
    solr.optimize();
    //<start id="solrj-search-1"/>
    SolrQuery queryParams = new SolrQuery();//<co id="solrj-search.co.query"/>
    queryParams.setFields("description", "title");//<co id="solrj-search.co.fields"/>
    queryParams.setQuery("description:win OR description:all");//<co id="solrj-search.co.terms"/>
    queryParams.setRows(10);
    QueryResponse response = solr.query(queryParams);//<co id="solrj-search.co.process"/>
    assertTrue("response is null and it shouldn't be", response != null);
    SolrDocumentList documentList = response.getResults();
    System.out.println("Docs: " + documentList);
    assertTrue("response.getResults() Size: " + documentList.size() +
            " is not: " + 3, documentList.size() == 3);
    /*
<calloutlist>
    <callout arearefs="solrj-search.co.query"><para>A <classname>SolrQuery</classname> is a easy-to-use container for creating the most common types of queries.</para></callout>
    <callout arearefs="solrj-search.co.fields"><para>Set the names of the Fields to be returned in the documents.</para></callout>
    <callout arearefs="solrj-search.co.terms"><para>Set the terms to search.  The "OR" is a boolean operator which allows one or the other or both to be present in the query.</para></callout>
    <callout arearefs="solrj-search.co.process"><para>Submit the query to Solr and get back a <classname>QueryResponse</classname> which contains the results of the search.</para></callout>

</calloutlist>
*/
    //<end id="solrj-search-1"/>

    //<start id="solrj-search-dismax"/>
    queryParams.setQuery("lazy");
    queryParams.setParam("defType", "dismax");//<co id="solrj-search.co.dismax"/>
    queryParams.set("qf", "title^3 description^10");//<co id="sorlj-search.co.qf"/>
    System.out.println("Query: " + queryParams);
    response = solr.query(queryParams);
    assertTrue("response is null and it shouldn't be", response != null);
    documentList = response.getResults();
    assertTrue("documentList Size: " + documentList.size() +
            " is not: " + 2, documentList.size() == 2);
/*
<calloutlist>
    <callout arearefs="solrj-search.co.dismax"><para>Tell Solr to use the <classname>DisMax</classname> Query Parser (named dismax in solrconfig.xml). </para></callout>
    <callout arearefs="sorlj-search.co.qf"><para>The DisMax parser will search across the fields given by the "qf" parameter and boosts the terms accordingly.</para></callout>

</calloutlist>
*/
    //<end id="solrj-search-dismax"/>

    //<start id="solrj-search-facets"/>
    queryParams = new SolrQuery();
    queryParams.setQuery("description:number");
    queryParams.setRows(10);
    queryParams.setFacet(true);//<co id="solrj-search-facets-facetOn"/>
    queryParams.set("facet.field", "date");//<co id="solrj-search-facets-date"/>
    response = solr.query(queryParams);
    assertTrue("response is null and it shouldn't be", response != null);
    System.out.println("Query: " + queryParams);
    documentList = response.getResults();
    assertTrue("documentList Size: " + documentList.size() +
            " is not: " + 10, documentList.size() == 10);
    System.out.println("Facet Response: " + response.getResponse());//<co id="solrj-search-facets-print"/>
    /*
<calloutlist>
  <callout arearefs="solrj-search-facets-facetOn"><para>Turn on faceting for this query</para></callout>
  <callout arearefs="solrj-search-facets-date"><para>Specify the Field to facet on</para></callout>
  <callout arearefs="solrj-search-facets-print"><para>Print out the facet information</para></callout>

</calloutlist>
*/
    //<end id="solrj-search-facets"/>
    //<start id="solrj-search-more-like-this"/>
    queryParams = new SolrQuery();
    queryParams.setQueryType("/mlt");//<co id="solrj-search.co.mlt"/>
    queryParams.setQuery("description:number");
    queryParams.set("mlt.match.offset", "0");//<co id="solrj-search.co.mlt.off"/>
    queryParams.setRows(1);
    queryParams.set("mlt.fl", "description, title");//<co id="solrj-search.co.mlt.fl"/>
    response = solr.query(queryParams);
    assertTrue("response is null and it shouldn't be", response != null);
    SolrDocumentList results = (SolrDocumentList) response.getResponse().get("match");
    assertTrue("results Size: " + results.size() + " is not: " + 1,
            results.size() == 1);
    /*
<calloutlist>
    <callout arearefs="solrj-search.co.mlt"><para>Create a "MoreLikeThis" search to find similar documents to the specified document.</para></callout>
    <callout arearefs="solrj-search.co.mlt.fl"><para>Specify the field to use to generate the "MoreLikeThis" query.</para></callout>
    <callout arearefs="solrj-search.co.mlt.off"><para>Specify which document in the original results to use as the "similar" document. </para></callout>
</calloutlist>
*/
    //<end id="solrj-search-more-like-this"/>

    log.info("--------------------end test()---------------------");
  }
}