package com.tamingtext.classifier.maxent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.Span;

import com.tamingtext.util.NameFinderEngine;

public class NameFinderFeatureGenerator implements FeatureGenerator {

  NameFinderEngine eng;
  
  public NameFinderFeatureGenerator() throws IOException {
    this(null);
  }
  
  public NameFinderFeatureGenerator(NameFinderEngine eng) {
    if (eng == null) {
      try {
        eng = new NameFinderEngine();
      }
      catch (IOException e) {
        throw (RuntimeException) new RuntimeException().initCause(e);
      }
    }
    this.eng = eng;
  }
  
  @Override
  @SuppressWarnings("all")
  //<start id="maxent.examples.features"/>
  public Collection extractFeatures(String[] text) {
    NameFinderME[] finders = eng.getNameFinders(); //<co id="nffg.engine"/>
    String[] modelNames    = eng.getModelNames();
    
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
