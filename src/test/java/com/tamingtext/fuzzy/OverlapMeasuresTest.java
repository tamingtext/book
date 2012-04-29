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

import com.tamingtext.TamingTextTestJ4;
import junit.framework.TestCase;
import org.apache.lucene.search.TopDocs;
import org.junit.*;

public class OverlapMeasuresTest extends TamingTextTestJ4 {


  @Test
  public void testJaccard() {
    OverlapMeasures om = new OverlapMeasures();
    assertEquals(om.jaccard("zoo".toCharArray(), "zoo".toCharArray()),1f);
    assertEquals(om.jaccard("zoo".toCharArray(), "zoom".toCharArray()),(float) 2/3);
    assertEquals(om.jaccard("zoot".toCharArray(), "zoomo".toCharArray()),(float) 2/4);
    assertEquals(om.jaccard("zooto".toCharArray(), "zoom".toCharArray()),(float) 2/4);
    assertEquals(om.jaccard("zooto".toCharArray(), "zoomo".toCharArray()),(float) 2/4);
  }

  //just a simple test of the cosine overlap discussion in the Fuzzy chapter.
  @Test
  public void testCosine() throws Exception {
    OverlapMeasures om = new OverlapMeasures();
    TopDocs docs = om.cosine("chars:mob", 10, "bob", "fob", "job", "cob", "bobo");
    if (docs != null) {
      System.out.println("Total hits: " + docs.totalHits);
      for (int i = 0; i < docs.scoreDocs.length; i++){
        System.out.println("Id: " + docs.scoreDocs[i].doc + " score: " + docs.scoreDocs[i].score);
      }
    }
  }
}
