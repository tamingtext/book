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

package com.tamingtext.texttamer.solr;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

public final class NameFilter extends TokenFilter {

  public static final String NE_PREFIX = "NE_";

  private final Tokenizer tokenizer;
  private final String[] tokenTypeNames;

  private final NameFinderME[] finders;
  private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);
  private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  private String text;
  private int    baseOffset;
  
  private Span[] spans;
  private String[] tokens;
  private Span[][] foundNames;

  private boolean[][] tokenTypes;

  private int spanOffset     = 0;
  private final Queue<AttributeSource.State> tokenQueue =
    new LinkedList<AttributeSource.State>();

  public NameFilter(TokenStream in,String[] modelNames, NameFinderME[] finders) {
    super(in);
    this.tokenizer = SimpleTokenizer.INSTANCE;
    this.finders = finders;
    this.tokenTypeNames = new String[modelNames.length];
    for (int i=0; i < modelNames.length; i++) {
      tokenTypeNames[i] = NE_PREFIX + modelNames[i];
    }
  }

  /** consume tokens from the upstream tokenizer and buffer them in a 
   *  StringBuilder whose contents will get passed to opennlp.
   * @throws IOException
   */
  protected boolean fillSpans() throws IOException {
    if (!input.incrementToken()) return false;
    
    // process the next sentence from the upstream tokenizer
    text = input.getAttribute(CharTermAttribute.class).toString();
    baseOffset = input.getAttribute(OffsetAttribute.class).startOffset();
    
    spans = tokenizer.tokenizePos(text);
    tokens = Span.spansToStrings(spans, text);
    foundNames = new Span[finders.length][];
    for (int i = 0; i < finders.length; i++) {
      foundNames[i] = finders[i].find(tokens);
    }

    //TODO: make this a bitset that is tokens.length * finders.length 
    // in size.
    tokenTypes = new boolean[tokens.length][finders.length];
    
    for (int i = 0; i < finders.length; i++) {
      Span[] spans = foundNames[i];
      for (int j = 0; j < spans.length; j++) {
        int start = spans[j].getStart();
        int end   = spans[j].getEnd();
        for (int k = start; k < end; k++) {
          tokenTypes[k][i] = true;
        }
      }
    }
    
    spanOffset = 0;

    return true;
  }

  public boolean incrementToken() throws IOException {
    // if there's nothing in the queue.
    if (tokenQueue.peek() == null) {
      // no spans or spans consumed
      if (spans == null || spanOffset >= spans.length) {
        // no more data to fill spans
        if (!fillSpans()) return false;
      }
      
      if (spanOffset >= spans.length) {
        return false;
      }
      
      // copy the token and any types.
      clearAttributes();
      keywordAtt.setKeyword(false);
      posIncrAtt.setPositionIncrement(1);
      offsetAtt.setOffset(
          baseOffset + spans[spanOffset].getStart(), 
          baseOffset + spans[spanOffset].getEnd()
      );
      termAtt.setEmpty().append(tokens[spanOffset]);
      
      // determine of the current token is of a named entity type, if so
      // push the current state into the queue and add a token reflecting
      // any matching entity types.
      boolean[] types = tokenTypes[spanOffset];
      for (int i = 0; i < finders.length; i++) {
        if (types[i]) {
          keywordAtt.setKeyword(true);
          posIncrAtt.setPositionIncrement(0);
          tokenQueue.add(captureState());
          
          posIncrAtt.setPositionIncrement(1);
          termAtt.setEmpty().append(tokenTypeNames[i]);
        }
      }
      
      spanOffset++;
      return true;
    }
    
    State state = tokenQueue.poll();
    restoreState(state);

    return true;
  }
  
  @Override
  public void close() throws IOException {
    super.close();
    resetState();
  }

  @Override
  public void end() throws IOException {
    super.end();
    resetState();
  }

  private void resetState() {
    this.spanOffset = 0;
    this.spans = null;
  }
  
  protected void dumpState() {
    System.err.println(text);
    System.err.println("---");
    
    for (int i=0; i < spans.length; i++) {
      System.err.println(i + ";" + spans[i].getStart() + ":" + spans[i].getEnd() + " '" + tokens[i] + "'");
    }
    
    System.err.println("--");
    
    for (int i=0; i < foundNames.length; i++) {
      System.err.println(tokenTypeNames[i]);
      for (int j=0; j < foundNames[i].length; j++) {
        int start = foundNames[i][j].getStart();
        int end   = foundNames[i][j].getEnd();
        System.err.println("\t" + start + ":" + end);
        for (int k = start; k < end; k++) {
          System.err.println("\t\t" + k + ":'" + tokens[k] + "'");
        }
      }
      System.err.println("--");
    }
    
    System.err.println("-------------------------------------");
  }
  
}
