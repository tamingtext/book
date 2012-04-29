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

public class LevenshteinDistance {

  //<start id="ed-levenshtein"/>
  public int levenshteinDistance(char s[], char t[]) {
    int m = s.length;
    int n = t.length;
    int d[][] = new int[m+1][n+1]; //<co id="co.fuzzy.levenshtein.distance"/>

    for (int i=0;i<=m;i++) //<co id="co.fuzzy.levenshtein.init1"/>
      d[i][0] = i;
    for (int j=0;j<=n;j++)
      d[0][j] = j;
    for (int j=1;j<=n;j++) {
      for (int i=1;i<=m;i++) {
        if (s[i-1] == t[j-1]) {
          d[i][j] = d[i-1][j-1];//<co id="co.fuzzy.levenshtein.match"/>
        } else {
          d[i][j] = Math.min(Math.min(
                  d[i-1][j] + 1, //<co id="co.fuzzy.levenshtein.insertion"/>
                  d[i][j-1] + 1),
                  d[i-1][j-1] + 1);
        }
      }
    }
    return d[m][n];
  }
  /*
  <calloutlist>
  <callout arearefs="co.fuzzy.levenshtein.distance"><para>Allocate the distance matrix.</para></callout>
  <callout arearefs="co.fuzzy.levenshtein.init1"><para>Initialize an upper bound on distance.</para></callout>
  <callout arearefs="co.fuzzy.levenshtein.match"><para>Cost is the same as the previous match.</para></callout>
  <callout arearefs="co.fuzzy.levenshtein.insertion"><para>Cost is one for an insertion, deletion, or substitution.</para></callout>
  </calloutlist>
   */
  //<end id="ed-levenshtein"/>
}
