package com.geoxp.heatmap;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.geocoord.geo.ObjectSizeFetcher;

public class T0 {
  @Test
  public void testMap() throws Exception {
    System.out.println(Runtime.getRuntime().totalMemory() + " " + Runtime.getRuntime().freeMemory());
    long free = Runtime.getRuntime().freeMemory();

    Map<Long,int[]> map = new HashMap<Long, int[]>();
    
    for (int i = 0; i < 10000000; i++) {
      map.put((long) i, new int[] { 0, 1, 2 });
      if (i % 100000 == 0) {
        System.out.println(Runtime.getRuntime().totalMemory() + " " + Runtime.getRuntime().freeMemory());
      }
    }
    
    System.out.println("OCC=" + (Runtime.getRuntime().freeMemory() - free));
    System.out.println(Runtime.getRuntime().totalMemory() + " " + Runtime.getRuntime().freeMemory());
  }
}
