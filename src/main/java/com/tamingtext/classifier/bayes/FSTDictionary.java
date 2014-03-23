package com.tamingtext.classifier.bayes;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.store.InputStreamDataInput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Outputs;
import org.apache.lucene.util.fst.UpToTwoPositiveIntOutputs;
import org.apache.lucene.util.fst.Util;

import com.google.common.base.Preconditions;
import com.google.common.io.Closer;


/** Represents a FST-based term dictionary that can be used to retrieve the index and document
 *  frequency for a term.
 *  <p/>
 *  <b>NOTE</b>: This class' {@link #getEntry(String)} method is not threadsafe due to the 
 *  re-use of internal buffers for dictionary lookups. Furthermore the entry objects returned
 *  from this method are re-used, so that if you need to preserve these objects in a collection
 *  please use the copy constructor in Entry instead.
 */
public class FSTDictionary {

  public static final Outputs<Object> OUTPUTS = UpToTwoPositiveIntOutputs.getSingleton(true);

  public static final String NUM_DOCS = "\0NUM*DOCS\0";
  
  final FST<Object> fst;
  final Entry entry = new Entry();
  final BytesRef scratchBytes = new BytesRef();
  final int size;
  
  public FSTDictionary(InputStream r) throws IOException {
    Closer c = Closer.create();
    c.register(r);
    try {
      InputStreamDataInput in = new InputStreamDataInput(r);
      FST<Object> fst = new FST<Object>(in, FSTDictionary.OUTPUTS);
      this.fst = fst;
      long sizeLong = fst.getArcWithOutputCount();
      if (sizeLong > Integer.MAX_VALUE) {
        // Mahout Vectors can't have cardinality larger than Integger.MAX_VALUE.
        throw new IllegalStateException("Term count exceeds Integer.MAX_VALUE, "
            + "we will have issues vectorizing documents with this dictionary");
      }
      this.size = (int) sizeLong;
    }
    finally {
      c.close();
    }
  }

  public int getDocumentCount() throws IOException {
    Entry e = getEntry(NUM_DOCS);
    return e.df;
  }
  
  public int getTermCount() throws IOException {
    return size;
  }
  
  public Entry getEntry(String term) throws IOException {
    scratchBytes.copyChars(term);
    Object o = Util.get(fst, scratchBytes);
    if (o == null) {
      return null;
    }
    else if (o instanceof Long && term.equals(NUM_DOCS)) {
      entry.term  = term;
      entry.index = -1;
      
      final long df = ((Long) o).longValue();
      Preconditions.checkState(df <= Integer.MAX_VALUE, 
          "FST entry.first contained value larger than Integer.MAX_VALUE for document count");
      
      entry.df = (int) df;
    }
    else if (o instanceof UpToTwoPositiveIntOutputs.TwoLongs) {
      final UpToTwoPositiveIntOutputs.TwoLongs tl = (UpToTwoPositiveIntOutputs.TwoLongs) o;
      entry.term  = term;
      
      Preconditions.checkState(tl.first <= Integer.MAX_VALUE, 
          "FST entry.first contained value larger than Integer.MAX_VALUE for " + term);
      Preconditions.checkState(tl.second <= Integer.MAX_VALUE, 
          "FST entry.second contained value larger than Integer.MAX_VALUE for " + term);
      
      entry.index = (int) tl.first;
      entry.df    = (int) tl.second;
    }
    else {
      throw new IllegalStateException("Unexpected class returned: " + o.getClass().getName());
    }
    return entry;
  }
  

  public static class Header {
    /** total dictionary term count */
    long termCount;

    /** the header of the dictionary */
    String columns;
  }

  /** Dictionary entry value class */
  public static class Entry {
    public Entry () { 
      /* no-op */
    }

    public Entry(Entry other) {
      this.term = other.term;
      this.index = other.index;
      this.df = other.df;
    }

    /** the current term */
    String term;

    /** the current term index, used as a numeric id */
    int index;

    /**
     * the current term document frequency the number of documents in
     * which the term appears.
     */
    int df;
  }

}
