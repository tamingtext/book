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

public class Util {
  public static  void validateFileWritable(final File f) {
    File file =   f.getAbsoluteFile();
    File parent = file.getParentFile();
    
    if (file.exists()) {
      if (!file.canWrite()) {
        throw new IllegalArgumentException("Can not overwrite existing file " + file.getAbsolutePath());
      }
    }
    else if (parent.exists()) {
      if (!parent.canWrite()) {
        throw new IllegalArgumentException("Can not write new file to directory " + parent.getAbsolutePath());
      }
    }
    else if (!parent.mkdirs()) {
      throw new IllegalArgumentException("Could not create parent directory " + parent.getAbsolutePath());
    }
  }
}
