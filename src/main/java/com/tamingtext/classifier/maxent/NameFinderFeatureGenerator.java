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

package com.tamingtext.classifier.maxent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.Span;

import com.tamingtext.util.NameFinderFactory;

public class NameFinderFeatureGenerator implements FeatureGenerator {

  NameFinderFactory factory;
  
  public NameFinderFeatureGenerator() throws IOException {
    this(null);
  }
  
  public NameFinderFeatureGenerator(NameFinderFactory factory) {
    if (factory == null) {
      try {
        factory = new NameFinderFactory();
      }
      catch (IOException e) {
        throw (RuntimeException) new RuntimeException().initCause(e);
      }
    }
    this.factory = factory;
  }
  
  @Override
  @SuppressWarnings("all")
  //<start id="maxent.examples.features"/>
  public Collection extractFeatures(String[] text) {
    NameFinderME[] finders = factory.getNameFinders(); //<co id="nffg.engine"/>
    String[] modelNames    = factory.getModelNames();
    
    Collection<String> features = new ArrayList<String>();
    StringBuilder builder = new StringBuilder();
    
    for (int i=0; i < finders.length; i++) { 
      Span[] spans = finders[i].find(text); //<co id="nffg.find"/>
      String model = modelNames[i];
      
      for (int j=0; j < spans.length; j++) {
        int start = spans[j].getStart();   //<co id="nffg.combine"/>
        int end   = spans[j].getEnd();
        
        builder.setLength(0);
        builder.append(model).append("=");
        for (int k = start; k < end; k++ ) {
          builder.append(text[k]).append('_');
        }
        builder.setLength(builder.length()-1);
        features.add(builder.toString()); //<co id="nffg.return"/>
      }
    } 
    return features;
  }
  /*
  <calloutlist>
    <callout arearefs="nffg.engine">Get Name Finders</callout>
    <callout arearefs="nffg.find">Find Names</callout>
    <callout arearefs="nffg.combine">Extract Names</callout>
    <callout arearefs="nffg.return">Collect Names</callout>
  </calloutlist>
   */
  //<end id="maxent.examples.features"/>
}
