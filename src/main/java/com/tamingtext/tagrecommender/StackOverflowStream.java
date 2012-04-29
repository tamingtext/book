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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackOverflowStream implements Iterator<StackOverflowPost>, Iterable<StackOverflowPost> {
  private static final Logger log = LoggerFactory.getLogger(StackOverflowStream.class);

  XMLInputFactory xif = null;
  XMLStreamReader reader = null;
  InputStream is = null;

  final StackOverflowPost post = new StackOverflowPost();

  public StackOverflowStream() throws IOException, XMLStreamException {
    this(null);
  }

  public StackOverflowStream(String inputFile) throws IOException, XMLStreamException {
    xif = XMLInputFactory.newInstance();
    if (inputFile != null) {
      this.open(inputFile);
    }
  }
  
  public void open(String inputFile) throws IOException, XMLStreamException {
    if (is != null) close();
    
    is = new FileInputStream(inputFile);
    reader = xif.createXMLStreamReader(is);
  }

  public void close() {
    try {
      reader.close();
    }
    catch (XMLStreamException ex) {
      log.warn("exception on reader close", ex);
    }
    finally {
      reader = null;
    }
    
    try {
      is.close();
    }
    catch (IOException ex) {
      log.warn("exception on input stream close", ex); 
    }
    finally {
      is = null;
    }
  }
  
  private boolean nextQueued = false;
  
  public boolean hasNext() {
    if (!nextQueued) {
      try {
        queueNext();
      }
      catch (XMLStreamException e) {
        throw (RuntimeException) new RuntimeException().initCause(e);
      }
    }
    
    return nextQueued;
  }

  protected void queueNext() throws XMLStreamException {
    LOOP: while (reader.hasNext()) {
      switch (reader.next()) {
      case XMLEvent.START_ELEMENT:
        if (reader.getLocalName().equals("row")) {
          populatePost();
          
          // we're interested in questions only.
          if (post.getPostTypeId() == 1) {
            nextQueued = true;
            break LOOP;
          }
        }
        break;
      }
    }
  }
  
  public StackOverflowPost next() {
    if (hasNext()) {
      nextQueued = false;
      return post;
    }
    else {
      throw new NoSuchElementException();
    }
  }
  
  protected void populatePost() {
    post.reInit();
    
    int attrCount = reader.getAttributeCount();
    for (int i=0; i < attrCount; i++) {
      attributeToPostField(
          reader.getAttributeLocalName(i),
          reader.getAttributeValue(i),
          post
      );
    }
  }
  
  
  public static Collection<String> parseTags(String tags) {
    return parseTags(Collections.singletonList(tags));
  }
  
  public static Collection<String> parseTags(Collection<String> tags) {
    List<String> outputTags = new LinkedList<String>();
    int start = -1, len = -1;
    
    for (String tag: tags) {
      char[] tagc = tag.toCharArray();
      start = -1; len = tagc.length;
      for (int i=0 ; i<len; i++) {
        if (tagc[i] == '<') {
          start = i;
        }
        else if (tagc[i] == '>') {
          outputTags.add(String.valueOf(tagc, start+1, i - start - 1));
        }
      }
    }
    
    return outputTags;
  }
  
  /** writes the specified attribute to a field in the supplied post object */
  protected void attributeToPostField(String name, String value, StackOverflowPost post) {
     if (name.equals("Id")) {
       post.setId(Integer.parseInt(value));
     }
     if (name.equals("PostTypeId")) {
       post.setPostTypeId(Integer.parseInt(value));
     }
     else if (name.equals("Title")) {
       post.setTitle(value);
     } else if (name.equals("Body")) {
       post.setBody(value);
     }
     else if (name.equals("Tags")) {
       post.setTags(parseTags(value));
     }
     else if (name.equals("CreationDate")) {
       post.setCreationDate(value);
     }
     else if (name.equals("AcceptedAnswerId")) {
       post.setAcceptedAnswerId(Integer.parseInt(value));
     }
     else if (name.equals("OwnerUserId")) {
       post.setOwnerUserId(Integer.parseInt(value));
     }
     else if (name.equals("Score")) {
       post.setScore(Integer.parseInt(value));
     }
     else if (name.equals("ViewCount")) {
       post.setViewCount(Integer.parseInt(value));
     }
     else if (name.equals("AnswerCount")) {
       post.setAnswerCount(Integer.parseInt(value));
     }
     else if (name.equals("CommentCount")) {
       post.setCommentCount(Integer.parseInt(value));
     }
     else if (name.equals("FavoriteCount")) {
       post.setFavoriteCount(Integer.parseInt(value));
     }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<StackOverflowPost> iterator() {
    return this;
  }
  
  public static void main(String[] args) throws Exception {
    StackOverflowStream stream = new StackOverflowStream();
    stream.open("/home/drew/taming-text/classification-examples/stackoverflow-corpus/training-data.xml");
    while (stream.hasNext()) {
      System.err.println(stream.next());
    }
  }
}
