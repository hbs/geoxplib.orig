package com.geocoord.geo;

import junit.framework.TestCase;

public class GeoHashHelperTestCase extends TestCase {
  
  /**
   * Check with the examples from http://en.wikipedia.org/wiki/Geohash
   */
  public void testToHHCode() {
    long hhcode = GeoHashHelper.toHHCode("ezs42");
    
    assertEquals(-6921889690756841472L, hhcode);
    
    hhcode = GeoHashHelper.toHHCode("u4pruydqqvj");
    assertEquals(-2155044206010576640L, hhcode);
  }
  
  public void testFromHHCode() {
    String geohash = GeoHashHelper.fromHHCode(-6921889690756841472L, 15);
    
    assertEquals("ezs420", geohash);
  }
}
