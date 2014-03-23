package com.tamingtext.classifier.bayes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.utils.vectors.io.VectorWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closer;

public class SequenceFileCategoryVectorWriter implements VectorWriter {

  private static final Logger log = LoggerFactory.getLogger(SequenceFileCategoryVectorWriter.class);

  private final Map<String, SequenceFile.Writer> 
  writers = new HashMap<String, SequenceFile.Writer>();

  Configuration conf;
  Path outputDir;
  long recNum = 0;
  Closer closer;
  
  public SequenceFileCategoryVectorWriter(String outputDir) {
    this.outputDir = new Path(outputDir);
    this.conf = new Configuration();
    this.closer = Closer.create();
  }

  public SequenceFile.Writer getWriterForCategory(String category) throws IOException {
    SequenceFile.Writer writer = writers.get(category);
    if (writer == null) {
      Path path = new Path(outputDir, category);
      log.info("Opening new writer for category " + category + " at " + path);
      writer = SequenceFile.createWriter(
          FileSystem.get(conf), conf, 
          path, 
          Text.class,
          VectorWritable.class
          );
      writers.put(category, writer);
      closer.register(writer);
    }
    return writer;
  }

  private static final String getCategory(String input) {
    int s = input.indexOf("/");
    int e = input.indexOf("/", s+1);
    if (s >= 0 && e > s) {
      return input.substring(s+1, e);
    }
    else {
      throw new IllegalArgumentException(
          "Could not extract category from '" + input + 
          "' found delimiters at " + s + " and " + e
          );
    }
  }

  @Override
  public long write(Iterable<Vector> iterable, long maxDocs) throws IOException {
    for (Vector point : iterable) {
      if (recNum >= maxDocs) {
        break;
      }

      if (point != null) {
        write(point);
      }
    }
    return recNum;
  }

  @Override
  public void close() throws IOException {
    closer.close();
  }

  @Override
  public long write(Iterable<Vector> iterable) throws IOException {
    return write(iterable, Long.MAX_VALUE);
  }

  @Override
  public void write(Vector vector) throws IOException {
    if (vector instanceof NamedVector) {
      NamedVector nv = (NamedVector) vector;
      String category = getCategory(nv.getName());
      SequenceFile.Writer writer = getWriterForCategory(category);
      writer.append(new Text(nv.getName()), new VectorWritable(vector));
      recNum++;
    }
    else {
      log.warn("Expected a NamedVector, but got a " 
          + vector.getClass().getName() + " skipping...");
    }
  }
}
