package com.tamingtext.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

public class MemoryStatus {

  static String[] units = {
      "Bytes", "KBytes", "MBytes", "GBytes"
  };
  
  List<MemoryPoolMXBean> memoryBeans;
  
  public MemoryStatus() {
    memoryBeans = ManagementFactory.getMemoryPoolMXBeans();
  }
  
  public void dumpMemory(String title) {
    System.err.println("----------" + title + "----------");
    double total = 0;
    for (MemoryPoolMXBean m: memoryBeans) {
      MemoryUsage u = m.getUsage();
      double used = u.getUsed();
      total += used;
      System.err.println(m.getName() + " " + toMemoryString(used));
    }
    System.err.println("Total " + toMemoryString(total));
    System.err.println("---------------------------------");
  }
  
  private String toMemoryString(double bytes) {
    int pos = 0;
    while (bytes > 1024) {
      pos++;
      bytes = bytes / 1024;
    }
    return String.format("%2.2f", bytes) + " " + units[pos];
  }
  
  public static void main(String[] args) {
    MemoryStatus stat = new MemoryStatus();
    stat.dumpMemory("before");
    Object[] o = new Object[100000];
    for (int i=0; i < o.length; i++) {
      o[i] = new String("booga");
    }
    stat.dumpMemory("after");
  }
}
