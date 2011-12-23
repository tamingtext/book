package com.tamingtext.texttamer.solr;

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
 

import java.io.IOException;
import java.io.Reader;

import opennlp.tools.sentdetect.SentenceDetector;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/** Tokenize input using the OpenNLP SentenceDetector *
 */
public final class SentenceTokenizer extends Tokenizer {
  
  private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

  private SentenceDetector detector;
  private String[] sentences = null;
  private int tokenOffset = 0;
  private int charOffset  = 0;
  
  public SentenceTokenizer(Reader in, SentenceDetector detector) {
    super(in);
    this.detector = detector;
  }
  
  public void reset(Reader in) throws IOException {
    super.reset(in);
    sentences = null;
  }
  
  public void fillSentences() throws IOException {
    char[] c = new char[256];
    int sz = 0;
    StringBuilder b = new StringBuilder();
    
    while ((sz = input.read(c)) >= 0) {
      b.append(c, 0, sz);
    }
    
    sentences = detector.sentDetect(b.toString());
    tokenOffset = 0;
    charOffset  = 0;
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (sentences == null) {
      fillSentences();
    }
    
    if (tokenOffset >= sentences.length) {
      return false;
    }
    
    String sentence = sentences[tokenOffset];
    clearAttributes();
    termAtt.setEmpty().append(sentence);
    posIncrAtt.setPositionIncrement(1);
    offsetAtt.setOffset(charOffset, charOffset + sentence.length());
    charOffset += sentence.length();
    tokenOffset++;
    
    return true;
  }
}
