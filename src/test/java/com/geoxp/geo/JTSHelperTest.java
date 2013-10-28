package com.geoxp.geo;

import org.junit.Test;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;

public class JTSHelperTest {
  @Test
  public void testCoverGeometry() throws Exception {
    
    String WKT = "POLYGON((-4.5 48.0, -4.5 49.0, -4.0 48.0, -4.5 48.0))";
    
    WKTReader reader = new WKTReader();
    
    long nano = System.nanoTime();
    Geometry geometry = null;
    int n = 10000;
    
    for (int i = 0; i < n; i++) {
      geometry = reader.read(WKT);
    }
    nano = System.nanoTime() - nano;
    System.out.println(nano / n / 1000000.0D);
    
    Coverage coverage = null;
    
    nano = System.nanoTime();

    for (int i = 0; i < n; i++) {
      coverage = JTSHelper.coverGeometry(geometry, 14, 14, false);
    }
    nano = System.nanoTime() - nano;
    System.out.println(nano / n / 1000000.0D);
    
    System.out.println(coverage.getCellCount());
    System.out.println(coverage.getResolutions());
    System.out.println(coverage);
  }
}
