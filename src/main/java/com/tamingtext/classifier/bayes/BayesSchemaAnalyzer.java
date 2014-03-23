package com.tamingtext.classifier.bayes;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.util.Version;

/** Emulates the TokenizerFactory defined in the bayes schema, but
 *  wraps it up as a Lucene Analyzer.
 */
public final class BayesSchemaAnalyzer extends Analyzer {
  
  final StandardTokenizerFactory factory;
  final ResourceLoader loader; 
  final Map<String,String> params;
  
  public BayesSchemaAnalyzer() {
    params = Collections.singletonMap("luceneMatchVersion", "4.6");
    loader = new ClasspathResourceLoader();
    factory = new StandardTokenizerFactory(new HashMap<String,String>(params));
  }
  
  @SuppressWarnings("resource")
  @Override
  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
    Tokenizer tokenizer = factory.create(reader);
    TokenFilter tok = new LowerCaseFilter(Version.LUCENE_46, tokenizer);

    Map<String,String> porterParams = new HashMap<String,String>(params);
    porterParams.put("language", "English");
    SnowballPorterFilterFactory porterfilter = new SnowballPorterFilterFactory(porterParams); 
    
    try {
      porterfilter.inform(loader);
      tok = porterfilter.create(tok);
    }
    catch (IOException ex) {
      throw new IllegalStateException("Exception loading resources for porter filter", ex);
    }
    
    return new TokenStreamComponents(tokenizer, tok);
  }
}