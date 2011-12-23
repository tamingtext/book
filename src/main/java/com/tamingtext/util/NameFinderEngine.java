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

package com.tamingtext.util;

import java.io.File;
import java.io.IOException;

import opennlp.maxent.io.GISModelReader;
import opennlp.maxent.io.PooledGISModelReader;
import opennlp.tools.lang.english.SentenceDetector;
import opennlp.tools.namefind.NameFinderME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Encapsulates OpenNLP's NameFinder by providing a mechanism to load
 *  all of the name finder models files found in a single directory into memory
 *  and instantiating an array of NameFinderME objects.
 */
public class NameFinderEngine {
  
  private static final Logger log = LoggerFactory.getLogger(NameFinderEngine.class);
  
  SentenceDetector detector;
  NameFinderME[] finders;
  String[] modelNames;
  
  public NameFinderEngine() throws IOException {
    this(null);
  }

  /** Create a NameFinderEngine that loads models from the specified directory,
   *  or, reads the <code>model.dir</code> system property in order to determine
   *  if the <code>modelDirectory</code> is <code>null</code> or empty.
   * @param modelDirectory
   */
  public NameFinderEngine(String modelDirectory) throws IOException {
    loadNameFinders(modelDirectory);
    loadSentenceDetector(modelDirectory);
  }
  
  protected String getModelDirectory(String modelDirectory) {
    if (modelDirectory == null || modelDirectory.equals("")){
      modelDirectory = System.getProperty("model.dir");
    }
    if (modelDirectory != null) {
      log.info("Model directory is: {}", modelDirectory);
    } else {
      throw new RuntimeException("Configuration Error: modelDirectory " +
          "argument or model.dir system property not set "+ modelDirectory);
    }
    return modelDirectory;
  }
  
  protected File[] findNameFinderModels(String modelDirectory) {
    modelDirectory  = getModelDirectory(modelDirectory);
    modelDirectory += File.separator + "english" + File.separator + "namefind";
    log.info("Loading models from {}", modelDirectory);
    File[] models = new File(modelDirectory).listFiles();
    if (models == null) {
      throw new RuntimeException("Configuration Error: No models in "+modelDirectory);
    }
    return models;
  }
  
  protected void loadSentenceDetector(String modelDirectory) throws IOException {
    modelDirectory  = getModelDirectory(modelDirectory);
    String model = modelDirectory +  
      File.separator + "english" + 
      File.separator + "sentdetect" +
      File.separator + "EnglishSD.bin.gz";
    detector = new SentenceDetector(model);
  }
  
  protected void loadNameFinders(String modelDirectory) throws IOException {
    //<start id="maxent.examples.namefinder.setup"/> 
    File modelFile;
    GISModelReader modelReader;

    File[] models  //<co id="nfe.findmodels"/>
      = findNameFinderModels(modelDirectory); 
    String[] modelNames    
      = new String[models.length];
    NameFinderME[] finders 
      = new NameFinderME[models.length];
    
    for (int fi=0; fi < models.length; fi++) {
      modelFile      = models[fi];
      modelNames[fi] = modelNameFromFile(modelFile); //<co id="nfe.modelname"/>
      
      log.info("Loading model {}", modelFile);
      modelReader = new PooledGISModelReader(modelFile); //<co id="nfe.modelreader"/>
      finders[fi] = new NameFinderME(modelReader.getModel());
      
    }
    /*<calloutlist>
      <callout arearefs="nfe.findmodels">Find Models</callout>
      <callout arearefs="nfe.modelname">Determine Model Name</callout>
       <callout arearefs="nfe.modelreader">Read Model</callout>
    </calloutlist>*/
    //<end id="maxent.examples.namefinder.setup"/>
    
    this.finders    = finders;
    this.modelNames = modelNames;
  }
  protected String modelNameFromFile(File modelFile) {
    String modelName = modelFile.getName();
    int nameStart = modelName.lastIndexOf(System.getProperty("file.separator")) + 1;
    int nameEnd = modelName.indexOf('.', nameStart);
    if (nameEnd == -1) {
      nameEnd = modelName.length();
    }
    
    return modelName.substring(nameStart, nameEnd);
  }
  
  /** Obtain a reference to an english sentence detector to use in conjunction 
   *  with the NameFinders
   */
  public SentenceDetector getSentenceDetector() {
    return detector;
  }
  
  /** Obtain a reference to the array of NameFinderME's loaded by the engine. 
   * @return
   */
  public NameFinderME[] getNameFinders() {
    return finders;
  }
  
  /** Returns the names of each of the models loaded by the engine, an array
   *  parallel with the array returned by {@link #getFinders()}
   * @return
   */
  public String[] getModelNames() {
    return modelNames;
  }
  
}
