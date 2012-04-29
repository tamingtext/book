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

package com.tamingtext.mahout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.tamingtext.TamingTextTestJ4;
import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.utils.vectors.io.SequenceFileVectorWriter;
import org.apache.mahout.utils.vectors.io.VectorWriter;
import org.junit.*;


/**
 *
 *
 **/
public class VectorExamplesTest extends TamingTextTestJ4 {
  @Test
  public void testProgrammatic() throws Exception {
    //<start id="vec.examples.programmatic"/>
    double[] vals = new double[]{0.3, 1.8, 200.228};
    Vector dense = new DenseVector(vals);//<co id="vec.exam.dense"/>
    assertTrue(dense.size() == 3);
    Vector sparseSame = new SequentialAccessSparseVector(3);//<co id="vec.exam.sparse.same"/>
    Vector sparse = new SequentialAccessSparseVector(3000);//<co id="vec.exam.sparse"/>
    for (int i = 0; i < vals.length; i++) {//<co id="vec.exam.assign.sparse"/>
      sparseSame.set(i, vals[i]);
      sparse.set(i, vals[i]);
    }
    assertFalse(dense.equals(sparse));//<co id="vec.exam.notequals.d.s"/>
    assertEquals(dense, sparseSame);//<co id="vec.exam.equals.d.s"/>
    assertFalse(sparse.equals(sparseSame));
    /*
<calloutlist>
    <callout arearefs="vec.exam.dense"><para>Create a <classname>DenseVector</classname> with a label of "my-dense" and 3 values.  The cardinality of this vector is 3 </para></callout>
    <callout arearefs="vec.exam.sparse.same"><para>Create a <classname>SparseVector</classname> with a label of my-sparse-same that has cardinality of 3</para></callout>

    <callout arearefs="vec.exam.sparse"><para>Create a <classname>SparseVector</classname> with a label of my-sparse and a cardinality of 3000.</para></callout>
    <callout arearefs="vec.exam.assign.sparse"><para>Set the values to the first 3 items in the sparse vectors.</para></callout>
    <callout arearefs="vec.exam.notequals.d.s"><para>The dense and the sparse <classname>Vector</classname>s are not equal because they have different cardinality.</para></callout>
    <callout arearefs="vec.exam.equals.d.s"><para>The dense and sparseSame <classname>Vector</classname>s are equal because they have the same values and cardinality</para></callout>

</calloutlist>
*/
    //<end id="vec.examples.programmatic"/>
    //<start id="vec.examples.seq.file"/>
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    File tmpLoc = new File(tmpDir, "sfvwt");
    tmpLoc.mkdirs();
    File tmpFile = File.createTempFile("sfvwt", ".dat", tmpLoc);

    Path path = new Path(tmpFile.getAbsolutePath());
    Configuration conf = new Configuration();//<co id="vec.examples.seq.conf"/>
    FileSystem fs = FileSystem.get(conf);
    SequenceFile.Writer seqWriter = SequenceFile.createWriter(fs, conf, path,
            LongWritable.class, VectorWritable.class);//<co id="vec.examples.seq.writer"/>
    VectorWriter vecWriter = new SequenceFileVectorWriter(seqWriter);//<co id="vec.examples.seq.vecwriter"/>
    List<Vector> vectors = new ArrayList<Vector>();
    vectors.add(sparse);
    vectors.add(sparseSame);
    vecWriter.write(vectors);//<co id="vec.examples.seq.write"/>
    vecWriter.close();
    /*
<calloutlist>
    <callout arearefs="vec.examples.seq.conf"><para>Create a <classname>Configuration</classname> for Hadoop</para></callout>
    <callout arearefs="vec.examples.seq.writer"><para>Create a Hadoop <classname>SequenceFile.Writer</classname> to handle the job of physically writing out the vectors to a file in HDFS</para></callout>
    <callout arearefs="vec.examples.seq.vecwriter"><para>A <classname>VectorWriter</classname> processes the <classname>Vector</classname>s and invokes the underlying write methods on the <classname>SequenceFile.Writer</classname></para></callout>
    <callout arearefs="vec.examples.seq.write"><para>Do the work of writing out the files</para></callout>

</calloutlist>
*/
    //<end id="vec.examples.seq.file"/>
  }
}
