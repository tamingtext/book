package com.tamingtext.qa;

import java.io.File;
import java.io.IOException;

import opennlp.tools.lang.english.ParserTagger;
import opennlp.tools.lang.english.TreebankChunker;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.util.Span;

/**
 * Finds flat chunks instead of a tree structure using a simpler model.
 *
 * This class is not thread-safe, but should be lightweight to construct.
 */
public class ChunkParser implements Parser {

  private TreebankChunker chunker;
  private ParserTagger tagger;

  public ChunkParser(TreebankChunker chunker, ParserTagger tagger) {
    this.chunker = chunker;
    this.tagger = tagger;
  }
  
  @Override
  public Parse parse(Parse tokens) {
    //<start id="cp.pos"/>
    Parse[] children = tokens.getChildren();//<co id="cp.child"/>
    String[] words = new String[children.length];
    double[] probs = new double[words.length];
    for (int i = 0, il = children.length; i < il; i++) {
      words[i] = children[i].toString();//<co id="cp.words"/>
    }
    String[] tags = tagger.tag(words);//<co id="cp.tag"/>
    tagger.probs(probs);//<co id="cp.probs"/>
    for (int j = 0; j < words.length; j++) {
      Parse word = children[j];
      double prob = probs[j];
      tokens.insert(new Parse(word.getText(), word.getSpan(), tags[j], prob, j));//<co id="cp.augment"/>
      tokens.addProb(Math.log(prob));
    }
    /*
    <calloutlist>
        <callout arearefs="cp.child"><para>The <methodname>parse</methodname> is a callback method from an internal OpenNLP API that tokenizes the original text.</para></callout>
        <callout arearefs="cp.words"><para>Get just the words for use with the tagger</para></callout>
        <callout arearefs="cp.tag"><para>Part of speech tag the words</para></callout>
        <callout arearefs="cp.probs"><para></para></callout>
        <callout arearefs="cp.augment"><para>Augment the initial parse with the part of speech information</para></callout>
    </calloutlist>
    */
    //<end id="cp.pos"/>
    String[] chunks = chunker.chunk(words, tags);
    chunker.probs(probs);
    int chunkStart = -1;
    String chunkType = null;
    double logProb=0;
    for (int ci=0,cn=chunks.length;ci<cn;ci++) {
      if (ci > 0 && !chunks[ci].startsWith("I-") && !chunks[ci-1].equals("O")) {
        Span span = new Span(children[chunkStart].getSpan().getStart(),children[ci-1].getSpan().getEnd());
        tokens.insert(new Parse(tokens.getText(), span, chunkType, logProb,children[ci-1]));
        logProb=0;
      }            
      if (chunks[ci].startsWith("B-")) {
        chunkStart = ci;
        chunkType = chunks[ci].substring(2);
      }
      logProb+=Math.log(probs[ci]);
    }
    if (!chunks[chunks.length-1].equals("O")) {
      int ci = chunks.length;
      Span span = new Span(children[chunkStart].getSpan().getStart(),children[ci-1].getSpan().getEnd());
      tokens.insert(new Parse(tokens.getText(), span, chunkType, logProb,children[ci-1]));
    }
    return tokens;
  }

  @Override
  public Parse[] parse(Parse tokens, int numParses) {
    //TODO: get multiple tag sequences and chunk each.
    return new Parse[] {parse(tokens)};
  }
  
}
