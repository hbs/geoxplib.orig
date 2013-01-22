package com.geoxp.heatmap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintStream;

import javax.imageio.ImageIO;

public class Image2Heatmap {

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    BufferedImage img = ImageIO.read(new File("/var/tmp/logo.png"));
    
    double leftLon = -90.0;
    double rightLon = 90.0;
    double topLat = 45.0;
    double bottomLat = -45.0;
    
    double deltaLat = Math.abs(topLat - bottomLat) / img.getHeight();
    double deltaLon = Math.abs(leftLon - rightLon) / img.getWidth();
    
    PrintStream ps = new PrintStream(new File("/var/tmp/geoxp.heatmap"));
    
    for (int x = 0; x < img.getWidth(); x++) {
      for (int y = 0; y < img.getHeight(); y++) {
        int rgb = img.getRGB(x, y);
        
        if (0 == (rgb & 0xff000000)) {
          continue;
        }
        
        ps.print(":");
        ps.print(topLat - y * deltaLat);
        ps.print(":");
        ps.print(leftLon + x * deltaLon);
        ps.println(":1:");
      }
    }
    
    ps.close();
  }

}
