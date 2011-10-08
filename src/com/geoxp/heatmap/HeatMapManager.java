package com.geoxp.heatmap;

import java.io.IOException;
import java.io.OutputStream;
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
  
  public void clear();
  
  /**
   * Expire data not updated for more than 'threshold' ms.
   * 
   * @param threshold
   */
  public void expire(long threshold);
  
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
  
  /**
   * Create a snapshot of the HeatMap data.
   * 
   * @param out OutputStream to write the snapshot to.
   * @param resolutions Collection of resolutions to consider (if null or empty, output all resolutions)
   */
  public void snapshot(OutputStream out, Collection<Integer> resolutions) throws IOException;
  
  /**
   * Restores a geocell's buckets from data created by 'snapshot'
   * @param encoded
   */
  public void restore(String encoded);
}
