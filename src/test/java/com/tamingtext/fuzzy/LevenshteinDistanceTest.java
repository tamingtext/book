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

package com.tamingtext.fuzzy;


import com.tamingtext.TamingTextTestJ4;
import org.junit.Test;

/**
 *
 *
 **/
public class LevenshteinDistanceTest extends TamingTextTestJ4 {
  @Test
  public void testLevenshtein() throws Exception {
    LevenshteinDistance lev = new LevenshteinDistance();
    assertEquals(0, lev.levenshteinDistance("zoo".toCharArray(), "zoo".toCharArray()));
    assertEquals(1, lev.levenshteinDistance("zoo".toCharArray(), "zoom".toCharArray()));//insert
    assertEquals(1, lev.levenshteinDistance("zoo".toCharArray(), "zo".toCharArray()));//delete
    assertEquals(1, lev.levenshteinDistance("zoo".toCharArray(), "boo".toCharArray()));//substitute
    assertEquals(2, lev.levenshteinDistance("zoo".toCharArray(), "boom".toCharArray()));//multiple
    assertEquals(3, lev.levenshteinDistance("zoo".toCharArray(), "".toCharArray()));//compare to empty
    assertEquals(3, lev.levenshteinDistance("".toCharArray(), "zoo".toCharArray()));//multiple
    assertEquals(7, lev.levenshteinDistance("zoo".toCharArray(), "zoological".toCharArray()));//multiple
    assertEquals(3, lev.levenshteinDistance("zoo".toCharArray(), "bed".toCharArray()));//multiple

    assertEquals(2, lev.levenshteinDistance("taming text".toCharArray(), "tamming test".toCharArray()));//book example
  }
}
