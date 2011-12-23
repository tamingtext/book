package com.tamingtext.classifier.mlt;

import java.util.Comparator;

import org.apache.mahout.classifier.ClassifierResult;

import com.tamingtext.classifier.mlt.TrainMoreLikeThis.MatchMode;

/** data type for matching categories */
public class CategoryHits extends ClassifierResult {
  int hits;
  
  public void setHits(int hits) {
    this.hits = hits;
  }
  
  public int getHits() {
    return this.hits;
  }
  
  public void incrementHits(int hits) {
    this.hits += hits;
  }
  
  public void incrementScore(double score) {
    this.setScore(this.getScore() + score);
  }
  
  public static Comparator<CategoryHits> comparatorForMode(MatchMode mode) {
    switch (mode) {
      case TFIDF:
        return byScoreComparator();
      case KNN:
        return byHitsComparator();
      default:
        throw new IllegalArgumentException("Unkonwn MatchMode" + mode);
    }
  }
  
  public static Comparator<CategoryHits> byHitsComparator() {
    return new Comparator<CategoryHits>() {
      @Override
      public int compare(CategoryHits o1, CategoryHits o2) {
        int cmp = o2.hits - o1.hits;
        if (cmp != 0) return cmp;
        
        cmp = Double.compare(o2.getScore(), o1.getScore());
        if (cmp != 0) return cmp;
        
        return o1.getLabel().compareTo(o2.getLabel());
      } 
    };
  }
  
  public static Comparator<CategoryHits> byScoreComparator() {
    return new Comparator<CategoryHits>() {
      @Override
      public int compare(CategoryHits o1, CategoryHits o2) {
        int cmp = Double.compare(o2.getScore(), o1.getScore());
        if (cmp != 0) return cmp;
        
        cmp = o2.hits - o1.hits;
        if (cmp != 0) return cmp;

        return o1.getLabel().compareTo(o2.getLabel());
      } 
    };
  }
}