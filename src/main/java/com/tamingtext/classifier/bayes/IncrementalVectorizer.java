package com.tamingtext.classifier.bayes;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.function.ObjectIntProcedure;
import org.apache.mahout.math.map.OpenObjectIntHashMap;
import org.apache.mahout.vectorizer.Weight;
import org.slf4j.Logger;

public class IncrementalVectorizer {

  public static final Logger logger = org.slf4j.LoggerFactory
      .getLogger(FSTDictionaryBuilder.class);

  final Analyzer a;
  final FSTDictionary dict;
  final Weight weight;


  public IncrementalVectorizer(Analyzer a, FSTDictionary dict, Weight weight) throws IOException {
    this.a = a;
    this.dict = dict;
    this.weight = weight;
  }
  
  public Vector createVector(String fieldname, Reader reader) throws IOException {
    //<start id="mahout.bayes.tokenize"/>
    OpenObjectIntHashMap<String> tokens = new OpenObjectIntHashMap<String>();
    TokenStream ts = a.tokenStream(fieldname, reader);
    ts.reset();
    CharTermAttribute attr = ts.getAttribute(CharTermAttribute.class);
    while (ts.incrementToken()) {
      tokens.adjustOrPutValue(attr.toString(), 1, 1);
    }
    ts.close();
    //<end id="mahout.bayes.tokenize"/>
    
    final int docTermCount = tokens.size();
    final int numDocs     = dict.getDocumentCount(); 
    final Vector vector = new RandomAccessSparseVector(dict.getTermCount(), docTermCount); // guess at initial size
    try {
      tokens.forEachPair(new ObjectIntProcedure<String>() {
        public boolean apply(String term, int termFrequency) {
          try {
            FSTDictionary.Entry e = dict.getEntry(term);
            double wt = weight.calculate(termFrequency, e.df, docTermCount, numDocs);
            vector.setQuick((int) e.index, wt);
          } catch (IOException e) {
            throw new TermLookupException("Unable to find dictionary entry for " + term, e);
          }
          return true;
        }
      });
    }
    catch (TermLookupException ex) {
      // this exception handling is kinda wacky, but it works.
      logger.warn(ex.getMessage() + " init cause: " + ex.getCause().getMessage());
      throw (IOException) ex.getCause();
    }
    
    //TODO: perform normalization here?
    
    return vector;
  }
  
  public static class TermLookupException extends RuntimeException {
    private static final long serialVersionUID = 7321838275376629963L;
    public TermLookupException(String message, Throwable initCause) {
      super(message, initCause);
    }
  }
}
