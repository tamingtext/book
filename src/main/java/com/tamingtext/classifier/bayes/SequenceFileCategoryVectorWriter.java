package com.tamingtext.classifier.bayes;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.utils.vectors.io.SequenceFileVectorWriter;

public class SequenceFileCategoryVectorWriter extends SequenceFileVectorWriter {
  private final SequenceFile.Writer writer;
  private long recNum = 0;
  
  public SequenceFileCategoryVectorWriter(SequenceFile.Writer writer) {
    super(writer);
    this.writer = writer;
  }

  @Override
  public long write(Iterable<Vector> iterable, long maxDocs) throws IOException {
    for (Vector point : iterable) {
      if (recNum >= maxDocs) {
        break;
      }
      
      
      if (point instanceof NamedVector) {
        NamedVector nv = (NamedVector) point;
      }
      else {
        
      }

      if (point != null) {
        if (point instanceof NamedVector) {
          NamedVector nv = (NamedVector) point;
          writer.append(new Text(nv.getName()), new VectorWritable(point));
        }
        else {
          writer.append(new Text(String.valueOf(recNum++)), new VectorWritable(point));
        }
      }
      
    }
    return recNum;
  }
}
