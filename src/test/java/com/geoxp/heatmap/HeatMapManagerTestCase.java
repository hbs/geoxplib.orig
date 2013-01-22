package com.geoxp.heatmap;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.geoxp.heatmap.Radiator.RadiatorModel;

public class HeatMapManagerTestCase {
  @Test
  public void test() {
    Map<Long,Integer> buckets = new HashMap<Long, Integer>();
    
    buckets.put(300000L, 12);
    buckets.put(3600000L, 24);
    
    MemoryHeatMapManager hmm = new MemoryHeatMapManager(buckets, 10, 22);
    
    System.out.println(hmm);
    
    long now = System.currentTimeMillis();
    
    long nano = System.nanoTime();
    
    for (int i = 0; i < 20000; i++) {
      hmm.store(Math.random() * 180.0 - 90.0, Math.random() * 360.0 - 180.0, now, i, true);
    }
    
    nano = System.nanoTime() - nano;
    
    System.out.println("ms = " + nano/ 1000000.0);
    System.out.println(hmm);

  }
  
  @Test
  public void testOpenSkyMapTileBuilder() throws Exception {
    
    Map<Long,Integer> buckets = new HashMap<Long, Integer>();
    
    buckets.put(300000L, 1);
    buckets.put(3600000L, 1);
    
    //HeatMapManager hmm = new MemoryHeatMapManager(buckets, 4, 14);
    HeatMapManager hmm = new BDBHeatMapManager("OpenSkyMap-test", buckets, 4, 18);
    
    long nano = System.nanoTime();

    if (true) {
    BufferedReader br = new BufferedReader(new FileReader("/Users/hbs/Documents/workspace/GeoCoord/data/volmvt_20110630.csv"));
    
    long now = System.currentTimeMillis();

    System.out.println(now);
    
    int count = 0;
    
    do {
      String line = br.readLine();
      if (null == line) {
        break;    
      }
    
      count++;
      
      String[] tokens = line.split(";");
      
      hmm.store(Double.valueOf(tokens[6].replaceAll("\\\"", "")),
                Double.valueOf(tokens[7].replaceAll("\\\"", "")), now, 1, true);
      
      if (count % 10000 == 0) {
        System.out.println(count);        
      }
    } while (true);
    }
    
    nano = System.nanoTime() - nano;
    
    System.out.println("ms = " + nano/ 1000000.0);
    System.out.println(hmm);
    
    TileBuilder tb = new HeatMapManagerTileBuilder(hmm, 4);

    for (int i = 0; i < 10; i++) {
    nano = System.nanoTime();
    
    int zoom = 7;
    
    Radiator radiator = new Radiator(20, 20);
    radiator.init(RadiatorModel.LINEARKERNEL);
    
    int[] palette = ColorMap.FIRE;
    //palette = ColorMap.generate(0xff0000);
    double opacity = 0.8;
    
    BufferedImage bi = tb.getTile(1315659000000L, 300000L, 1, 1.0, 68, 50, zoom, radiator, palette, opacity);
    
    ImageIO.write(bi, "PNG", new FileOutputStream("/tmp/hm2.png"));
    
    nano = System.nanoTime() - nano;
    
    System.out.println(nano / 1000000.0);
    }
  }
  
  @Test
  public void testTileBuilder() throws Exception {
    Map<Long,Integer> buckets = new HashMap<Long, Integer>();
    
    buckets.put(300000L, 12);
    buckets.put(3600000L, 24);
    
    MemoryHeatMapManager hmm = new MemoryHeatMapManager(buckets, 4, 18);
    
    System.out.println(hmm);
    
    long now = System.currentTimeMillis();
    
    long nano = System.nanoTime();
    
    for (int i = 0; i < 3000; i++) {
      hmm.store(Math.random() * 180.0 - 90.0, Math.random() * 360.0 - 180.0, now, i, true);
    }
    
    nano = System.nanoTime() - nano;
    
    System.out.println("ms = " + nano/ 1000000.0);
    System.out.println(hmm);
    
    TileBuilder tb = new HeatMapManagerTileBuilder(hmm, 4);
    
    nano = System.nanoTime();
    
    Radiator radiator = new Radiator(16, 16);
    radiator.init(RadiatorModel.CIRCLE);
    
    int[] palette = ColorMap.CLASSIC;
    double opacity = 1.0;
    
    BufferedImage bi = tb.getTile(1315657500000L, 300000L, 1, 1.0, 0, 0, 1, radiator, palette, opacity);
    
    ImageIO.write(bi, "PNG", new FileOutputStream("/tmp/hm2.png"));
    
    nano = System.nanoTime() - nano;
    
    System.out.println(nano / 1000000.0);
  }
}
