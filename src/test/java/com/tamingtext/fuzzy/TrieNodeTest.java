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
import org.junit.*;

public class TrieNodeTest extends TamingTextTestJ4 {
  @Test
  public void testOne() {
    TrieNode node = new TrieNode(false);
    node.addWord("tom");
    String[] words = node.getWords("tom", 2);
    assertTrue(words.length == 1);
    assertTrue(words[0].equals("tom"));
  }
  @Test
  public void testOneLong() {
    TrieNode node = new TrieNode(false);
    node.addWord("tomorrow");
    String[] words = node.getWords("tom", 2);
    assertTrue(words.length == 1);
    assertTrue(words[0].equals("tomorrow"));
  }
  @Test
  public void testOneShort() {
    TrieNode node = new TrieNode(false);
    node.addWord("tomorrow");
    String[] words = node.getWords("t", 2);
    assertTrue(words.length == 1);
    assertTrue(words[0].equals("tomorrow"));
  }

  @Test
  public void testShortLong() {
    TrieNode node = new TrieNode(false);
    node.addWord("tom");
    node.addWord("tomorrow");
    String[] words = node.getWords("tom", 2);
    assertTrue(words.length == 2);
    assertTrue(words[0].equals("tom"));
    assertTrue(words[1].equals("tomorrow"));
  }
  @Test
  public void testLongShort() {
    TrieNode node = new TrieNode(false);
    node.addWord("tomorrow");
    node.addWord("tom");
    String[] words = node.getWords("tom", 2);
    assertTrue(words.length == 2);
    assertTrue(words[0].equals("tom"));
    assertTrue(words[1].equals("tomorrow"));
  }
  @Test
  public void testSame() {
    TrieNode node = new TrieNode(false);
    node.addWord("tom");
    node.addWord("tom");
    String[] words = node.getWords("tom", 2);
    assertTrue(words.length == 1);
    assertTrue(words[0].equals("tom"));
  }
  @Test
  public void testSplit() {
    TrieNode node = new TrieNode(false);
    node.addWord("tomorrow");
    node.addWord("tomahawk");
    String[] words = node.getWords("tom", 2);
    assertTrue(words.length == 2);
    assertTrue(words[0].equals("tomahawk"));
    assertTrue(words[1].equals("tomorrow"));
  }
  @Test
  public void testMoreSame() {
    TrieNode node = new TrieNode(false);
    node.addWord("tom");
    node.addWord("tomorrow");
    node.addWord("tomorrow");
    String[] words = node.getWords("tom", 2);
    assertTrue(words.length == 2);
    assertTrue(words[0].equals("tom"));
    assertTrue(words[1].equals("tomorrow"));
  }
  @Test
  public void testGetMore() {
    TrieNode node = new TrieNode(false);
    node.addWord("tomorrow");
    node.addWord("tom");
    node.addWord("bat");
    String[] words = node.getWords("tom", 5);
    assertTrue(words.length == 2);
    assertTrue(words[0].equals("tom"));
    assertTrue(words[1].equals("tomorrow"));
  }
  @Test
  public void testGetLess() {
    TrieNode node = new TrieNode(false);
    node.addWord("bat");
    node.addWord("tomorrow");
    node.addWord("tom");
    node.addWord("tommy");
    String[] words = node.getWords("tom", 1);
    assertTrue(words.length == 1);
    assertTrue(words[0].equals("tom"));
    words = node.getWords("tom", 3);
    assertTrue(words.length == 3);
    assertTrue(words[0].equals("tom"));
    assertTrue(words[1].equals("tommy"));
    assertTrue(words[2].equals("tomorrow"));
  }
}
