package com.geoxp.heatmap;

import java.util.Collection;

import com.geocoord.thrift.data.HeatMapConfiguration;

public interface HeatMapManager {
  public HeatMapConfiguration getConfiguration();
  public void setConfiguration(HeatMapConfiguration conf);
  
  public TileBuilder getTileBuilder();
  
  public void store(double lat, double lon, long timestamp, int value, boolean update, Collection<Integer> resolutions);
  public void store(double lat, double lon, long timestamp, int value, boolean update);
  public void store(long hhcode, long timestamp, int value, boolean update);
  public void store(long hhcode, long timestamp, int value, boolean update, Collection<Integer> resolutions);
  
  /**
   * Return lat,lon and value for a given geocell.
   * 
   * @param geocell
   * @param timestamp
   * @param bucketspan
   * @param bucketcount
   * @param timedecay
   * @return
   */
  public double[] getData(long geocell, long timestamp, long bucketspan, int bucketcount, double timedecay);
  public void setDoExpire(boolean doexpire);
  public long getBucketCount();
}
