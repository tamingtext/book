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
