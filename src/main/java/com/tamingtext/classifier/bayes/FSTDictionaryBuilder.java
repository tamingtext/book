package com.tamingtext.classifier.bayes;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.ToolRunner;
import org.apache.lucene.store.OutputStreamDataOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FST.INPUT_TYPE;
import org.apache.lucene.util.fst.Util;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.PathType;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileDirIterable;
import org.apache.mahout.math.VectorWritable;
import org.slf4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.io.Closer;

/**
 * Builds a Lucene FST from a dictionary of terms created as a part of the
 * Mahout vectorization process.
 * 
 * This FST will map terms to dictionary ID's and Frequencies.
 */
public class FSTDictionaryBuilder {

  public static final Logger logger = org.slf4j.LoggerFactory
      .getLogger(FSTDictionaryBuilder.class);

  @Parameter(names = { "-i", "--input" }, description = "Input File", required = true)
  private String inputFile;

  @Parameter(names = { "-c", "--count-vectors" }, description = "Count Vectors", required = false)
  private boolean countVectors;
  
  @Parameter(names = { "-d", "--document-count" }, description = "Document Count", required = false)
  private long documentCount = -1;
  
  @Parameter(names = { "-v", "--vector-dir" }, description = "Vector Directory", required = false)
  private String vectorDir;
  
  @Parameter(names = { "-o", "--output" }, description = "Output FST File", required = true)
  private String fstOutputFile;

  @Parameter(names = "--help", help = true)
  private boolean help;

  public int run(String[] args) throws IOException {
    if (!parseArgs(args))
      return -1;

    Preconditions.checkNotNull("Expected an input file to be specified",
        inputFile);

    Preconditions.checkArgument(vectorDir != null || documentCount > -1, 
        "Expected a document count or a vector directory to be specified");

    Preconditions.checkNotNull("Expected an output file to be specified",
        fstOutputFile);

    File f = new File(fstOutputFile);
    File p = f.getParentFile();
    p.mkdirs();
    if (!p.isDirectory()) {
      throw new FileNotFoundException("Can not write to directory (can not create, or already exists and is not a directory): " + p.getAbsolutePath());
    }

    if (vectorDir != null) {
      logger.info("Counting vectors found in " + vectorDir);
      // we need to derive the document count from the number of vectors,
      // this is a pretty ham-handed approach.
      Configuration conf = new Configuration();
      Path vp = new Path(vectorDir);
      SequenceFileDirIterable<Text, VectorWritable> it = new SequenceFileDirIterable<Text, VectorWritable>(vp, PathType.LIST, conf);
      documentCount = 0;
      Iterator<Pair<Text, VectorWritable>> itr = it.iterator();
      while (itr.hasNext()) {
        itr.next();
        documentCount++;
      }
      logger.info("Completed counting vectors, found: " + documentCount);
    }
    
    Builder<Object> builder = new Builder<Object>(INPUT_TYPE.BYTE1, FSTDictionary.OUTPUTS);
    BytesRef scratchBytes = new BytesRef();
    IntsRef scratchInts = new IntsRef();
    Closer c = Closer.create();

    try {
      Reader r = new BufferedReader(new FileReader(inputFile));
      c.register(c);

      OutputStreamDataOutput out = new OutputStreamDataOutput(
          new FileOutputStream(fstOutputFile));
      c.register(out);

      scratchBytes.copyChars(FSTDictionary.NUM_DOCS);
      Util.toIntsRef(scratchBytes, scratchInts);
      builder.add(scratchInts, Long.valueOf(documentCount));
      
      for (FSTDictionary.Entry entry : new TextDictionaryIterable(r)) {
        scratchBytes.copyChars(entry.term);
        Util.toIntsRef(scratchBytes, scratchInts);
        builder.add(scratchInts, Long.valueOf(entry.index));
        builder.add(scratchInts, Long.valueOf(entry.df));
      }

      FST<Object> fst = builder.finish();
      fst.save(out);

      if (logger.isInfoEnabled()) {
        logger.info("Saved fst to: " + fstOutputFile);
        logger.info("Fst size in bytes: " + fst.sizeInBytes());
        logger.info("Fst arc count: " + fst.getArcCount() + ", "
            + fst.getArcWithOutputCount() + " arcs with output");
        logger.info("Fst node count: " + fst.getNodeCount());
      }
    } catch (IOException ex) {
      throw new IOException(
          "Unable to read dictionary file " + ex.getMessage(), ex);
    } finally {
      c.close();
    }

    return 0;
  }

  public boolean parseArgs(String[] args) {
    JCommander jc = new JCommander(this);
    try {
      jc.parse(args);
      if (help) {
        usage(null, jc);
        return false;
      }
    } catch (ParameterException ex) {
      usage(ex.getMessage(), jc);
      return false;
    }
    return true;
  }

  public static void usage(String message, JCommander jc) {
    if (message != null)
      System.err.println(message);
    ToolRunner.printGenericCommandUsage(System.err);
    StringBuilder out = new StringBuilder();
    jc.usage(out);
    System.err.println(out);
  }

  public static void main(String[] args) throws Exception {
    FSTDictionaryBuilder b = new FSTDictionaryBuilder();
    b.run(args);
  }

  /** Iterable for the text dictionary format */
  public static class TextDictionaryIterable implements Closeable,
  Iterable<FSTDictionary.Entry> {
    final BufferedReader in;

    public TextDictionaryIterable(Reader r) {
      Preconditions.checkNotNull(r, "reader argument was null");

      if (r instanceof BufferedReader) {
        in = (BufferedReader) r;
      } else {
        in = new BufferedReader(r);
      }
    }

    @Override
    public Iterator<FSTDictionary.Entry> iterator() {
      try {
        return new TextDictionaryIterator(in);
      } catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }

    @Override
    public void close() throws IOException {
      if (in != null) {
        in.close();
      }
    }
  }

  /** Iterator for the Text Dictionary format */
  public static class TextDictionaryIterator implements
  Iterator<FSTDictionary.Entry> {
    public static final Logger logger = org.slf4j.LoggerFactory
        .getLogger(TextDictionaryIterator.class);

    final BufferedReader in;
    final FSTDictionary.Entry  entry  = new FSTDictionary.Entry();
    final FSTDictionary.Header header = new FSTDictionary.Header();
    FSTDictionary.Entry current;
    int currentLine;

    public TextDictionaryIterator(Reader r) throws IOException {
      if (r instanceof BufferedReader) {
        in = (BufferedReader) r;
      } else {
        in = new BufferedReader(r);
      }

      try {
        header.termCount = Long.parseLong(in.readLine());
      } catch (NumberFormatException ex) {
        throw new IOException("Dictionary does not stard with a term count");
      }

      header.columns = in.readLine();
      currentLine = 2;
    }

    @Override
    public boolean hasNext() {
      if (current != null)
        return true;
      try {
        return readNextEntry();
      } catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }

    @Override
    public FSTDictionary.Entry next() {
      if (current == null && !hasNext()) {
        throw new NoSuchElementException();
      }

      Preconditions.checkNotNull(current);
      current = null;
      return entry;
    }

    public boolean readNextEntry() throws IOException {
      while (true) {
        // read until we successfully read and parse an entire line, or we reach
        // the end of the file.
        String line = in.readLine();
        if (line == null)
          return false;
        currentLine++;

        int pos = 0;
        for (String col : Splitter.on("\t").split(line)) {
          try {
            col = col.trim();
            switch (pos) {
            case 0:
              entry.term = col;
              break;
            case 1:
              entry.df = Integer.parseInt(col);
              break;
            case 2:
              entry.index = Integer.parseInt(col);
              break;
            default:
              logger.warn("Extra column " + pos + " observed at line: " + line
                  + ", ignoring extra column.");
            }
          } catch (NumberFormatException ex) {
            logger.warn("Line " + line + " had invalid value in column " + pos
                + " ignoring line");
            continue;
          } finally {
            pos++;
          }
        }
        break; // successfully read and parsed a line.
      }
      current = entry; // queue up the entry.
      return true;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }
}
