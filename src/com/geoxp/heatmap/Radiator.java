package com.geoxp.heatmap;

import java.util.HashMap;
import java.util.Map;

/**
 * A Radiator is a radiation model from a heat point centered
 * on a rectangle.
 * 
 * Values vary from -128 to 127, -128 being the brightest (intensity 1.0) and 127 (intensity 0.0) the dimmest.
 * 
 * @author herberts
 *
 */
public class Radiator {
  /**
   * Values of each square's pixel
   */
  byte[][] values = null;

  public enum RadiatorModel { CIRCLE, LINEARKERNEL, SMOOTHKERNEL };
  
  private static Map<String, Radiator> cache = new HashMap<String, Radiator>();
  
  public Radiator(int width, int height) {
    values = new byte[width][];
    for (int i = 0; i < width; i++) {
      values[i] = new byte[height];
      for (int j = 0; j < height; j++) {
        // Initialize at MAX_VALUE (meaning an intensity of 0.0)
        values[i][j] = Byte.MAX_VALUE;
      }
    }
  }
  
  public void init(RadiatorModel model) {
    
    // Center value is higher intensity (-128)
    if (RadiatorModel.CIRCLE == model) {
      int r = (int) Math.floor(Math.sqrt(values.length * values.length + values[0].length * values[0].length) / 2.5);
      if (2 * r > Math.min(values.length, values[0].length)) {
    	r = (int) (Math.min(values.length, values[0].length) / 2);
      }
      
      for (int i = 0; i < 360; i++) {
        for (int j = 0; j <= r; j++) {
          int y = (int)((values[0].length / 2.0) + j * Math.cos(i * Math.PI / 180.0));
          int x = (int)((values.length / 2.0) + j * Math.sin(i * Math.PI / 180.0));
          
          if (x >= 0 && x < values.length && y >= 0 && y < values[0].length) {
              values[x][y] = (byte)(-128 + 255 * (((double)j) / r));
          }
        }
      }
    } else if (RadiatorModel.LINEARKERNEL == model) {
      double cx = values[0].length / 2.0;
      double cy = values.length / 2.0;
      double r = cx;
      
      for (int x = 0; x < values[0].length; x++) {
        for (int y = 0; y < values.length; y++) {
          double d = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
          if (d > r) {
            continue;
          }
          values[x][y] = (byte) ((int) (255 * (d / r)) -128);
        }
      }
    } else if (RadiatorModel.SMOOTHKERNEL == model) {
      double cx = values[0].length / 2.0;
      double cy = values.length / 2.0;
      double r = cx;
      
      for (int x = 0; x < values[0].length; x++) {
        for (int y = 0; y < values.length; y++) {
          double d = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
          if (d > r) {
            continue;
          }
          // (f(0) + (255 - f(0)) * (d/r)) - 128
          values[x][y] = (byte) ((int) (128 + 127 * (d / r)) -128);
        }
      }      
    }
    
    for (int x = 0; x < values.length; x++) {
      for (int y = 0; y < values[0].length; y++) {
        System.out.print(values[x][y]);
        System.out.print(" ");
      }
      System.out.println();
    }
  }
 
  public int getWidth() {
    return values.length;
  }
  public int getHeight() {
    return values[0].length;
  }
  public byte getValue(int x, int y) {
    if (x >= 0 && x <values.length && y>= 0 && y <values[0].length) {
      return values[x][y];
    } else {
      // Return MAX_VALUE (intensity 0.0) if outside the canvas.
      return Byte.MAX_VALUE;
    }
  }
  
  public static Radiator get(String name) {
    
    Radiator rad = cache.get(name);

    if (null != rad) {
      return rad;
    }
    
    if (name.startsWith("lk")) {
      int r = Integer.valueOf(name.substring(2));
      
      if (r > 0 && r <= 32) {
        rad = new Radiator(r, r);
        rad.init(RadiatorModel.LINEARKERNEL);
        cache.put(name, rad);
        return rad;
      }
    } else if (name.startsWith("c")) {
      int r = Integer.valueOf(name.substring(1));      
      
      if (r > 0 && r <= 32) {
        rad = new Radiator(r, r);
        rad.init(RadiatorModel.CIRCLE);
        cache.put(name, rad);
        return rad;
      }
    } else if (name.startsWith("sk")) {
      int r = Integer.valueOf(name.substring(2));
      
      
      if (r > 0 && r <= 32) {
        rad = new Radiator(r, r);
        rad.init(RadiatorModel.CIRCLE);
        cache.put(name, rad);
        return rad;
      }
    }
    
    //
    // Default
    //
    
    return get("lk8");
  }
}
