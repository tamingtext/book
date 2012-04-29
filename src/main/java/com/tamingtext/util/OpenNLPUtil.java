package com.tamingtext.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenNLPUtil {
  
  private static final Logger log = LoggerFactory.getLogger(OpenNLPUtil.class);
  private static final String DEFAULT_MODEL_LANGUAGE = "en";
  
  public static String getModelDirectory(Map<String, String> args) {
    String modelDirectory = null;
    
    if (args != null) {
      modelDirectory = args.get("modelDirectory");
    }
    
    if (modelDirectory == null || modelDirectory.equals("")) {
      modelDirectory = System.getProperty("model.dir");
    }
    
    if (modelDirectory == null || modelDirectory.equals("")) {
      throw new RuntimeException("Configuration Error: modelDirectory argument "
          + "or model.dir system property not set: "+modelDirectory);
    }
    else {
      log.info("Model directory is: {}", modelDirectory);
    }
    
    return modelDirectory;
  }

  public static String getModelLanguage(Map<String, String> args) {
    String modelLanguage = null;
    
    if (args != null) {
      args.get("modelLanguage");
    }
    
    if (modelLanguage == null || modelLanguage.equals("")) {
      modelLanguage = System.getProperty("model.language");
    }
    
    if (modelLanguage == null || modelLanguage.equals("")) {
      log.warn("modelLanguage argument or model.language property not set, "
          + "using default: " + DEFAULT_MODEL_LANGUAGE);
      modelLanguage = DEFAULT_MODEL_LANGUAGE;
    } 
    
    return modelLanguage;
  }
}
