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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.benchmark.byTask.feeds.ContentSource;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.lucene.benchmark.byTask.utils.StreamUtils;

public class WexWikiContentSource extends ContentSource {
  
  private static final Map<String,Integer> ELEMENTS = new HashMap<String,Integer>();
  private static final int TITLE = 0;
  private static final int DATE = TITLE + 1;
  private static final int BODY = DATE + 1;
  private static final int ID = BODY + 1;
  private static final int CATEGORY = ID + 1;
  private static final int LENGTH = CATEGORY + 1;
  // LENGTH is used as the size of the tuple, so whatever constants we need that
  // should not be part of the tuple, we should define them after LENGTH.
  private static final int PAGE = LENGTH + 1;

  private BufferedReader ir;
  
  WexWikiContentSource() {
    
  }
  
  private class Parser {
    String[] tuple = new String[LENGTH];
    
    public String[] next() throws IOException {
      
      String[] parts;
      
      do {
        String line = ir.readLine();
        if (line == null) return null;
        parts = line.split("\\t");
      } 
      while (parts.length != 5);
      
      tuple[ID] = parts[0];
      tuple[TITLE] = parts[1];
      tuple[DATE] = parts[2];
      tuple[BODY] = parts[4];
      tuple[CATEGORY] = parseCategory(parts[3]);
      
      return tuple;
    }
    
    final String CATEGORY_PREFIX = "<target>Category:";
    final int    CATEGORY_PREFIX_LEN = CATEGORY_PREFIX.length()-1;
    final String CATEGORY_SUFFIX = "</target>";
    final int    CATEGORY_SUFFIX_LEN = CATEGORY_SUFFIX.length()-1;
    
    final StringBuilder b = new StringBuilder();
    
    public String parseCategory(String input) {
      b.setLength(0);
      int start = 0;
      int end   = 0;
      while (true) {
        start = input.indexOf("target>Category:", end);
        if (start < 0) break;
        start += CATEGORY_PREFIX_LEN;
        end   = input.indexOf("</target>", start);
        if (end < start) break;
        b.append(input.substring(start, end)).append(";;");
        end   += CATEGORY_SUFFIX_LEN;
      }
      return b.toString();
    }
  }
  
  private File file;
  Parser parser = new Parser();
  
  public void resetInputs() throws IOException {
    super.resetInputs();
    ir = getReader(file);
  }
  
  public BufferedReader getReader(File file) throws IOException {
    InputStream is = StreamUtils.inputStream(file);
    return new BufferedReader(new InputStreamReader(is, "UTF-8"));
  }
  
  @Override
  public void close() throws IOException {
    if (ir != null) {
      ir.close();
      ir = null;
    }
  }
  
  Properties props = new Properties();
  
  @Override
  public DocData getNextDocData(DocData docData) throws NoMoreDataException,
      IOException {
    if (ir == null) {
      ir = getReader(file);
    }
    
    String[] tuple = parser.next();
    if (tuple == null) return null;
    
    docData.clear();
    docData.setID(Integer.parseInt(tuple[ID]));
    docData.setTitle(tuple[TITLE]);
    docData.setBody(tuple[BODY]);
    docData.setDate(tuple[DATE]);
    
    props.setProperty("category", tuple[CATEGORY]);
    docData.setProps(props);
    
    return docData;
  }
  
  @Override
  public void setConfig(Config config) {
    super.setConfig(config);
    String fileName = config.get("docs.file", null);
    if (fileName == null) {
      throw new IllegalArgumentException("docs.file must be set");
    }
    file = new File(fileName).getAbsoluteFile();
  }
}
