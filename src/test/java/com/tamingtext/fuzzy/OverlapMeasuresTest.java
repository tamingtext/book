package com.tamingtext.fuzzy;

import com.tamingtext.TamingTextTestJ4;
import junit.framework.TestCase;
import org.junit.*;

public class OverlapMeasuresTest extends TamingTextTestJ4 {
  @Test
  public void testJaccard() {
    OverlapMeasures om = new OverlapMeasures();
    assertEquals(om.jaccard("zoo".toCharArray(), "zoo".toCharArray()),1f);
    assertEquals(om.jaccard("zoo".toCharArray(), "zoom".toCharArray()),(float) 2/3);
    assertEquals(om.jaccard("zoot".toCharArray(), "zoomo".toCharArray()),(float) 2/4);
    assertEquals(om.jaccard("zooto".toCharArray(), "zoom".toCharArray()),(float) 2/4);
    assertEquals(om.jaccard("zooto".toCharArray(), "zoomo".toCharArray()),(float) 2/4);
  }

}
