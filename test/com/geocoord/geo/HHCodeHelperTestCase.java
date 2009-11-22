package com.geocoord.geo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class HHCodeHelperTestCase extends TestCase {
  
  public void testGetHHCodeValue() {    
    assertEquals(0xb570707070707071L, HHCodeHelper.getHHCodeValue(48.0, -4.5));
    assertEquals(0xc000000000000000L, HHCodeHelper.getHHCodeValue(0.0, 0.0));
    assertEquals(0x0000000000000000L, HHCodeHelper.getHHCodeValue(-90.0, -180.0));
    assertEquals(0xffffffffffffffffL, HHCodeHelper.getHHCodeValue(90.0, 180.0));
    assertEquals(0xffffffffffffffffL, HHCodeHelper.getHHCodeValue(91.0, 181.0));
  }
  
  public void testSplitHHCode() {
    long[] coords = HHCodeHelper.splitHHCode(0xc000000000000000L, 32);
    
    assertEquals(0x80000000L, coords[0]);
    assertEquals(0x80000000L, coords[1]);

    coords = HHCodeHelper.splitHHCode(0xffffffffffffffffL, 16);

    assertEquals(0xffff0000L, coords[0]);
    assertEquals(0xffff0000L, coords[1]);
    
    coords = HHCodeHelper.splitHHCode(0xe036a70a028aa0aaL, 10);
    
    System.out.println(coords[1]);
  }
  
  public void testAbove() {
    
    //
    // Test even levels for cell 0
    //
    
    assertEquals(0x0000000000000002L, HHCodeHelper.above(0L, 32));
    assertEquals(0x0000000000000020L, HHCodeHelper.above(0L, 30));
    assertEquals(0x0000000000000200L, HHCodeHelper.above(0L, 28));
    assertEquals(0x0000000000002000L, HHCodeHelper.above(0L, 26));
    assertEquals(0x0000000000020000L, HHCodeHelper.above(0L, 24));
    assertEquals(0x0000000000200000L, HHCodeHelper.above(0L, 22));
    assertEquals(0x0000000002000000L, HHCodeHelper.above(0L, 20));
    assertEquals(0x0000000020000000L, HHCodeHelper.above(0L, 18));
    assertEquals(0x0000000200000000L, HHCodeHelper.above(0L, 16));
    assertEquals(0x0000002000000000L, HHCodeHelper.above(0L, 14));
    assertEquals(0x0000020000000000L, HHCodeHelper.above(0L, 12));
    assertEquals(0x0000200000000000L, HHCodeHelper.above(0L, 10));
    assertEquals(0x0002000000000000L, HHCodeHelper.above(0L, 8));
    assertEquals(0x0020000000000000L, HHCodeHelper.above(0L, 6));
    assertEquals(0x0200000000000000L, HHCodeHelper.above(0L, 4));
    assertEquals(0x2000000000000000L, HHCodeHelper.above(0L, 2));
    
    //
    // Test above cell at level 32 for cells 1-f
    //
    
    assertEquals(0x0000000000000003L, HHCodeHelper.above(1L, 32));
    assertEquals(0x0000000000000006L, HHCodeHelper.above(4L, 32));
    assertEquals(0x0000000000000007L, HHCodeHelper.above(5L, 32));
    assertEquals(0x0000000000000008L, HHCodeHelper.above(2L, 32));
    assertEquals(0x0000000000000009L, HHCodeHelper.above(3L, 32));
    assertEquals(0x000000000000000cL, HHCodeHelper.above(6L, 32));
    assertEquals(0x000000000000000dL, HHCodeHelper.above(7L, 32));
    assertEquals(0x000000000000000aL, HHCodeHelper.above(8L, 32));
    assertEquals(0x000000000000000bL, HHCodeHelper.above(9L, 32));
    assertEquals(0x000000000000000eL, HHCodeHelper.above(0xcL, 32));
    assertEquals(0x000000000000000fL, HHCodeHelper.above(0xdL, 32));
    assertEquals(0x0000000000000020L, HHCodeHelper.above(0xaL, 32));
    assertEquals(0x0000000000000021L, HHCodeHelper.above(0xbL, 32));
    assertEquals(0x0000000000000024L, HHCodeHelper.above(0xeL, 32));
    assertEquals(0x0000000000000025L, HHCodeHelper.above(0xfL, 32));

    
    //
    // Test wrap around
    //
    
    assertEquals(0x0000000000000000L, HHCodeHelper.above(0xaaaaaaaaaaaaaaaaL, 32));
    assertEquals(0x1111111111111111L, HHCodeHelper.above(0xbbbbbbbbbbbbbbbbL, 32));
    assertEquals(0x4444444444444444L, HHCodeHelper.above(0xeeeeeeeeeeeeeeeeL, 32));
    assertEquals(0x5555555555555555L, HHCodeHelper.above(0xffffffffffffffffL, 32));
  }
  
  public void testBelow() {
    
    assertEquals(0x0000000000000000L, HHCodeHelper.below(0x0000000000000002L, 32));
    assertEquals(0x0000000000000001L, HHCodeHelper.below(0x0000000000000003L, 32));
    assertEquals(0x0000000000000004L, HHCodeHelper.below(0x0000000000000006L, 32));
    assertEquals(0x0000000000000005L, HHCodeHelper.below(0x0000000000000007L, 32));
    assertEquals(0x0000000000000002L, HHCodeHelper.below(0x0000000000000008L, 32));
    assertEquals(0x0000000000000003L, HHCodeHelper.below(0x0000000000000009L, 32));
    assertEquals(0x0000000000000006L, HHCodeHelper.below(0x000000000000000cL, 32));
    assertEquals(0x0000000000000007L, HHCodeHelper.below(0x000000000000000dL, 32));
    assertEquals(0x0000000000000008L, HHCodeHelper.below(0x000000000000000aL, 32));
    assertEquals(0x0000000000000009L, HHCodeHelper.below(0x000000000000000bL, 32));
    assertEquals(0x000000000000000cL, HHCodeHelper.below(0x000000000000000eL, 32));
    assertEquals(0x000000000000000dL, HHCodeHelper.below(0x000000000000000fL, 32));
    assertEquals(0x000000000000000aL, HHCodeHelper.below(0x0000000000000020L, 32));
    assertEquals(0x000000000000000bL, HHCodeHelper.below(0x0000000000000021L, 32));
    assertEquals(0x000000000000000eL, HHCodeHelper.below(0x0000000000000024L, 32));
    assertEquals(0x000000000000000fL, HHCodeHelper.below(0x0000000000000025L, 32));
    
    // Test wrap around
    
    assertEquals(0xaaaaaaaaaaaaaaaaL, HHCodeHelper.below(0x0000000000000000L, 32));
    assertEquals(0xbbbbbbbbbbbbbbbbL, HHCodeHelper.below(0x1111111111111111L, 32));
    assertEquals(0xeeeeeeeeeeeeeeeeL, HHCodeHelper.below(0x4444444444444444L, 32));
    assertEquals(0xffffffffffffffffL, HHCodeHelper.below(0x5555555555555555L, 32));
    
    //
    // Test some random hhcodes at random resolutions.
    // This test assumes testAbove succeeded
    //
    
    for (int r = 32; r > 0; r--) {
      for (int i = 0; i < 100; i++) {
        long hhcode = Math.round(Math.random() * (1L << 64));
        
        assertEquals(hhcode, HHCodeHelper.below(HHCodeHelper.above(hhcode, r), r));
      }      
    }
  }
  
  public void testRight() {
    assertEquals(0x1L, HHCodeHelper.right(0L, 32));
    assertEquals(0x4L, HHCodeHelper.right(1L, 32));
    assertEquals(0x5L, HHCodeHelper.right(4L, 32));
    assertEquals(0x3L, HHCodeHelper.right(2L, 32));
    assertEquals(0x6L, HHCodeHelper.right(3L, 32));
    assertEquals(0x7L, HHCodeHelper.right(6L, 32));
    assertEquals(0x9L, HHCodeHelper.right(8L, 32));
    assertEquals(0xcL, HHCodeHelper.right(9L, 32));
    assertEquals(0xdL, HHCodeHelper.right(0xcL, 32));
    
    assertEquals(0x10L, HHCodeHelper.right(5L, 32));
    assertEquals(0x12L, HHCodeHelper.right(7L, 32));
    assertEquals(0x18L, HHCodeHelper.right(0xdL, 32));
    assertEquals(0x1aL, HHCodeHelper.right(0xfL, 32));

    // Test wrap around
    
    assertEquals(0x0000000000000000L, HHCodeHelper.right(0x5555555555555555L, 32));
    assertEquals(0x2222222222222222L, HHCodeHelper.right(0x7777777777777777L, 32));
    assertEquals(0x8888888888888888L, HHCodeHelper.right(0xddddddddddddddddL, 32));
    assertEquals(0xaaaaaaaaaaaaaaaaL, HHCodeHelper.right(0xffffffffffffffffL, 32));
  }

  public void testLeft() {
    assertEquals(0x1L, HHCodeHelper.left(4L, 32));
    assertEquals(0x4L, HHCodeHelper.left(5L, 32));
    assertEquals(0x5L, HHCodeHelper.left(0x10L, 32));
    assertEquals(0x3L, HHCodeHelper.left(6L, 32));
    assertEquals(0x6L, HHCodeHelper.left(7L, 32));
    assertEquals(0x7L, HHCodeHelper.left(0x12L, 32));
    assertEquals(0x9L, HHCodeHelper.left(0xcL, 32));
    assertEquals(0xcL, HHCodeHelper.left(0xdL, 32));
    assertEquals(0xdL, HHCodeHelper.left(0x18L, 32));
    
    assertEquals(0x0L, HHCodeHelper.left(1L, 32));
    assertEquals(0x2L, HHCodeHelper.left(3L, 32));
    assertEquals(0x8L, HHCodeHelper.left(9L, 32));
    assertEquals(0xaL, HHCodeHelper.left(0xbL, 32));
    
    // Test random values, assuming testRight passed
    
    for (int r = 32; r > 0; r--) {
      for (int i = 0; i < 100; i++) {
        long hhcode = Math.round(Math.random() * (1L << 64));
        
        assertEquals(hhcode, HHCodeHelper.left(HHCodeHelper.right(hhcode, r), r));        
      }
    }
    
    //
    // Test wrap around
    //
    
    assertEquals(0x5555555555555555L, HHCodeHelper.left(0x0000000000000000L, 32));
    assertEquals(0x7777777777777777L, HHCodeHelper.left(0x2222222222222222L, 32));
    assertEquals(0xddddddddddddddddL, HHCodeHelper.left(0x8888888888888888L, 32));
    assertEquals(0xffffffffffffffffL, HHCodeHelper.left(0xaaaaaaaaaaaaaaaaL, 32));
  }
  
  public void testToString() {
    assertEquals("0000000000000000", HHCodeHelper.toString(0L));
    assertEquals("123456789abcdef0", HHCodeHelper.toString(0x123456789abcdef0L));
    
    assertEquals("1", HHCodeHelper.toString(0x123456789abcdef0L, 2));
    assertEquals("12", HHCodeHelper.toString(0x123456789abcdef0L, 4));
    assertEquals("123", HHCodeHelper.toString(0x123456789abcdef0L, 6));
    assertEquals("1234", HHCodeHelper.toString(0x123456789abcdef0L, 8));
    assertEquals("12345", HHCodeHelper.toString(0x123456789abcdef0L, 10));
    assertEquals("123456", HHCodeHelper.toString(0x123456789abcdef0L, 12));
    assertEquals("1234567", HHCodeHelper.toString(0x123456789abcdef0L, 14));
    assertEquals("12345678", HHCodeHelper.toString(0x123456789abcdef0L, 16));
    assertEquals("123456789", HHCodeHelper.toString(0x123456789abcdef0L, 18));
    assertEquals("123456789a", HHCodeHelper.toString(0x123456789abcdef0L, 20));
    assertEquals("123456789ab", HHCodeHelper.toString(0x123456789abcdef0L, 22));
    assertEquals("123456789abc", HHCodeHelper.toString(0x123456789abcdef0L, 24));
    assertEquals("123456789abcd", HHCodeHelper.toString(0x123456789abcdef0L, 26));
    assertEquals("123456789abcde", HHCodeHelper.toString(0x123456789abcdef0L, 28));
    assertEquals("123456789abcdef", HHCodeHelper.toString(0x123456789abcdef0L, 30));
  }
  
  public void testBbox() {
    assertEquals("0 1 4 5 2 3 6 7 8 9 c d a b e f", HHCodeHelper.bbox(-90,-180,90,180));
    assertEquals("0 1 2 3 8 9 a b", HHCodeHelper.bbox(-90,-180,90.0,-0.0000001));
    assertEquals("0 1 2 3", HHCodeHelper.bbox(-90,-180,-0.0000001,-0.0000001));
    assertEquals("c d e f", HHCodeHelper.bbox(0, 0, 90, 180));
    assertEquals("b570 b571 b574 b575 e020 e021 e024 b572 b573 b576 b577 e022 e023 e026", HHCodeHelper.bbox(48, -5, 49, 4));
    assertEquals("9ff caa cab b55 e00 e01 b57 e02 e03 b5d e08 e09", HHCodeHelper.bbox(43, -5.5, 51.2, 6.1));
  }
  
  public void testOptimize() {
  }
  
  public void testCoverPolygon() {
    
    
    
    List<Long> vertices = new ArrayList<Long>() {{
      add(HHCodeHelper.getHHCodeValue(-90.0, -180.0));
      add(HHCodeHelper.getHHCodeValue(-90.0, 180.0));
      add(HHCodeHelper.getHHCodeValue(90.0, 180.0));
      add(HHCodeHelper.getHHCodeValue(90.0, -180.0));      
    }};
    
    long nano = System.nanoTime();
    Map<Integer,List<Long>> coverage = HHCodeHelper.coverPolygon(vertices, 10);
    System.out.println(System.nanoTime() - nano);
    HHCodeHelper.optimize(coverage, 0x0000000000000000L);
    //System.out.println(coverage);
    System.out.println(HHCodeHelper.getCoverageString(coverage));
  
  
    
    vertices = new ArrayList<Long>() {{
      add(HHCodeHelper.getHHCodeValue(51.344338660599234,2.548828125));
      add(HHCodeHelper.getHHCodeValue(48.574789910928864,-5.537109375));
      add(HHCodeHelper.getHHCodeValue(43.45291889355465,-1.93359375));
      add(HHCodeHelper.getHHCodeValue(42.09822241118974,3.515625));
      add(HHCodeHelper.getHHCodeValue(43.89789239125797,8.876953125));
      add(HHCodeHelper.getHHCodeValue(49.0954521625348,8.701171875));
    }};

    nano = System.nanoTime();
    coverage = HHCodeHelper.coverPolygon(vertices, 12);
    System.out.println(System.nanoTime() - nano);
    HHCodeHelper.optimize(coverage, 0x0000000000000000L);
    //System.out.println(coverage);
    int ncells = 0;
    for (List<Long> cells: coverage.values()) {
      ncells += cells.size();
    }
    System.out.println(ncells + " cells");
    System.out.println("POST OPT " + HHCodeHelper.getCoverageString(coverage));
      
     
  }
  
  public static void main(String[] args) {
    
  }
  
  public void testOptimize_Thresholds() {
    Map<Integer,List<Long>> coverage = new HashMap<Integer, List<Long>>() {{
      put(32, new ArrayList<Long>() {{
        add(0x1L);
      }});
    }};
    
    //
    // Optimize coverage with a clustering threhold of 1 for resolution 32, i.e.
    // if one cell of resolution 32 is found, replace it by its enclosing cell of
    // resolution 30.
    //
    
    HHCodeHelper.optimize(coverage, 0x00000000000000001L);
    
    //
    // The optimized coverage should have 1 cell at resolution 30 (0) and
    // none at resolution 32.
    //
    
    assertEquals(2, coverage.keySet().size());
    assertEquals(0, coverage.get(32).size());
    assertEquals(1, coverage.get(30).size());
    assertTrue(coverage.get(30).contains(0L));
  }
}

