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

package com.tamingtext.opennlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import org.junit.Test;

import com.tamingtext.TamingTextTestJ4;
import com.tamingtext.qa.ChunkParser;

public class ChunkParserTest extends TamingTextTestJ4 {

  @Test
  public void test() throws IOException {

    File modelDir = getModelDir();
    //<start id="openChunkParse"/>
    FileInputStream chunkerStream = new FileInputStream(
        new File(modelDir,"en-chunker.bin"));
    ChunkerModel chunkerModel = new ChunkerModel(chunkerStream);
    ChunkerME chunker = new ChunkerME(chunkerModel);
    FileInputStream posStream = new FileInputStream(
        new File(modelDir,"en-pos-maxent.bin"));
    POSModel posModel = new POSModel(posStream);
    POSTaggerME tagger =  new POSTaggerME(posModel);
    Parser parser = new ChunkParser(chunker, tagger);
    Parse[] results = ParserTool.parseLine("The Minnesota Twins , " +
            "the 1991 World Series Champions , are currently in third place .",
            parser, 1);
    Parse p = results[0];
    Parse[] chunks = p.getChildren();
    assertTrue(chunks.length == 9);
    assertTrue(chunks[0].getType().equals("NP"));
    assertTrue(chunks[0].getHead().toString().equals("Twins"));
    //<end id="openChunkParse"/>
  }
}