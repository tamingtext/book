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