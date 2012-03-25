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
import java.io.IOException;

import opennlp.maxent.io.GISModelReader;
import opennlp.model.AbstractModel;
import opennlp.model.AbstractModelReader;
import opennlp.model.DataReader;
import opennlp.model.GenericModelReader;
import opennlp.perceptron.PerceptronModelReader;

/** A subclass of {@link opennlp.model.GenericModelReader} that 
 *  conserves memory by using delegate readers that call 
 *  <code>intern()</code> on the strings they read from their models.
 *  <p>
 *  The assumption here is that there is enough duplication in the strings in
 *  multiple models being loaded that we will benefit from maintaining a single
 *  copy of each string.
 */  
public class PooledGenericModelReader extends GenericModelReader {
  
  private AbstractModelReader delegateModelReader;
  
  public PooledGenericModelReader(File f) throws IOException {
    super(f);
  }
  
  public PooledGenericModelReader(DataReader dataReader) {
    super(dataReader);
  }

  @Override
  public void checkModelType() throws IOException {
    String modelType = readUTF();
    if (modelType.equals("Perceptron")) {
      delegateModelReader = new LocalPooledPerceptronModelReader(this.dataReader);
    }
    else if (modelType.equals("GIS")) {
      delegateModelReader = new LocalPooledGISModelReader(this.dataReader);
    }
    else {
      throw new IOException("Unknown model format: "+modelType);
    }
  }
  
  @Override
  public AbstractModel constructModel() throws IOException {
    return delegateModelReader.constructModel();
  }
  
  /** Subclass of {@link opennlp.maxent.io.GISModelReader} that conserves
   *  memory by calling <code>intern()</code> on the strings it reads from the
   *  model it loads.
   */
  static class LocalPooledGISModelReader extends GISModelReader {
    public LocalPooledGISModelReader(DataReader reader) {
      super(reader);
    }

    @Override
    public String readUTF() throws IOException {
      return super.readUTF().intern();
    }
  }
  
  /** Subclass of {@link opennlp.perceptron.PerceptronModelReader} that conserves
   *  memory by calling <code>intern()</code> on the strings it reads from the
   *  model it loads.
   */
  static class LocalPooledPerceptronModelReader extends PerceptronModelReader {
    public LocalPooledPerceptronModelReader(DataReader reader) {
      super(reader);
    }

    @Override
    public String readUTF() throws IOException {
      return super.readUTF().intern();
    }
  }
}
