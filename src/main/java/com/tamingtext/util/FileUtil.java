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

package com.tamingtext.util;

import java.io.File;
import java.io.FileFilter;

public class FileUtil {

  public static File[] buildFileList(File f) {
    if (!f.isDirectory()) {
      throw new IllegalArgumentException(f + " is not a directory or does not exit");
    }
    
    // list training data.
    File[] inputFiles = f.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        if (pathname.isFile()) {
          String name = pathname.getName();
          if (name.startsWith(".") || name.endsWith(".crc")) {
            return false;
          }
          return true; // a file.
        }
        
        return false; // not a file.
      }
    });
    return inputFiles;
  }

}
