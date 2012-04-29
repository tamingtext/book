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