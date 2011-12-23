package com.tamingtext.opennlp;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

import java.io.File;
import java.io.IOException;

import com.tamingtext.TamingTextTestJ4;
import opennlp.tools.lang.english.ParserTagger;
import opennlp.tools.lang.english.TreebankChunker;
import opennlp.tools.lang.english.TreebankParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;

import com.tamingtext.qa.ChunkParser;
import org.junit.*;

public class ChunkParserTest extends TamingTextTestJ4 {

  @Test
  public void test() throws IOException {

    File parserDir = getChunkerDir();
    //<start id="openChunkParse"/>
    TreebankChunker chunker = new TreebankChunker(parserDir.getAbsolutePath()
            + File.separator + "EnglishChunk.bin.gz");
    File posDir = getPOSDir();
    ParserTagger tagger =  new ParserTagger(posDir.getAbsolutePath() + File.separator + "tag.bin.gz",
            posDir.getAbsolutePath() + File.separator + "tagdict", true);
    Parser parser = new ChunkParser(chunker, tagger);
    Parse[] results = TreebankParser.parseLine("The Minnesota Twins , " +
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