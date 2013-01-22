package com.geoxp.heatmap;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class BDBHeatMapManagerTestCase {
  @Test
  public void test() throws Exception {
    Map<Long,Integer> buckets = new HashMap<Long, Integer>();
    
    buckets.put(300000L, 1);
    buckets.put(3600000L, 1);

    BDBHeatMapManager manager = new BDBHeatMapManager("TEST", buckets, 4, 16);
    
    long now = System.currentTimeMillis();
    
    long nano = System.nanoTime();
    
    for (int i = 0; i < 100000; i++) {
      manager.store(i, now, 1, false);
    }
    
    nano = System.nanoTime() - nano;
    
    System.out.println(nano / 1000000.0);
  }
}
