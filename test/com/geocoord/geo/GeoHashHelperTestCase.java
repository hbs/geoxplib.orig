package com.geocoord.geo;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

public class GeoHashHelperTestCase extends TestCase {
  
  /**
   * Check with the examples from http://en.wikipedia.org/wiki/Geohash
   */
  @Test
  public void testToHHCode() {
    long hhcode = GeoHashHelper.toHHCode("ezs42");
    
    Assert.assertEquals(-6921889690756841472L, hhcode);
    
    hhcode = GeoHashHelper.toHHCode("u4pruydqqvj");
    Assert.assertEquals(-2155044206010576640L, hhcode);
    
    hhcode = GeoHashHelper.toHHCode("gbsc07bczzpf");
    Assert.assertEquals("b570702e87ffd5d0", HHCodeHelper.toString(hhcode));
  }
  
  public void testFromHHCode() {
    String geohash = GeoHashHelper.fromHHCode(-6921889690756841472L, 15);    
    Assert.assertEquals("ezs420", geohash);
    
    geohash = GeoHashHelper.fromHHCode(0xb570702e87ffd5d0L, 32);
    Assert.assertEquals("gbsc07bczzpf", geohash);
  }
}
