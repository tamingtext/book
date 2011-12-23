package com.tamingtext.qa;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.tamingtext.TTTestCaseJ4;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 *
 **/
public class PassageRankingComponentTest  extends TTTestCaseJ4 {
  @BeforeClass
  public static void beforeClass() throws Exception {
    initCore("solrconfig-qa-prc.xml",
        "schema-qa.xml");


  }

  @Test
  public void testComponent() throws Exception{
    assertU("Add a doc to be ranked",
        adoc("id",  "1234",
            "details", "Michael Jordan is the greatest basketball player of all time"));

    assertU("Add a doc to be ranked",
        adoc("id",  "1235",
            "details", "Wayne Gretzky is the greatest hockey player of all time"));

    assertU(commit());
    //sanity check
    assertQ("Couldn't find query",
        req("q", "*:*", "defType", "lucene", "qa", "false"), "//result[@numFound=2]", "//str[@name='id'][.='1235']");
    assertQ("Couldn't find query",
        req("q", "details:hockey", "defType", "lucene", "qa", "false"), "//result[@numFound=1]", "//str[@name='id'][.='1235']");
    assertQ("Couldn't find query",
        req("Who is the greatest hockey player of all time?"), "//result[@numFound=1]", "//str[@name='id'][.='1235']");


  }

}
