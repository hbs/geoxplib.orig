package com.geoxp.heatmap;

import java.util.Collection;

public interface HeatMapManager {
  public void store(double lat, double lon, long timestamp, int value, boolean update, Collection<Integer> resolutions);
  public void store(double lat, double lon, long timestamp, int value, boolean update);
  public void store(long hhcode, long timestamp, int value, boolean update);
  public void store(long hhcode, long timestamp, int value, boolean update, Collection<Integer> resolutions);
  public double getData(long geocell, long timestamp, long bucketspan, int bucketcount, double timedecay);
  public int getMinResolution();
  public int getMaxResolution();
  public void setDoExpire(boolean doexpire);
  public long getBucketCount();
}
