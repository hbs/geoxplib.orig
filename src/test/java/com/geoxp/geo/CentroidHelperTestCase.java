package com.geoxp.geo;

import com.geoxp.geo.CentroidHelper;
import com.geoxp.geo.HHCodeHelper;

import junit.framework.TestCase;

public class CentroidHelperTestCase extends TestCase {
  public void testCentroid_TwoPoints() {
    long a = HHCodeHelper.buildHHCode(0, 0, 32);
    long b = HHCodeHelper.buildHHCode(0xffffffffL, 0xffffffffL, 32);
    long c = CentroidHelper.centroid(a, 1, b, 1);
    
    assertEquals(0x3fffffffffffffffL, c);
    
    c = CentroidHelper.centroid(a, 2, b, 1);
    
    assertEquals(0x3333333333333333L, c);

    c = CentroidHelper.centroid(a, 4, b, 1);
    
    assertEquals(0x0f0f0f0f0f0f0f0fL, c);
    
    a = HHCodeHelper.buildHHCode(0, 0x7fffffffL, 32);
    b = HHCodeHelper.buildHHCode(0xffffffffL, 0x7fffffffL, 32);
    
    c = CentroidHelper.centroid(a,1,b,1);
    
    assertEquals(0x3fffffffffffffffL, c);

    a = HHCodeHelper.buildHHCode(0, 0x7fffffffL, 32);
    b = HHCodeHelper.buildHHCode(0x7fffffffL, 0xffffffffL, 32);

    c = CentroidHelper.centroid(a,1,b,1);

    assertEquals(0x4fffffffffffffffL, c);

    a = HHCodeHelper.buildHHCode(0xffffffffL, 0L, 32);
    b = HHCodeHelper.buildHHCode(0, 0xffffffffL, 32);

    c = CentroidHelper.centroid(a,1,b,1);

    assertEquals(0x3fffffffffffffffL, c);
  }
}
