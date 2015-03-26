package com.geoxp.heatmap;

/**
 * Kernel used to build heatmaps
 */
public class Kernel {
  
  private static final byte MIN_VALUE = 127;
  
  private static final byte MAX_VALUE = -128;
  
  /**
   * Matrix of kernel values
   */
  private final byte[][] values;
  
  /**
   * 
   * @param width Width of kernel
   * @param height Height of kernel
   * @param func Kernel Function
   */
  public Kernel(int width, int height, KernelFunction func) {
    
    if (width < 2 || height < 2) {
      throw new RuntimeException("Kernel width/height MUST be >= 2");
    }
    
    this.values = new byte[width][height];
    
    // Compute byte kernel function values for a range of x values from 0 to 1
    byte[] kfvalues = new byte[Math.round(Math.max(width, height) / 2)];
    
    //
    // Compute kernel function value as byte
    //
    
    
    for (int i = 0; i < kfvalues.length; i++) {
      double v = func.f(i * 2.0D / (kfvalues.length - 1));
      
      if (v < 0.0D) {
        v = 0.0D;
      }
      if (v > 1.0D) {
        v = 1.0D;
      }
      
      kfvalues[i] = (byte) (MAX_VALUE + Math.abs(MAX_VALUE - MIN_VALUE) * (1.0 - v));
    }
    
    //
    // Fill kernel matrix
    //
    
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        // Compute x/y
        double x = -1.0D + i * 2.0D / (width - 1);
        double y = -1.0D + j * 2.0D / (height - 1);
        
        // Compute distance from center
        double d = Math.sqrt(x * x + y * y);
        
        if (d > 1.0D) {
          this.values[i][j] = MIN_VALUE;
          continue;
        }
        
        // Compute offset in kfvalues array
        int idx = (int) (d / (1.0D / (kfvalues.length - 1)));
        
        this.values[i][j] = kfvalues[idx];
      }
    }
  }
  
  public int getWidth() {
    return this.values.length;
  }
  
  public int getHeight() {
    return this.values[0].length;
  }
  
  public byte getValue(int x, int y) {
    if (x < 0 || x >= this.values.length) {
      return MIN_VALUE;
    }
    if (y < 0 || y >= this.values[0].length) {
      return MIN_VALUE;
    }
    
    return this.values[x][y];
  }
  
  public static Kernel get(String ref) {
    ref = ref.toUpperCase();
    
    int size = Integer.parseInt(ref.replaceAll("[^0-9]", ""));
    
    if (size < 0 || size > 128) {
      size = 48;
    }
    
    if (ref.startsWith("UNIFORM")) {
      return new Kernel(size, size, new KernelFunction.UNIFORM());
    } else if (ref.startsWith("TRIANGULAR")) {
      return new Kernel(size, size, new KernelFunction.TRIANGULAR());
    } else if (ref.startsWith("EPANECHNIKOV")) {
      return new Kernel(size, size, new KernelFunction.EPANECHNIKOV());
    } else if (ref.startsWith("QUARTIC")) {
      return new Kernel(size, size, new KernelFunction.QUARTIC());
    } else if (ref.startsWith("TRIWEIGHT")) {
      return new Kernel(size, size, new KernelFunction.TRIWEIGHT());
    } else if (ref.startsWith("TRICUBE")) {
      return new Kernel(size, size, new KernelFunction.TRICUBE());
    } else if (ref.startsWith("GAUSSIAN")) {
      return new Kernel(size, size, new KernelFunction.GAUSSIAN());
    } else if (ref.startsWith("COSINE")) {
      return new Kernel(size, size, new KernelFunction.COSINE());
    } else if (ref.startsWith("LOGISTIC")) {
      return new Kernel(size, size, new KernelFunction.LOGISTIC());
    } else if (ref.startsWith("SILVERMAN")) {
      return new Kernel(size, size, new KernelFunction.SILVERMAN());
    } else {
      return new Kernel(size, size, new KernelFunction.GAUSSIAN());
    }
  }
}
