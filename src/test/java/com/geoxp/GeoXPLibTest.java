//
//   GeoXP Lib, library for efficient geo data manipulation
//
//   Copyright 2020-      SenX S.A.S.
//   Copyright 2019-2020  iroise.net S.A.S.
//   Copyright 1999-2019  Mathias Herberts
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package com.geoxp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import com.geoxp.GeoXPLib.GeoXPShape;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class GeoXPLibTest {
  @Test
  public void testBytesFromGeoXPPoint() {
    long hhcode = 0x1234567897abcdefL;
    
    byte[] bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 2);
    Assert.assertEquals(1, bytes.length);
    Assert.assertEquals(0x10, bytes[0]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 4);
    Assert.assertEquals(1, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 6);
    Assert.assertEquals(2, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x30, bytes[1]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 8);
    Assert.assertEquals(2, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 10);
    Assert.assertEquals(3, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x50, bytes[2]);    

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 12);
    Assert.assertEquals(3, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 14);
    Assert.assertEquals(4, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x70, bytes[3]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 16);
    Assert.assertEquals(4, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 18);
    Assert.assertEquals(5, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x90, bytes[4]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 20);
    Assert.assertEquals(5, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 22);
    Assert.assertEquals(6, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xa0, bytes[5]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 24);
    Assert.assertEquals(6, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 26);
    Assert.assertEquals(7, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);
    Assert.assertEquals((byte) 0xc0, bytes[6]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 28);
    Assert.assertEquals(7, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);
    Assert.assertEquals((byte) 0xcd, bytes[6]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 30);
    Assert.assertEquals(8, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);
    Assert.assertEquals((byte) 0xcd, bytes[6]);
    Assert.assertEquals((byte) 0xe0, bytes[7]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 32);
    Assert.assertEquals(8, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);
    Assert.assertEquals((byte) 0xcd, bytes[6]);
    Assert.assertEquals((byte) 0xef, bytes[7]);

    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 0));
    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 1));
    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 3));
    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 33));
    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 34));
  }
  
  @Test
  public void testRegexp() throws Exception {
    
    //
    // Read WKT
    //
    
    WKTReader reader = new WKTReader();    
    Geometry geometry = null;
    
    String wkt = "POLYGON ((-4.5 48, -4.25 48.5, -4 48, -4.5 48))";
    
    geometry = reader.read(wkt.toString());
    
    //
    // Convert Geometry to a GeoXPShape
    //
    
    long nano = System.nanoTime();
    GeoXPShape shape = GeoXPLib.toGeoXPShape(geometry, 0.01, false);
    shape = GeoXPLib.limit(shape, 1000);
    
    //shape = new GeoXPShape();
    //shape.geocells = new long[100];
    //for (int i = 0; i < 100; i++) {
    //  shape.geocells[i] = 0x1a00000000000000L | i;
    //}
    
    String regexp = GeoXPLib.toRegexp(shape);
    
    Pattern p = Pattern.compile(regexp);
    System.out.println(regexp);
    System.out.println(p.matcher("b57070707070707").matches());
    nano = System.nanoTime() - nano;
    System.out.println(nano / 1000000.0D);
  }
  
  @Test
  public void testBB() throws Exception {
    String wkt = "POLYGON((-4.5 48.0, -4.5 48.5, -4.0 48.5, -4.0 48.0, -4.5 48.0))";
        
    WKTReader reader = new WKTReader();    
    Geometry geometry = null;
    
    try {
      geometry = reader.read(wkt.toString());
    } catch (ParseException pe) {
      pe.printStackTrace();
    }
    
    //
    // Convert Geometry to a GeoXPShape
    //
    
    GeoXPShape shape = GeoXPLib.toGeoXPShape(geometry, 0.01, false);
    
    System.out.println(Arrays.toString(GeoXPLib.bbox(shape)));
  }
  
  
  @Test
  public void testUniform() throws Exception {
    //
    // Read WKT
    //
    
    WKTReader reader = new WKTReader();    
    Geometry geometry = null;
    
    String wkt = "POLYGON((10.689 -25.092, 34.595 -20.170, 38.814 -35.639, 13.502 -39.155, 10.689 -25.092))";
    geometry = reader.read(wkt.toString());
    
    int maxcells = 10000;
    int res = 10;
    
    System.out.println(GeoXPLib.toUniformGeoXPShape(geometry, res, false, maxcells));
  }
  
  @Test
  public void testOptimize() throws Exception {
    WKTReader reader = new WKTReader();    
    String wkt = "POLYGON((10.689 -25.092, 34.595 -20.170, 38.814 -35.639, 13.502 -39.155, 10.689 -25.092))";
    Geometry geometry = reader.read(wkt.toString());
    int res = 14;
    
    GeoXPShape shape = GeoXPLib.toGeoXPShape(geometry, 0.0001, false, Integer.MAX_VALUE);
    shape = GeoXPLib.limitResolution(shape, 12);
    Set<Long> cells1 = new HashSet<Long>();
    for (long cell: shape.geocells) {
      cells1.add(cell);
    }
    shape = GeoXPLib.toGeoXPShape(geometry, 12, false, Integer.MAX_VALUE);
    Set<Long> cells2 = new HashSet<Long>();
    for (long cell: shape.geocells) {
      cells2.add(cell);
    }
    
    System.out.println(cells1.size());
    System.out.println(cells2.size());
    
    //cells1.removeAll(cells2);
    for (long cell: cells1) {
      String hex = Long.toHexString(cell);
      if (hex.contains("60cb9")) {
        System.out.println(hex);
      }
    }
  }
}
