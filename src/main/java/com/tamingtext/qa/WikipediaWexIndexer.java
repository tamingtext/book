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

package com.tamingtext.qa;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikipediaWexIndexer {
  private transient static Logger log = LoggerFactory.getLogger(WikipediaWexIndexer.class);
  
  private SolrServer server;
  public static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr";
  
  public WikipediaWexIndexer() throws MalformedURLException {
    this.server = new CommonsHttpSolrServer(DEFAULT_SOLR_URL);
  }
  
  public WikipediaWexIndexer(SolrServer server) throws MalformedURLException {
    this.server = server;
  }
  
  public int index(File wikipediaWEX) throws Exception {
    return index(wikipediaWEX, Integer.MAX_VALUE, 1000);
  }
  
  public int index(File wikipediaWEX, int numDocs, int batchSize)
      throws Exception {
    int result = 0;
    if (wikipediaWEX != null && wikipediaWEX.isFile()) {
      WexWikiContentSource contentSource = new WexWikiContentSource();
      Properties properties = new Properties();
      // fileName = config.get("docs.file", null);
      String filePath = wikipediaWEX.getAbsolutePath();
      properties.setProperty("docs.file", filePath);
      properties.setProperty("doc.maker.forever", "false");
      contentSource.setConfig(new Config(properties));
      contentSource.resetInputs();
      // docMaker.openFile();
      List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>(1000);
      int i = 0;
      SolrInputDocument sDoc = null;
      long start = System.currentTimeMillis();
      try {
        DocData docData = new DocData();
        
        while ((docData = contentSource.getNextDocData(docData)) != null
            && i < numDocs) {
          int mod = i % batchSize;
          
          sDoc = new SolrInputDocument();
          docs.add(sDoc);
          sDoc.addField("file", filePath + "_" + i);
          
          sDoc.addField("docid", String.valueOf(docData.getID()));
          sDoc.addField("body", docData.getBody());
          sDoc.addField("doctitle", docData.getTitle());
          sDoc.addField("name_s", docData.getName());
          
          String[] categories = docData.getProps().getProperty("category")
              .split(";;");
          
          for (String c : categories) {
            sDoc.addField("category", c);
          }
          
          if (mod == batchSize - 1) {
            log.info("Sending: " + docs.size() + " docs"
                + " total sent for this file: " + i);
            server.add(docs);
            docs.clear();
          }
          i++;
        }
      } catch (NoMoreDataException e) {

      }
      long finish = System.currentTimeMillis();
      if (log.isInfoEnabled()) {
        log.info("Indexing took " + (finish - start) + " ms");
      }
      if (docs.size() > 0) {
        server.add(docs);
      }
      result = i + docs.size();
      server.commit();
      server.optimize();
    } else {
      System.out.println("Can't find file: " + wikipediaWEX);
    }
    return result;
  }
  
  public static void main(String[] args) throws Exception {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();
    
    Option wikipediaFileOpt = obuilder
        .withLongName("wikiFile")
        .withRequired(true)
        .withArgument(
            abuilder.withName("wikiFile").withMinimum(1).withMaximum(1)
                .create())
        .withDescription(
            "The path to the wikipedia dump file. "
                + "May be a directory containing wikipedia dump files. "
                + "If a directory is specified, files starting with the prefix "
                + "freebase-segment- are used.").withShortName("w").create();
    
    Option numDocsOpt = obuilder
        .withLongName("numDocs")
        .withRequired(false)
        .withArgument(
            abuilder.withName("numDocs").withMinimum(1).withMaximum(1).create())
        .withDescription("The number of docs to index").withShortName("n")
        .create();
    
    Option solrURLOpt = obuilder
        .withLongName("solrURL")
        .withRequired(false)
        .withArgument(
            abuilder.withName("solrURL").withMinimum(1).withMaximum(1).create())
        .withDescription("The URL where Solr lives").withShortName("s")
        .create();
    
    Option solrBatchOpt = obuilder
        .withLongName("batch")
        .withRequired(false)
        .withArgument(
            abuilder.withName("batch").withMinimum(1).withMaximum(1).create())
        .withDescription("The number of docs to include in each indexing batch")
        .withShortName("b").create();
    
    Option helpOpt = obuilder.withLongName("help").withDescription(
        "Print out help").withShortName("h").create();
    
    Group group = gbuilder.withName("Options").withOption(wikipediaFileOpt)
        .withOption(numDocsOpt).withOption(solrURLOpt).withOption(solrBatchOpt)
        .withOption(helpOpt).create();
    
    Parser parser = new Parser();
    parser.setGroup(group);
    
    try {
      CommandLine cmdLine = parser.parse(args);
      
      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return;
      }
      
      File file;
      file = new File(cmdLine.getValue(wikipediaFileOpt).toString());
      File[] dumpFiles;
      if (file.isDirectory()) {
        dumpFiles = file.listFiles(new FilenameFilter() {
          public boolean accept(File file, String s) {
            return s.startsWith("freebase-segment-");
          }
        });
      } else {
        dumpFiles = new File[] {file};
      }
      
      int numDocs = Integer.MAX_VALUE;
      if (cmdLine.hasOption(numDocsOpt)) {
        numDocs = Integer.parseInt(cmdLine.getValue(numDocsOpt).toString());
      }
      String url = DEFAULT_SOLR_URL;
      if (cmdLine.hasOption(solrURLOpt)) {
        url = cmdLine.getValue(solrURLOpt).toString();
      }
      int batch = 100;
      if (cmdLine.hasOption(solrBatchOpt)) {
        batch = Integer.parseInt(cmdLine.getValue(solrBatchOpt).toString());
      }
      WikipediaWexIndexer indexer = new WikipediaWexIndexer(
          new CommonsHttpSolrServer(url));
      int total = 0;
      for (int i = 0; i < dumpFiles.length && total < numDocs; i++) {
        File dumpFile = dumpFiles[i];
        log.info("Indexing: " + file + " Num files to index: "
            + (numDocs - total));
        long start = System.currentTimeMillis();
        int totalFile = indexer.index(dumpFile, numDocs - total, batch);
        long finish = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
          log
              .info("Indexing " + dumpFile + " took " + (finish - start)
                  + " ms");
        }
        total += totalFile;
        log.info("Done Indexing: " + file + ". Indexed " + totalFile
            + " docs for that file and " + total + " overall.");
        
      }
      log.info("Indexed " + total + " docs overall.");
    } catch (OptionException e) {
      log.error("Exception", e);
      CommandLineUtil.printHelp(group);
      return;
    }
  }
}
