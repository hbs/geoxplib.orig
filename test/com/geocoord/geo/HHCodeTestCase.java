package com.geocoord.geo;

import java.util.List;

import junit.framework.TestCase;

public class HHCodeTestCase extends TestCase {

    public void testHHCode_String() {
      HHCode hhcode = new HHCode(48.0, -4.5);
            
      assertEquals("b57070707070705a", hhcode.toString());
      
      hhcode = new HHCode(0.0, 0.0);
      assertEquals("3fffffffffffffff", hhcode.toString());
            
      hhcode = new HHCode(-90.0, -180.0);
      assertEquals("0000000000000000", hhcode.toString());

      hhcode = new HHCode(90.0, 180.0);
      assertEquals("ffffffffffffffff", hhcode.toString());

      
      hhcode = new HHCode(0x0123456789abcdefL);
      assertEquals("0123456789abcdef", hhcode.toString());
      
      for (int i = 1; i < 17; i++) {
        assertEquals("0123456789abcdef".substring(0,i), hhcode.toString(i * 2));
      }
    }
    
    public void testHHCode_Long() {
      HHCode hhcode = new HHCode(0.0,0.0);
      assertEquals(0x3fffffffffffffffL, hhcode.value());

      hhcode = new HHCode(0x0123456789abcdefL);

      for (int i = 1; i < 17; i++) {
        assertEquals(0x0123456789abcdefL >> ((16 - i) * 4), hhcode.value(i * 2));
      }      
    }
    
    public void testHHCode_LatLon() {
      HHCode hhcode = new HHCode(0xffffffffffffffffL);
      
      assertEquals(90.0, hhcode.getLat());
      assertEquals(180.0, hhcode.getLon());
      
      hhcode = new HHCode(0);

      assertEquals(-90.0, hhcode.getLat());
      assertEquals(-180.0, hhcode.getLon());
      
      hhcode = new HHCode(0xb57070707070705aL);
      assertEquals(48.0, hhcode.getLat(), 0.0000001);
      assertEquals(-4.5, hhcode.getLon(), 0.0000001);
      
      hhcode = new HHCode(0x3fffffffffffffffL);
      assertEquals(0, hhcode.getLat(), 0.0000001);
      assertEquals(0, hhcode.getLon(), 0.0000001);
    }
    
    public void testHHCode_bbox() {
      assertEquals("0 1 4 5 2 3 6 7 8 9 c d a b e f", HHCode.bbox(-90,-180,90,180));
      assertEquals("0 1 2 3 8 9 a b", HHCode.bbox(-90,-180,90,0));
      assertEquals("0 1 2 3", HHCode.bbox(-90,-180,0,0));
      assertEquals("c d e f", HHCode.bbox(0.0000001, 0.0000001, 90, 180));
      assertEquals("b570 b571 b574 b575 e020 e021 e024 b572 b573 b576 b577 e022 e023 e026", HHCode.bbox(48, -5, 49, 4));
      assertEquals("9ff caa cab b55 e00 e01 b57 e02 e03 b5d e08 e09", HHCode.bbox(43, -5.5, 51.2, 6.1));
    }
    
    public void testHHCode_steps() {
     List<Long> steps = HHCode.steps(0x11, 0x75); 
     System.out.println(steps);
    }
    
    public void testBbox_InternationalDateLine() {
      System.out.println(HHCode.bbox(-90,90.01,90,-90));
    }
    public void testGeoboxes() {
      System.out.println(HHCode.geoboxes(-4.214943141390638, -81.2109375, 70.61261423801925, 94.5703125));
    }
}
