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

package com.tamingtext.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.tamingtext.TamingTextTestJ4;
import junit.framework.TestCase;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.junit.*;

public class StringUtilTest extends TamingTextTestJ4 {



  @Test
  public void testWhitespace() throws Exception {
    String[] gold = {"The", "Carolina", "Hurricanes", "won", "the", "2006", "Stanley", "Cup."};
    String[] result = StringUtil.tokenizeWhitespace("The Carolina Hurricanes won the 2006 Stanley Cup.");
    assertTrue("result Size: " + result.length + " is not: " + gold.length, result.length == gold.length);
    for (int i = 0; i < result.length; i++) {
      assertTrue(result[i] + " is not equal to " + gold[i], result[i].equals(gold[i]) == true);

    }

  }
  @Test
  public void testLuceneStandardTokenizer() throws Exception {
    String[] gold = {"I", "can't", "beleive", "that", "the", "Carolina", "Hurricanes", "won", "the", "2005", "2006", "Stanley", "Cup",};
    StandardTokenizer tokenizer = new StandardTokenizer(Version.LUCENE_36, new StringReader("I can't beleive that the Carolina Hurricanes won the 2005-2006 Stanley Cup."));
    List<String> result = new ArrayList<String>();
    while (tokenizer.incrementToken()) {
      result.add(((CharTermAttribute) tokenizer.getAttribute(CharTermAttribute.class)).toString());
    }
    assertTrue("result Size: " + result.size() + " is not: " + gold.length, result.size() == gold.length);
    int i = 0;
    for (String chunk : result) {
      assertTrue(chunk + " is not equal to " + gold[i], chunk.equals(gold[i]) == true);
      i++;
    }
  }
  @Test
  public void testVikings() throws Exception {
    String[] gold = {"Last", "week", "the", "National", "Football", "League", "crowned", "a", "new", "Super", "Bowl", "Champion",
            "Minnesota", "Vikings", "fans", "will", "take", "little", "solace", "in", "the", "fact", "that", "they",
            "lost", "to", "the", "eventual", "champion", "in", "the", "playoffs"};
    StandardTokenizer tokenizer = new StandardTokenizer(Version.LUCENE_36, new StringReader("Last week the National Football League crowned a new Super Bowl Champion." +
            "  Minnesota Vikings fans will take little solace in the fact that they" +
            " lost to the eventual champion in the playoffs."));
    List<String> result = new ArrayList<String>();
    while (tokenizer.incrementToken()) {
      result.add(((CharTermAttribute) tokenizer.getAttribute(CharTermAttribute.class)).toString());
    }
    assertTrue("result Size: " + result.size() + " is not: " + gold.length, result.size() == gold.length);
    int i = 0;
    for (String chunk : result) {
      System.out.println(chunk);
      assertTrue(chunk + " is not equal to " + gold[i], chunk.equals(gold[i]) == true);
      i++;
    }
  }


}