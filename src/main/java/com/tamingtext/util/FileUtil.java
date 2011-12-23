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
