package com.geoxp.heatmap;

public abstract class KernelFunction {

  public abstract double f(double u);
  
  //
  // Library of standard kernels
  // @see http://en.wikipedia.org/wiki/Kernel_(statistics)
  //
  
  public static class UNIFORM extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return 0.5D;
    }
  }
  
  public static class TRIANGULAR extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return (1.0D - Math.abs(u));
    }    
  }
  
  public static class EPANECHNIKOV extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return (3.0D/4.0D) * (1.0D - u * u);
    }    
  }

  public static class QUARTIC extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return (15.0D/16.0D) * Math.pow((1.0D - u * u), 2.0D);
    }    
  }

  public static class TRIWEIGHT extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return (35.0D/32.0D) * Math.pow((1.0D - u * u), 3.0D);
    }    
  }

  public static class TRICUBE extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return (70.0D/81.0D) * Math.pow((1.0D - Math.pow(Math.abs(u), 3.0D)), 3.0D);
    }    
  }

  public static class GAUSSIAN extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return (1.0D / Math.sqrt(2.0D * Math.PI)) * Math.exp(-0.5D * u * u);
    }
  }

  public static class COSINE extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return (Math.PI / 4.0D) * Math.cos((Math.PI / 2.0D) * u);
    }
  }

  public static class LOGISTIC extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return 1.0D / (Math.exp(u) + 2.0D + Math.exp(-u));
    }
  }

  public static class SILVERMAN extends KernelFunction {
    @Override
    public double f(double u) {
      if (u < 1.0D || u > 1.0D) { return 0.0D; }
      return 0.5D * Math.exp(- Math.abs(u) / Math.sqrt(2.0D)) * Math.sin((Math.PI / 4.0D) + Math.abs(u) / Math.sqrt(2.0D));
    }
  }

}
