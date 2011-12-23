package com.tamingtext.texttamer.solr;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.solr.analysis.BaseTokenFilterFactory;

import com.tamingtext.util.NameFinderEngine;

public class NameFilterFactory extends BaseTokenFilterFactory {
  private NameFinderEngine nameFinderEngine;
  
  public NameFilterFactory() {
    
  }
  
  public void init(Map<String, String> args) {
    super.init(args);
    String modelDirectory = args.get("modelDirectory");
    
    try {
      nameFinderEngine = new NameFinderEngine(modelDirectory);
    }
    catch (IOException e) {
      throw (RuntimeException) new RuntimeException().initCause(e);
    }
  }

  public NameFilter create(TokenStream ts) {
    return new NameFilter(ts,
        nameFinderEngine.getModelNames(), nameFinderEngine.getNameFinders());
  }
}
