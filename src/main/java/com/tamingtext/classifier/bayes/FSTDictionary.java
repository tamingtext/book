package com.tamingtext.classifier.bayes;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.store.InputStreamDataInput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Outputs;
import org.apache.lucene.util.fst.UpToTwoPositiveIntOutputs;
import org.apache.lucene.util.fst.Util;

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

  final FST<Object> fst;
  final Entry entry = new Entry();
  final BytesRef scratchBytes = new BytesRef();

  public FSTDictionary(InputStream r) throws IOException {
    Closer c = Closer.create();
    c.register(r);
    try {
      InputStreamDataInput in = new InputStreamDataInput(r);
      FST<Object> fst = new FST<Object>(in, FSTDictionary.OUTPUTS);
      this.fst = fst;
    }
    finally {
      c.close();
    }
  }

  public Entry getEntry(String term) throws IOException {
    scratchBytes.copyChars(term);
    Object o = Util.get(fst, scratchBytes);
    if (o == null) {
      return null;
    }
    else if (o instanceof UpToTwoPositiveIntOutputs.TwoLongs) {
      UpToTwoPositiveIntOutputs.TwoLongs tl = (UpToTwoPositiveIntOutputs.TwoLongs) o;
      entry.term  = term;
      entry.index = tl.first;
      entry.df    = tl.second;
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
    long index;

    /**
     * the current term docuemtn frequency, e.g the number of documents it
     * appears in
     */
    long df;
  }

}
