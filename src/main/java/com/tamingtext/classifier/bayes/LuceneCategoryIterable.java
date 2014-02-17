
package com.tamingtext.classifier.bayes;

import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.mahout.math.Vector;
import org.apache.mahout.utils.vectors.TermInfo;
import org.apache.mahout.utils.vectors.lucene.LuceneIterator;
import org.apache.mahout.vectorizer.Weight;

/**
 * {@link Iterable} counterpart to {@link LuceneIterator}.
 */
public class LuceneCategoryIterable implements Iterable<Vector> {

  public static final double NO_NORMALIZING = -1.0;

  private final IndexReader indexReader;
  private final String categoryField;
  private final String field;
  private final String idField;
  private final TermInfo terminfo;
  private final double normPower;
  private final double maxPercentErrorDocs;
  private final Weight weight;

  public LuceneCategoryIterable(IndexReader reader, String idField, String field, String categoryField, TermInfo terminfo, Weight weight) {
    this(reader, idField, field, categoryField, terminfo, weight, NO_NORMALIZING);
  }

  public LuceneCategoryIterable(IndexReader indexReader, String idField, String field, String categoryField, TermInfo terminfo, Weight weight,
      double normPower) {
    this(indexReader, idField, field, categoryField, terminfo, weight, normPower, 0);
  }

  /**
   * Produce a LuceneIterable that can create the Vector plus normalize it.
   *
   * @param indexReader         {@link org.apache.lucene.index.IndexReader} to read the documents from.
   * @param idField             field containing the id. May be null.
   * @param field               field to use for the Vector
   * @param categoryField       field to use for Vector category information
   * @param normPower           the normalization value. Must be nonnegative, or {@link #NO_NORMALIZING}
   * @param maxPercentErrorDocs the percentage of documents in the lucene index that can have a null term vector
   */
  public LuceneCategoryIterable(IndexReader indexReader,
                        String idField,
                        String field,
                        String categoryField,
                        TermInfo terminfo,
                        Weight weight,
                        double normPower,
                        double maxPercentErrorDocs) {
    this.indexReader = indexReader;
    this.idField = idField;
    this.field = field;
    this.categoryField = categoryField;
    this.terminfo = terminfo;
    this.normPower = normPower;
    this.maxPercentErrorDocs = maxPercentErrorDocs;
    this.weight = weight;
  }

  @Override
  public Iterator<Vector> iterator() {
    return new LuceneCategoryIterator(indexReader, idField, field, categoryField, terminfo, weight, normPower, maxPercentErrorDocs);
  }
}
