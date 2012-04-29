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


import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.benchmark.byTask.feeds.DocData;
import org.apache.lucene.benchmark.byTask.feeds.EnwikiContentSource;
import org.apache.lucene.benchmark.byTask.feeds.NoMoreDataException;
import org.apache.lucene.benchmark.byTask.utils.Config;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;


/**
 * Take in the Lucene Wikipedia benchmark docs and index them in Solr
 */
public class WikipediaIndexer {
  private transient static Log log = LogFactory.getLog(WikipediaIndexer.class);
  private static final String LINE_SEP = System.getProperty("line.separator");

  private SolrServer server;
  public static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr";

  public WikipediaIndexer() throws MalformedURLException {
    server = new CommonsHttpSolrServer(DEFAULT_SOLR_URL);
  }

  public WikipediaIndexer(SolrServer server) throws MalformedURLException {

    this.server = server;
  }

  private static SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
  private static SimpleDateFormat solrFormatter =
          new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
  public static TimeZone UTC = TimeZone.getTimeZone("UTC");

  public int index(File wikipediaXML) throws Exception {
    return index(wikipediaXML, Integer.MAX_VALUE, 1000);
  }

  public int index(File wikipediaXML, int numDocs, int batchSize) throws Exception {
    int result = 0;
    if (wikipediaXML != null && wikipediaXML.exists()) {
      EnwikiContentSource contentSource = new EnwikiContentSource();
      Properties properties = new Properties();
      //fileName = config.get("docs.file", null);
      String filePath = wikipediaXML.getAbsolutePath();
      properties.setProperty("docs.file", filePath);
      properties.setProperty("doc.maker.forever", "false");
      contentSource.setConfig(new Config(properties));
      contentSource.resetInputs();
      //docMaker.openFile();
      List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>(1000);
      int i = 0;
      SolrInputDocument sDoc = null;
      long start = System.currentTimeMillis();
      try {
        DocData docData = new DocData();

        while ((docData = contentSource.getNextDocData(docData)) != null && i < numDocs) {
          int mod = i % batchSize;

          sDoc = new SolrInputDocument();
          docs.add(sDoc);
          sDoc.addField("file", filePath + "_" + i);

          sDoc.addField("docid", String.valueOf(i));
          sDoc.addField("body", docData.getBody());
          sDoc.addField("doctitle", docData.getTitle());
          sDoc.addField("name_s", docData.getName());


          if (mod == batchSize - 1) {
            log.info("Sending: " + docs.size() + " docs" + " total sent for this file: " + i);
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
      System.out.println("Can't find file: " + wikipediaXML);
    }
    return result;
  }

  public static void main(String[] args) throws Exception {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();

    Option wikipediaFileOpt = obuilder.withLongName("wikiFile").withRequired(true).withArgument(
            abuilder.withName("wikiFile").withMinimum(1).withMaximum(1).create()).
            withDescription("The path to the wikipedia dump file.  Maybe a directory containing wikipedia dump files." +
                    "  If a directory is specified, only .xml files are used.").withShortName("w").create();

    Option numDocsOpt = obuilder.withLongName("numDocs").withRequired(false).withArgument(
            abuilder.withName("numDocs").withMinimum(1).withMaximum(1).create()).
            withDescription("The number of docs to index").withShortName("n").create();

    Option solrURLOpt = obuilder.withLongName("solrURL").withRequired(false).withArgument(
            abuilder.withName("solrURL").withMinimum(1).withMaximum(1).create()).
            withDescription("The URL where Solr lives").withShortName("s").create();

    Option solrBatchOpt = obuilder.withLongName("batch").withRequired(false).withArgument(
            abuilder.withName("batch").withMinimum(1).withMaximum(1).create()).
            withDescription("The number of docs to include in each indexing batch").withShortName("b").create();

    Group group = gbuilder.withName("Options").withOption(wikipediaFileOpt).withOption(numDocsOpt).withOption(solrURLOpt).withOption(solrBatchOpt).create();

    Parser parser = new Parser();
    parser.setGroup(group);
    CommandLine cmdLine = parser.parse(args);

    File file;
    file = new File(cmdLine.getValue(wikipediaFileOpt).toString());
    File[] dumpFiles;
    if (file.isDirectory()) {
      dumpFiles = file.listFiles(new FilenameFilter() {
        public boolean accept(File file, String s) {
          return s.endsWith(".xml");
        }
      });
    } else {
      dumpFiles = new File[]{file};
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
    WikipediaIndexer indexer = new WikipediaIndexer(new CommonsHttpSolrServer(url));
    int total = 0;
    for (int i = 0; i < dumpFiles.length && total < numDocs; i++) {
      File dumpFile = dumpFiles[i];
      log.info("Indexing: " + file + " Num files to index: " + (numDocs - total));
      long start = System.currentTimeMillis();
      int totalFile = indexer.index(dumpFile, numDocs - total, batch);
      long finish = System.currentTimeMillis();
      if (log.isInfoEnabled()) {
        log.info("Indexing " + dumpFile + " took " + (finish - start) + " ms");
      }
      total += totalFile;
      log.info("Done Indexing: " + file + ". Indexed " + totalFile + " docs for that file and " + total + " overall.");

    }
    log.info("Indexed " + total + " docs overall.");
  }

}


