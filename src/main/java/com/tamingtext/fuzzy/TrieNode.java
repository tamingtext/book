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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TrieNode {

  //<start id="trie-node1"/>
  private boolean isWord; //<co id="co.trie.word"/>
  private TrieNode[] children; 
  private String suffix; //<co id="co.trie.suffix"/>
    
  public TrieNode(boolean word, String suffix) {
    this.isWord = word;
    if (suffix == null) children = new TrieNode[26]; //<co id="co.trie.children"/>
    this.suffix = suffix;
  }
  /*
  <calloutlist>
  <callout arearefs="co.trie.word"><para>Does this prefix make a word.</para></callout>
  <callout arearefs="co.trie.suffix"><para>Rest of word if prefix is unique.</para></callout>      
  <callout arearefs="co.trie.children"><para>Initialize children for each letter.</para></callout>
  </calloutlist>
   */
  //<end id="trie-node1"/>
  
  public TrieNode(boolean word) { 
	this(word,null);
  }    
  
  public boolean isWord() {
    return isWord;
  }
  
//<start id="trie-addWord"/>
  public boolean addWord(String word) {
    return addWord(word.toLowerCase(),0);
  }
  
  private boolean addWord(String word, int index) {
    if (index == word.length()) { //<co id="co.trie.end-of-word"/>
      if (isWord) {
        return false; //<co id="co.trie.duplicate"/>
      }
      else {
        isWord = true; //<co id="co.trie.mark-word"/>
        return true;
      }
    }
    if (suffix != null) { //<co id="co.trie.has-suf"/>
      if (suffix.equals(word.substring(index))) { 
        return false; //<co id="co.trie.duplicate-suf"/>
      }
      String tmp = suffix;
      this.suffix = null;
      children = new TrieNode[26];
      addWord(tmp,0); //<co id="co.trie.split-suffix"/>
    }
    int ci = word.charAt(index)-(int)'a';
    TrieNode child = children[ci];
    if (child == null) {
      if (word.length() == index -1) {
        children[ci] = new TrieNode(true,null); //<co id="co.trie.create-word"/>
      }
      else {
        children[ci] = new TrieNode(false,word.substring(index+1)); //<co id="co.trie.create-suf"/>
      }
      return true;
    }
    return child.addWord(word, index+1); //<co id="co.trie.recurse"/>
  }
  /*
  <calloutlist>
  <callout arearefs="co.trie.end-of-word"><para>Check if end of the word.</para></callout>
  <callout arearefs="co.trie.duplicate"><para>Existing word; return false.</para></callout>
  <callout arearefs="co.trie.mark-word"><para>Mark prefix as a word.</para></callout>
  <callout arearefs="co.trie.has-suf"><para>Check if this node has a suffix.</para></callout>
  <callout arearefs="co.trie.duplicate-suf">existing word, return false.<para></para></callout>
  <callout arearefs="co.trie.split-suffix">Split up the suffix.<para></para></callout>
  <callout arearefs="co.trie.create-word"><para>Prefix creates a new word.</para></callout>
  <callout arearefs="co.trie.create-suf"><para>Prefix and suffix create a new word.</para></callout>              
  <callout arearefs="co.trie.recurse"><para>Recurse on next character.</para></callout>
  </calloutlist>                
  */
  //<end id="trie-addWord"/>
  
  //<start id="trie-getWords"/>
  public String[] getWords(String prefix, int numWords) {  //<co id="co.trie.prefix"/>
    List<String> words = new ArrayList<String>(numWords);
    TrieNode prefixRoot = this;
    for (int i=0;i<prefix.length();i++) {
      if (prefixRoot.suffix == null) {
        int ci = prefix.charAt(i)-(int)'a';
        prefixRoot = prefixRoot.children[ci];
        if (prefixRoot == null) {
          break;
        }
      }
      else { //<co id="co.trie.no-prefix"/>
        if (prefixRoot.suffix.startsWith(prefix.substring(i))) {
          words.add(prefix.substring(0,i)+prefixRoot.suffix);
        }
        prefixRoot = null;
        break;
      }
    }
    if (prefixRoot != null) {
      prefixRoot.collectWords(words,numWords,prefix); 
    }
    return words.toArray(new String[words.size()]);
  }
  
  private void collectWords(List<String> words, //<co id="co.trie.collect"/>
                            int numWords, String prefix) { 
    if (this.isWord()) {
      words.add(prefix);
      if (words.size() == numWords) return;
    }
    if (suffix != null) {
      words.add(prefix+suffix);
      return;
    }
    for (int ci=0;ci<children.length;ci++) {
      String nextPrefix = prefix+(char) (ci+(int)'a');
      if (children[ci] != null) {
        children[ci].collectWords(words, numWords, nextPrefix);
        if (words.size() == numWords) return;
      }
    }
  }
  /*
  <calloutlist>
  <callout arearefs="co.trie.prefix"><para>Traverse the tree until the prefix is consumed.</para></callout>
  <callout arearefs="co.trie.no-prefix"><para>Handle the case where the entire prefix has not been split into trie nodes.</para></callout>  
  <callout arearefs="co.trie.collect"><para>Collect all the words that are children of the prefix node.</para></callout>
  </calloutlist>    
  */
  //<end id="trie-getWords"/>
  
  public String toString() {
    StringBuffer cs = new StringBuffer(children.length);
    for (int ci=0;ci<children.length;ci++) {
      if (children[ci] != null) {
        cs.append((char) (ci+(int)'a'));
      }
    }
    return "word="+isWord+" suffix="+suffix+" cs="+cs;
  }
  
  public static void main(String[] args) throws IOException {
    TrieNode node = new TrieNode(false);
    int lc = 0;
    BufferedReader br = new BufferedReader(new FileReader(args[0]));
    for (String line = br.readLine();line !=null;line = br.readLine()) {
      node.addWord(line);
      lc++;
    }
    System.out.println("Loaded "+lc+" lines");
    BufferedReader br2 = new BufferedReader(new InputStreamReader(System.in));
    for (String line = br2.readLine();line !=null;line = br2.readLine()) {
      String[] words = node.getWords(line, 10);
      System.out.println(java.util.Arrays.asList(words));
    }
  }
}
