package com.tamingtext.classifier.bayes;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.index.IndexReader;
import org.apache.mahout.utils.vectors.TermInfo;
import org.apache.mahout.utils.vectors.lucene.LuceneIterable;
import org.apache.mahout.utils.vectors.lucene.LuceneIterator;
import org.apache.mahout.vectorizer.Weight;

public class LuceneCategoryIterator extends LuceneIterator {

  protected final Set<String> categoryFieldSelector;
  protected final String categoryField;
  
  /**
   * Produce a LuceneIterable that can create the Vector plus normalize it.
   *
   * @param indexReader   {@link IndexReader} to read the documents from.
   * @param idField       field containing the id. May be null.
   * @param field         field to use for the Vector
   * @param categoryField field to use to obtain category information
   * @param termInfo      termInfo
   * @param weight        weight
   * @param normPower     the normalization value. Must be non-negative, or {@link LuceneIterable#NO_NORMALIZING}
   */
  public LuceneCategoryIterator(IndexReader indexReader, 
                        String idField, 
                        String field, 
                        String categoryField, 
                        TermInfo termInfo, 
                        Weight weight,
                        double normPower) {
    this(indexReader, idField, field, categoryField, termInfo, weight, normPower, 0.0);
  }

  /**
   * @param indexReader {@link IndexReader} to read the documents from.
   * @param idField    field containing the id. May be null.
   * @param field      field to use for the Vector
   * @param categoryField field to use to obtain category information
   * @param termInfo   termInfo
   * @param weight     weight
   * @param normPower  the normalization value. Must be non-negative, or {@link LuceneIterable#NO_NORMALIZING}
   * @param maxPercentErrorDocs most documents that will be tolerated without a term freq vector. In [0,1].
   * @see #LuceneIterator(org.apache.lucene.index.IndexReader, String, String, org.apache.mahout.utils.vectors.TermInfo,
   * org.apache.mahout.vectorizer.Weight, double)
   */
  public LuceneCategoryIterator(IndexReader indexReader,
                        String idField,
                        String field,
                        String categoryField,
                        TermInfo termInfo,
                        Weight weight,
                        double normPower,
                        double maxPercentErrorDocs) {
    super(indexReader, idField, field, termInfo, weight, normPower, maxPercentErrorDocs);
    this.categoryField = categoryField;
    if (categoryField != null) {
      categoryFieldSelector = new TreeSet<String>();
      categoryFieldSelector.add(categoryField);
    } else {
      categoryFieldSelector = null;
    }
  }
  
  
  @Override
  protected String getVectorName(int documentIndex) throws IOException {
    String name = super.getVectorName(documentIndex);
    
    String category;
    if (categoryField != null) {
      category = indexReader.document(documentIndex, categoryFieldSelector).get(categoryField);
    } else {
      category = "NONE";
    }
    
    return "/" + category + "/" + name;
  }

}
