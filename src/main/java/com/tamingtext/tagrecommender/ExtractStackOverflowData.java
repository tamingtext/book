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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractStackOverflowData {
  
  private static final Logger log = LoggerFactory.getLogger(ExtractStackOverflowData.class);

  File inputFile;
  File trainingOutputFile;
  File testOutputFile;
  
  int trainingDataSize = 100000;
  int testDataSize = 10000;
  
  public static void main(String [] args) {
    ExtractStackOverflowData si = new ExtractStackOverflowData();
    if (si.parseArgs(args)) {
      si.extract();
    }
  }
  
  
  public boolean parseArgs(String[] args) {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    Option helpOpt = DefaultOptionCreator.helpOption();
    
    Option inputFileOpt = obuilder.withLongName("inputFile").withRequired(true).withArgument(
        abuilder.withName("inputFile").withMinimum(1).withMaximum(1).create()).withDescription(
        "The input file").withShortName("i").create();
    
    Option trainingOutputOpt = obuilder.withLongName("trainingOutputFile").withRequired(true).withArgument(
        abuilder.withName("trainingOutputFile").withMinimum(1).withMaximum(1).create()).withDescription(
        "The training data output file").withShortName("tr").create();
    
    Option testOutputOpt = obuilder.withLongName("testOutputFile").withRequired(true).withArgument(
        abuilder.withName("testOutputFile").withMinimum(1).withMaximum(1).create()).withDescription(
        "The test data output file").withShortName("te").create();
    
    Option trainingDataSizeOpt = obuilder.withLongName("trainingDataSize").withRequired(false).withArgument(
        abuilder.withName("trainingDataSize").withMinimum(1).withMaximum(1).create()).withDescription(
        "The number of questions to extract for training data").withShortName("trs").create();
    
    Option testDataSizeOpt = obuilder.withLongName("testDataSize").withRequired(false).withArgument(
        abuilder.withName("testDataSize").withMinimum(1).withMaximum(1).create()).withDescription(
        "The number of questions to extract for training data").withShortName("tes").create();

    Group group = gbuilder.withName("Options").withOption(inputFileOpt).withOption(trainingOutputOpt)
         .withOption(testOutputOpt).withOption(trainingDataSizeOpt).withOption(testDataSizeOpt).create();
    
    try {
      Parser parser = new Parser();
      parser.setGroup(group);
      CommandLine cmdLine = parser.parse(args);
      
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return false;
      }
      
      inputFile = new File((String) cmdLine.getValue(inputFileOpt));
      trainingOutputFile = new File((String) cmdLine.getValue(trainingOutputOpt));
      testOutputFile = new File((String) cmdLine.getValue(testOutputOpt));

      if (cmdLine.hasOption(trainingDataSizeOpt)) {
        trainingDataSize = Integer.parseInt((String) cmdLine.getValue(trainingDataSizeOpt));
      }
      
      if (cmdLine.hasOption(testDataSizeOpt)) {
        testDataSize = Integer.parseInt((String) cmdLine.getValue(testDataSizeOpt));
      }     
      
    } catch (OptionException e) {
      log.error("Command-line option Exception", e);
      CommandLineUtil.printHelp(group);
      return false;
    }
    
    validate();
    return true;
  }

  protected void validate() {
    if (!inputFile.exists()) {
      throw new IllegalArgumentException("inputFile " + inputFile.getAbsolutePath() + " does not exist");
    }
    
    if (trainingDataSize < 1) {
      throw new IllegalArgumentException("trainingDataSize must be 1 or more");
    }
    
    if (testDataSize < 1) {
      throw new IllegalArgumentException("testDataSize must be 1 or more");
    }
    
    Util.validateFileWritable(trainingOutputFile);
    Util.validateFileWritable(testOutputFile);
  }
  
  public void extract() {
    XMLInputFactory xif = XMLInputFactory.newInstance();
    XMLStreamReader reader = null;
    InputStream is = null;
    
    XMLOutputFactory xof = XMLOutputFactory.newInstance();
    XMLStreamWriter writer = null;
    OutputStream os = null;
    
    try {
      log.info("Reading data from " + inputFile);
      
      is = new FileInputStream(inputFile);
      reader = xif.createXMLStreamReader(is);
      
      os = new FileOutputStream(trainingOutputFile);
      writer = xof.createXMLStreamWriter(os);
      int trainingDataCount = extractXMLData(reader, writer, trainingDataSize);
      os.close();
      
      os = new FileOutputStream(testOutputFile);
      writer = xof.createXMLStreamWriter(os);
      int testDataCount = extractXMLData(reader, writer, testDataSize);
      os.close();
      
      log.info("Extracted " + trainingDataCount + " rows of training data");
      log.info("Extracted " + testDataCount     + " rows of test data");
    }
    catch (XMLStreamException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /** Extract as many as <code>limit</code> questions from the <code>reader</code>
   *  provided, writing them to <code>writer</code>.
   * @param reader
   * @param writer
   * @param limit
   * @return
   * @throws XMLStreamException
   */
  protected int extractXMLData(XMLStreamReader reader, XMLStreamWriter writer, int limit) throws XMLStreamException {

    int questionCount = 0;
    int attrCount;
    boolean copyElement = false;
    
    writer.writeStartDocument();
    writer.writeStartElement("posts");
    writer.writeCharacters("\n");
    while (reader.hasNext() && questionCount < limit) {
      switch (reader.next()) {
      case XMLEvent.START_ELEMENT:
        if (reader.getLocalName().equals("row")) {
          attrCount = reader.getAttributeCount();
          for (int i=0; i < attrCount; i++) {
            // copy only the questions.
            if (reader.getAttributeName(i).getLocalPart().equals("PostTypeId") &&
                reader.getAttributeValue(i).equals("1")) {
              copyElement = true;
              break;
            }   
          }
          
          if (copyElement) {
            writer.writeCharacters("  ");
            writer.writeStartElement("row");
            for (int i=0; i < attrCount; i++) {
              writer.writeAttribute(
                  reader.getAttributeName(i).getLocalPart(), 
                  reader.getAttributeValue(i));
            }
            writer.writeEndElement();
            writer.writeCharacters("\n");
            copyElement = false;
            questionCount++;
          }
        }
        break;
      }
    }
    writer.writeEndElement();
    writer.writeEndDocument();
    writer.flush();
    writer.close();
    
    return questionCount;
  }
}
