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

package com.tamingtext.qa;


/**
 *
 *
 **/
public interface QAParams {
  public static final String QA_PREFIX = "qa.";
  /**
   * The Size of the Passage window around a match, measured in Tokens
   */
  public static final String PRIMARY_WINDOW_SIZE = QA_PREFIX + "pws";

  public static final String ADJACENT_WINDOW_SIZE = QA_PREFIX + "aws";

  public static final String SECONDARY_WINDOW_SIZE = QA_PREFIX + "sws";

  public static final String QUERY_FIELD = QA_PREFIX + "qf";

  public static final String QA_ROWS = QA_PREFIX + "rows";

  public static final String SLOP = QA_PREFIX + "qSlop";

  public static final String BIGRAM_WEIGHT = QA_PREFIX + "bw";

  public static final String ADJACENT_WEIGHT = QA_PREFIX + "aw";

  public static final String SECOND_ADJ_WEIGHT = QA_PREFIX + "saw";

  public static final String COMPONENT_NAME = "qa";
}
