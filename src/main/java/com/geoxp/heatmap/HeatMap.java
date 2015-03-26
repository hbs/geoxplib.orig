package com.geoxp.heatmap;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class HeatMap {
  
  private int width = 0;
  private int height = 0;
  private byte[][] map = null;
  
  public HeatMap(int width, int height) {
    this.width = width;
    this.height = height;
    map = new byte[width][];
    for (int i = 0; i < width; i++) {
      map[i] = new byte[height];
      Arrays.fill(map[i], Byte.MAX_VALUE);
    }
  }
  
  /**
   * Add radiation to the map.
   * 
   * @param x X coordinate of radiating kernel
   * @param y Y coordinate of radiating kernel
   * @param kernel Kernel centered in X/Y
   * @param intensity Intensity of kernel (from 0.0 to 1.0)
   */
  public void radiate(int x, int y, Kernel kernel, double intensity) {
    
    if (0.0D == intensity) {
      return;
    }
    
    int xoffset = kernel.getWidth() / 2;
    int yoffset = kernel.getHeight() / 2;
    
    for (int i = x - xoffset; i < x + xoffset; i++) {
      for (int j = y - yoffset; j < y + yoffset; j++) {
        if (i >= 0 && j >= 0 && i < width && j < height) {
          
          // Read the value from the kernel (offset it to [-255,0])
          int v2 = kernel.getValue(i - (x - xoffset), j - (y - yoffset)) - 127;

          // Nothing to do if v2 is 0
          if (0 == v2) {
            continue;
          }
          
          // Multiply the value from the kernel by the intensity
          // Since the value is in the range -255 (brightest) to 0 (dimmest) and
          // the intensity is [0,1.0] we can multiply by the intensity directly.
          //
          // After multiplying we shift v2 into [0,255] again
          v2 = (int) (255 + (int) (intensity * v2));

          // Read the current value on the map (offset it to [0,255])
          int v1 = 128 + (int) map[i][j];

          // Multiply v1 by v2, the brightest values being closest to 0, multiplicqtion increases brightness
          // Shift result back to [-128,127]
          map[i][j] = (byte) (-128 + (v1*v2)/255);          
        }
      }
    }
  }
  
  public BufferedImage getImage(int offsetX, int offsetY, int imgWidth, int imgHeight, int[] palette, double opacityshift) {
    
    // Clone the palette so we can change the alpha channel
    // by multiplying it by the opacityshift
    
    int[] shiftedpalette = new int[palette.length];
  
    System.arraycopy(palette, 0, shiftedpalette, 0, palette.length);
    
    if (opacityshift < 1.0) {
      for (int i = 0; i < shiftedpalette.length; i++) {
        if (opacityshift == 0.0) {
          shiftedpalette[i] &= 0x00ffffff;
        } else {
          int alpha = (shiftedpalette[i] >> 24) & 0xff;
          alpha = (((int) (alpha * opacityshift)) & 0xff) << 24;
          shiftedpalette[i] = alpha | (shiftedpalette[i] & 0x00ffffff);
        }
      }
    }
      
    BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
    //System.out.println("IMG=" + (System.nanoTime() - nano));
    
    for (int x = 0; x < imgWidth; x++) {
      for (int y = 0; y < imgHeight; y++) {
        int v = (128 + map[x+offsetX][y+offsetY]);   
        image.setRGB(x,y,shiftedpalette[v]);
      }
    }
    
    return image;
    
    //System.out.println("IMG=" + (System.nanoTime() - nano));
    //ImageIO.write(image, "PNG", new FileOutputStream("/tmp/hm.png"));
    //System.out.println("WRITE=" + (System.nanoTime() - nano));
  }
  
  public void dump() {
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        System.out.print(map[i][j]);
        System.out.print(" ");
      }
      System.out.println();
    }
  }
  
  public int getWidth() {
    return width;
  }
  public int getHeight() {
    return height;
  }
}
