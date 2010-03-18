package com.geocoord.geo;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CoverageTestCase extends TestCase {
  public void testOptimize_Thresholds() {
    Coverage coverage = new Coverage();
    coverage.addCell(32, 0x1L);
    
    //
    // Optimize coverage with a clustering threshold of 1 for resolution 32, i.e.
    // if one cell of resolution 32 is found, replace it by its enclosing cell at
    // resolution 30.
    //
    
    coverage.optimize(0x00000000000000001L);
    
    //
    // The optimized coverage should have 1 cell at resolution 30 (0) and
    // none at resolution 32.
    //
    
    assertEquals("000000000000000", coverage.toString());
  }

  public void testOptimize_Threshold() {
    
    Coverage coverage = new Coverage();
    
    coverage.addCell(4, 0xa000000000000000L);
    coverage.addCell(4, 0xa100000000000000L);
    coverage.addCell(4, 0xa200000000000000L);
    coverage.addCell(4, 0xa300000000000000L);
    
    int hashcode = coverage.hashCode();
    
    // Threshold of 5, the coverage should not be modified.
    coverage.optimize(0x0500000000000000L);
    assertEquals(hashcode, coverage.hashCode());

    // Threshold of 4, the coverage should be modified
    coverage.optimize(0x0400000000000000L);

    assertEquals("a", coverage.toString());
  }

  public void testOptimize_CleanUp() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(2,0xa000000000000000L);
    coverage.addCell(6,0xa000000000000000L);
    coverage.addCell(6,0xa010000000000000L);
    coverage.addCell(6,0xa020000000000000L);
    coverage.addCell(6,0xa030000000000000L);

    // Threshold of 5, the coverage should not be modified.
    coverage.optimize(0x0500000000000000L);

    assertEquals("a", coverage.toString());
  }

  public void testArea() {
    Coverage coverage = new Coverage();
    
    assertEquals(0L, coverage.area());
    
    coverage.addCell(2, 0x0L);
    
    assertEquals(0x0800000000000000L, coverage.area());

    coverage.addCell(2, 0x1000000000000000L);
    coverage.addCell(2, 0x2000000000000000L);
    coverage.addCell(2, 0x3000000000000000L);
    coverage.addCell(2, 0x4000000000000000L);
    coverage.addCell(2, 0x5000000000000000L);
    coverage.addCell(2, 0x6000000000000000L);
    coverage.addCell(2, 0x7000000000000000L);
    coverage.addCell(2, 0x8000000000000000L);
    
    assertEquals(0x4800000000000000L, coverage.area());
    
    coverage.addCell(32, 0x8000000000000000L);
    assertEquals(0x4800000000000000L, coverage.area());

    coverage.addCell(30, 0x8000000000000000L);
    assertEquals(0x4800000000000008L, coverage.area());

    coverage.addCell(28, 0x8000000000000000L);
    assertEquals(0x4800000000000088L, coverage.area());

    coverage.addCell(26, 0x8000000000000000L);
    assertEquals(0x4800000000000888L, coverage.area());

    coverage.addCell(24, 0x8000000000000000L);
    assertEquals(0x4800000000008888L, coverage.area());

    coverage.addCell(22, 0x8000000000000000L);
    assertEquals(0x4800000000088888L, coverage.area());

    coverage.addCell(20, 0x8000000000000000L);
    assertEquals(0x4800000000888888L, coverage.area());

    coverage.addCell(18, 0x8000000000000000L);
    assertEquals(0x4800000008888888L, coverage.area());

    coverage.addCell(16, 0x8000000000000000L);
    assertEquals(0x4800000088888888L, coverage.area());

    coverage.addCell(14, 0x8000000000000000L);
    assertEquals(0x4800000888888888L, coverage.area());

    coverage.addCell(12, 0x8000000000000000L);
    assertEquals(0x4800008888888888L, coverage.area());

    coverage.addCell(10, 0x8000000000000000L);
    assertEquals(0x4800088888888888L, coverage.area());

    coverage.addCell(8, 0x8000000000000000L);
    assertEquals(0x4800888888888888L, coverage.area());

    coverage.addCell(6, 0x8000000000000000L);
    assertEquals(0x4808888888888888L, coverage.area());

    coverage.addCell(4, 0x8000000000000000L);
    assertEquals(0x4888888888888888L, coverage.area());
  }
  
  public void testToString() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(6, 0x0L);
    
    coverage.addCell(2, 0x1000000000000000L);
    coverage.addCell(2, 0x2000000000000000L);
    coverage.addCell(2, 0x3000000000000000L);
    coverage.addCell(2, 0x4000000000000000L);
    coverage.addCell(2, 0x5000000000000000L);
    coverage.addCell(2, 0x6000000000000000L);
    coverage.addCell(2, 0x7000000000000000L);
    coverage.addCell(2, 0x8000000000000000L);
    
    coverage.addCell(32, 0x8000000000000000L);
    coverage.addCell(30, 0x8000000000000000L);
    coverage.addCell(28, 0x8000000000000000L);
    coverage.addCell(26, 0x8000000000000000L);
    coverage.addCell(24, 0x8000000000000000L);
    coverage.addCell(22, 0x8000000000000000L);
    coverage.addCell(20, 0x8000000000000000L);
    coverage.addCell(18, 0x8000000000000000L);
    coverage.addCell(16, 0x8000000000000000L);
    coverage.addCell(14, 0x8000000000000000L);
    coverage.addCell(12, 0x8000000000000000L);
    coverage.addCell(10, 0x8000000000000000L);
    coverage.addCell(8, 0x8000000000000000L);
    coverage.addCell(6, 0x8000000000000000L);
    coverage.addCell(4, 0x8000000000000000L);
    
    Assert.assertEquals("8 1 2 3 4 5 6 7 80 800 000 8000 80000 800000 8000000 80000000 800000000 8000000000 80000000000 800000000000 8000000000000 80000000000000 800000000000000 8000000000000000", coverage.toString());
  }
  
  public void testNormalize_Expand() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(2, 0xf000000000000000L);
    
    coverage.normalize(4);
    
    assertEquals(1, coverage.getResolutions().size());
    assertTrue(coverage.getResolutions().contains(4));
    assertEquals("fe fc fa f8 f6 f4 f2 f0 ff fd fb f9 f7 f5 f3 f1", coverage.toString());
  }

  public void testNormalize_Compact() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(2, 0xf000000000000000L);
    coverage.addCell(32,0xf000000000000001L);
    coverage.addCell(30,0xf000000000000010L);
    
    coverage.normalize(4);
    
    assertEquals(1, coverage.getResolutions().size());
    assertTrue(coverage.getResolutions().contains(4));
    assertEquals("fe fc fa f8 f6 f4 f2 f0 ff fd fb f9 f7 f5 f3 f1", coverage.toString());
  }

  public void testClone() {
    Coverage coverage = new Coverage();
    coverage.addCell(2,0);
    coverage.normalize(6);
    
    Coverage clone = coverage.deepCopy();
    
    assertEquals(clone.toString(),coverage.toString());
    coverage.removeCell(6, 0L);
    assertNotSame(clone.toString(),coverage.toString());
  }
}
