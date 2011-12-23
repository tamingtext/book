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

package com.tamingtext.tagrecommender;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Transformed for Stack Overflow data dump tags for the Data Import Handler.
 *  Input is tags in:
 *  
 *  <code>&lt;tag1&gt;&lt;tag2&gt;&lt;tag3&gt;&lt;tag4&gt;</code>
 *  
 *  Output is creates a list of separate tags which will be inserted into the
 *  row using the "tags" property.
 *  
 * @author drew
 *
 */
@SuppressWarnings("unchecked")
//<start id="tag.examples.xform"/> 
public class StackOverflowTagTransformer {
  public Object transformRow(Map<String,Object> row) {
    List<String> tags = (List<String>) row.get("tags");
    if (tags != null) {
      Collection<String> outputTags = StackOverflowStream.parseTags(tags);
      row.put("tags", outputTags);
    }
    return row;
  }
}
//<end id="tag.examples.xform"/>