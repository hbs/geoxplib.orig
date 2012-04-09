package com.geoxp.heatmap;

import junit.framework.TestCase;

public class GoogleMapsTestCase extends TestCase {
  public void testIdempotency() {
    
    for (int i = 0; i < 1000; i++) {
      int zoom = (int) (Math.random() * 31);
      long x = (long) (Math.random() * (1 << zoom)) * 256;
      long y = (long) (Math.random() * (1 << zoom)) * 256;

      double[] ll = GoogleMaps.XY2LatLon(x, y, zoom);
      long[] xy = GoogleMaps.LatLon2XY(ll[0], ll[1], zoom);
      
      assertEquals(x, xy[0]);
      assertEquals(y, xy[1]);      
    }
    
    for (int i=0 ; i < 31; i++) {
      double[] ll = GoogleMaps.XY2LatLon(0, 0, i);
      
      System.out.println(ll[0] + "," + ll[1]);
    }
  }  
  
  public void testLatLon2XY() {
    double lat = 48.0;
    double lon = -4.5;
    
    for (int zoom = 0; zoom < 30; zoom++) {
      long[] xy = GoogleMaps.LatLon2XY(lat, lon, zoom);
      double[] ll = GoogleMaps.XY2LatLon(124*256, 88*256, zoom);
      System.out.println(zoom + " " + xy[0]/256 + " " + xy[1]/256 + " " + ll[0] + " " + ll[1]);
    }
    
  }
}
