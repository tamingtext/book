package com.tamingtext.texttamer.solr;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import opennlp.tools.lang.english.SentenceDetector;

import org.apache.solr.analysis.BaseTokenizerFactory;

public class SentenceTokenizerFactory extends BaseTokenizerFactory {
  
  SentenceDetector detector;
  
  @Override
  public void init(Map<String,String> args) {
    super.init(args);
    String modelDirectory = args.get("modelDirectory");
    if (modelDirectory == null || modelDirectory.equals("")){
      modelDirectory = System.getProperty("model.dir");
    }
    if (modelDirectory != null) {
      modelDirectory += File.separator + "english" + File.separator + "sentdetect";
      log.info("Loading models from {}", modelDirectory);
    } else {
      throw new RuntimeException("Configuration Error: modelDirectory argument or model.dir system property not set "+modelDirectory);
    }
    File sentenceDir = new File(modelDirectory);
    File model = new File(sentenceDir, "EnglishSD.bin.gz");
    
    try {
     detector = new SentenceDetector(model.getAbsolutePath());
    }
    catch (IOException e) {
      throw new RuntimeException("Configuration Error: " + model.getAbsolutePath(), e);
    }
  }

  public SentenceTokenizer create(Reader input) {
    return new SentenceTokenizer(input, detector);
  }
}