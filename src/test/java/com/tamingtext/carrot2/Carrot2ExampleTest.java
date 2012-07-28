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

package com.tamingtext.carrot2;

import com.tamingtext.TamingTextTestJ4;
import junit.framework.TestCase;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;

import org.carrot2.core.attribute.AttributeNames;
import org.junit.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Show a few different ways to run the Carrot2 clustering tools.
 */
public class Carrot2ExampleTest extends TamingTextTestJ4 {

  private static final String[] titles = {
          "Red Fox jumps over Lazy Brown Dogs",
          "Mary Loses Little Lamb.  Wolf At Large.",
          "Lazy Brown Dogs Promise Revenge on Red Fox",
          "March Comes in like a Lamb"
  };
  private static final String[] snippets = {
          "The sly red fox ran down the canyon, through the woods and over the field, jumping over Farmer Ted's lazy brown dogs as if they were two fallen trees.",
          "In a disastrous turn of events, Mary, the shepherd, lost one of her lambs last night between 10 and 11 PM.  While it can't be proved just yet, the strange disappearance of Mr. Wolf suggests his involvement.",
          "After being thoroughly embarrassed by the red fox yesterday, Farmer Ted's brown dogs came out with a press release vowing vengeance on the fox.",
          "After a cold and blustery February, citizens of Minneapolis were relieved that March entered like a lamb instead of a lion."
  };
  protected List<Document> documents;


  @Test
  public void testSimple() throws Exception {
    //<start id="crt2.simple"/>
    //... setup some documents elsewhere
    final Controller controller =
            ControllerFactory.createSimple();//<co id="crt2.controller.creation"/>
    documents = new ArrayList<Document>();
    for (int i = 0; i < titles.length; i++) {
      Document doc = new Document(titles[i], snippets[i],
              "file://foo_" + i + ".txt");
      documents.add(doc);
    }
    final ProcessingResult result = controller.process(documents,
            "red fox",
            LingoClusteringAlgorithm.class);//<co id="crt2.process"/>
    displayResults(result);//<co id="crt2.print"/>

    /*
      <calloutlist>
        <callout arearefs="crt2.controller.creation"><para>Create the <classname>IController</classname></para></callout>
        <callout arearefs="crt2.process"><para>Cluster the documents</para></callout>
        <callout arearefs="crt2.print"><para>Print out the clusters</para></callout>
      </calloutlist>
    */
    //<end id="crt2.simple"/>
  }

  private void displayResults(ProcessingResult result) {
    Collection<Cluster> clusters = result.getClusters();
    for (Cluster cluster : clusters) {
      System.out.println("Cluster: " + cluster.getLabel());
      for (Document document : cluster.getDocuments()) {
        System.out.println("\t" + document.getTitle());
      }
    }

  }

}
