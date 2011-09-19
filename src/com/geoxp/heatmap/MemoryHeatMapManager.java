package com.geoxp.heatmap;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.thrift.data.HeatMapConfiguration;

/**
 * Class that manages multiresolution geo data suitable for creating
 * heatmaps.
 */
public class MemoryHeatMapManager implements HeatMapManager {
  
  /**
   * Configuration of this HeatMapManager
   */
  private HeatMapConfiguration configuration;
  
  /**
   * Tile builder associated with this manager
   */
  private final TileBuilder builder;
  
  /**
   * Map from bucketspan to index in the bucket array.
   */
  private final Map<Long,Integer> bucketIndices;
  
  /**
   * Number of buckets per bucketspan
   */
  private final Map<Long, Integer> bucketCounts;
  
  /**
   * Total number of buckets managed per geo-cell
   */
  private final int totalBuckets;
  
  /**
   * Resolution prefixed for geo-cells, indexed by resolution (even 2->30)
   */
  private static final long[] RESOLUTION_PREFIXES = { 0x0L,
    0x0L, 0x1000000000000000L, // 2
    0x0L, 0x2000000000000000L, // 4
    0x0L, 0x3000000000000000L, // 6
    0x0L, 0x4000000000000000L, // 8
    0x0L, 0x5000000000000000L, // 10
    0x0L, 0x6000000000000000L, // 12
    0x0L, 0x7000000000000000L, // 14
    0x0L, 0x8000000000000000L, // 16
    0x0L, 0x9000000000000000L, // 18
    0x0L, 0xa000000000000000L, // 20
    0x0L, 0xb000000000000000L, // 22
    0x0L, 0xc000000000000000L, // 24
    0x0L, 0xd000000000000000L, // 26
    0x0L, 0xe000000000000000L, // 28
    0x0L, 0xf000000000000000L, // 30
  };
  
  /**
   * Map of Geo-Cell to buckets
   * 
   * Each bucket is a pair of ints, the first one being the
   * 
   * Each bucket is composed of the following (in this order):
   * 
   *   int  lower boundary of the bucket (in s since the epoch)
   *   int  value of the bucket
   *   long (2xint) hhcode of the centroid if computing centroids
   *   
   * A timestamped (ts) value will end in a bucket if ((ts - (ts % bucketspan)) / 1000) == boundary
   *  
   * Those buckets are stored in the int[] array. The int at index 0
   * is the timestamp of the last update of this geocell.
   * 
   */
  private final TLongObjectHashMap<int[]> geobuckets;
  
  /**
   * Default resolutions to use if none were specified.
   */
  private final HashSet<Integer> default_resolutions;
  
  /**
   * Length of longer bucketspan * bucketcount
   */
  private long maxspan;
  
  /**
   * Should we expire data as we read it
   */
  private boolean doexpire = true;
  
  /**
   * Flag indicating whether or not to perform
   * fast expires.
   */
  private boolean fastExpire = false;
  
  /**
   * Size of records in bytes
   */
  private final int recordSize;

  /**
   * Size of each bucket in 'ints'
   */
  private final int bucketSize;
  
  /**
   * Create an instance of HeatMapManager which will manage
   * data in the given buckets.
   * 
   * @param buckets Map of bucketspan (in ms) to bucket count.
   * @param minresolution Coarsest resolution considered (even 2->30)
   * @param maxresolution Finest resolution considered (even 2->30)
   */
  public MemoryHeatMapManager(HeatMapConfiguration configuration) {
    
    this.configuration = configuration;
    
    this.builder = new HeatMapManagerTileBuilder(this);
    
    int index = 0;
    
    bucketIndices = new HashMap<Long, Integer>();
    bucketCounts = new HashMap<Long, Integer>();
    
    for (long bucketspan: this.configuration.getBuckets().keySet()) {
      
      int count = this.configuration.getBuckets().get(bucketspan);
      
      bucketCounts.put(bucketspan, count);
      bucketIndices.put(bucketspan, index);
      
      if (bucketspan * count > maxspan) {
        maxspan = bucketspan * count;
      }
      
      index += count;;
    }
    
    totalBuckets = index;
    
    //
    // Set record size:
    //
    // 1 int (4 bytes) for last update timestamp
    // 2 int (2*4 bytes) for each (timestamp,value) tuple
    // 1 long (1*8 bytes) for each tuple (for the hhcode of the centroid)
    //
    
    recordSize = 1 + (2 + (this.configuration.isCentroidEnabled() ? 2 : 0)) * totalBuckets;

    bucketSize = this.configuration.isCentroidEnabled() ? 4 : 2;
    
    geobuckets = new TLongObjectHashMap<int[]>();
    
    this.default_resolutions = new HashSet<Integer>();
    
    for (int r = this.configuration.getMinResolution(); r <= this.configuration.getMaxResolution(); r+=2) {
      default_resolutions.add(r);
    }    
  }
  
  @Override
  public void store(double lat, double lon, long timestamp, int value, boolean update) {
    store(lat, lon, timestamp, value, update, default_resolutions);
  }

  @Override
  public void store(double lat, double lon, long timestamp, int value, boolean update, Collection<Integer> resolutions) {
    long hhcode = HHCodeHelper.getHHCodeValue(lat, lon);
    store(hhcode, timestamp, value, update, resolutions);
  }
  
  @Override
  public void store(long hhcode, long timestamp, int value, boolean update) {
    store(hhcode, timestamp, value, update, default_resolutions);
  }
  
  @Override
  public void store(long hhcode, long timestamp, int value, boolean update, Collection<Integer> resolutions) {
    
    //
    // Check if we've reached the maximum number of buckets (if set)
    // or the high/low watermark
    //
    
    if (this.configuration.getMaxBuckets() > 0) {
      long nbuckets = geobuckets.size() * totalBuckets;

      if (!fastExpire && this.configuration.getHighWaterMark() > 0 && nbuckets > this.configuration.getHighWaterMark()) {        
        //
        // We've gone over the HWM, enable fast expire
        //
        fastExpire = true;
        System.out.println("NBuckets (" + nbuckets + ") reached HWM (" + this.configuration.getHighWaterMark() + "), enabling fast expire.");
      } else if (fastExpire && (nbuckets < this.configuration.getLowWaterMark())) {
        //
        // We've gone back below the LWM, disable fast expire
        //
        fastExpire = false;
        System.out.println("NBuckets (" + nbuckets + ") fell below LWM (" + this.configuration.getLowWaterMark() + "), disabling fast expire.");
      } 
      
      //
      // We've reached the max, return now
      //

      if (nbuckets >= this.configuration.getMaxBuckets()) {
        return;
      }
    }

    long[] geocells = HHCodeHelper.toGeoCells(hhcode);
    
    for (int r: resolutions) {      
      if (r > this.configuration.getMaxResolution() || r < this.configuration.getMinResolution() || (r % 2 != 0)) {
        continue;
      }
      
      try {
        //
        // Attempt to retrieve buckets for the geocell
        //
        int[] buckets = geobuckets.get(geocells[(r >> 1) - 1]);
        
        //
        // Allocate buckets if they are null
        //
        if (null == buckets) {
          buckets = new int[recordSize];        
          geobuckets.put(geocells[(r >> 1) - 1], buckets);
        }
    
        //
        // Store value in the correct bucket
        //
        store(buckets, hhcode, timestamp, value, update);        
      } catch (ArrayIndexOutOfBoundsException aioobe) {        
      }
    }      
  }
  
  private void store(int[] buckets, long hhcode, long timestamp, int value, boolean update) {
    
    //
    // Ignore negative values
    //
    
    if (value < 0) {
      return;
    }
    
    //
    // Loop on all bucketspans
    //
    
    for (long bucketspan: bucketIndices.keySet()) {
      
      //
      // Compute bucket boundary (in s)
      //
      
      long boundary = timestamp - (timestamp % bucketspan);
      boundary /= 1000;
      
      //
      // Compute index for 'timestamp' at the given bucketspan
      //
      
      int index = bucketIndices.get(bucketspan) + (int) (boundary % bucketCounts.get(bucketspan));
    
      // Multiply by 4 (or 2) and shift by 1 (to account for the last modified timestamp)
      
      if (this.configuration.isCentroidEnabled()) {
        index <<= 2;
      } else {
        index <<= 1;
      }
      index++;
      
      //
      // Compare the first int of the bucket, if the boundary is >, ignore
      // the store.
      // If it's the same, update count (next int).
      // If it's <, clear the bucket, setting the new boundary
      //
      
      if (buckets[index] > boundary) {
        // This bucket already contains data that's more recent, ignore this datapoint
        continue;
      } else if (buckets[index] == boundary) {
        // If value is 0, it won't change either the value or the centroid, so do nothing, it's faster
        if (update && value > 0) {

          if (this.configuration.isCentroidEnabled()) {
            // Update centroid
            long centroidhhcode = buckets[index + 2] & 0xffffffffL;
            centroidhhcode <<= 32;
            centroidhhcode |= buckets[index + 3] & 0xffffffffL;
            
            long[] centroid = HHCodeHelper.splitHHCode(centroidhhcode, HHCodeHelper.MAX_RESOLUTION);
            long[] source = HHCodeHelper.splitHHCode(hhcode, HHCodeHelper.MAX_RESOLUTION);
            
            int currentvalue = buckets[index + 1];
            
            centroid[0] = centroid[0] * ((long) currentvalue) + ((long) value) * source[0];
            centroid[1] = centroid[1] * ((long) currentvalue) + ((long) value) * source[1];
            
            // Update value
            currentvalue += value;
            buckets[index + 1] = currentvalue;
            
            centroidhhcode = HHCodeHelper.buildHHCode(centroid[0] / (long) currentvalue, centroid[1] / (long) currentvalue, HHCodeHelper.MAX_RESOLUTION);

            // Store centroid
            buckets[index + 2] = (int) ((centroidhhcode >> 32) & 0xffffffffL);
            buckets[index + 3] = (int) (centroidhhcode & 0xffffffffL);            
          } else {
            // Update value
            buckets[index + 1] += value;            
          }
          
        } else if (!update) {
          buckets[index + 1] = value;
          
          if (this.configuration.isCentroidEnabled()) {
            // Set centroid to be the current hhcode
            buckets[index + 2] = (int) ((hhcode >> 32) & 0xffffffffL);
            buckets[index + 3] = (int) (hhcode & 0xffffffffL);            
          }
        }
      } else {
        buckets[index] = (int) boundary;
        buckets[index + 1] = value;
        
        if (this.configuration.isCentroidEnabled()) {
          // Set centroid to be the current hhcode
          buckets[index + 2] = (int) ((hhcode >> 32) & 0xffffffffL);
          buckets[index + 3] = (int) (hhcode & 0xffffffffL);          
        }
      }
      
      // Update buckets[0] with the current time
      buckets[0] = (int) (System.currentTimeMillis() / 1000);      
    }
  }
  
  @Override
  public double[] getData(long geocell, long timestamp, long bucketspan, int bucketcount, double timedecay) {
    
    double[] result = new double[3];
    result[2] = 0.0;
    
    //
    // If there are no suitable buckets or not enough of them, return 0.0
    //
    
    if (!bucketCounts.containsKey(bucketspan) || bucketCounts.get(bucketspan) < bucketcount) {
      return result;
    }
   
    int[] buckets;
    
    try {
      buckets = geobuckets.get(geocell);
    } catch (ArrayIndexOutOfBoundsException aioobe) {
      return result;
    }
    
    if (null == buckets) {
      return result;
    }
    
    //
    // If data has expired, remove it and return 0.0
    //
    
    if (doexpire) {
      long now = System.currentTimeMillis();
      long age = now - buckets[0] * 1000L;
      
      if (age > maxspan) {
        geobuckets.remove(geocell);
        return result;
      } else if (fastExpire && age > this.configuration.getFastExpireTTL()) {
        //
        // When fast expiring data, we still use the
        // content we just found.
        //
        geobuckets.remove(geocell);
      }
    }
    
    int index = bucketIndices.get(bucketspan);

    //
    // Compute first/last bucket boundary for the selected bucketspan
    //
    
    long hights = timestamp - (timestamp % bucketspan);
    long lowts = hights - (bucketcount - 1) * bucketspan;
    
    hights = hights / 1000;
    lowts = lowts / 1000;
    
    //
    // Scan the buckets of the given bucketspan,
    // returning the value from the ones which lie in [lowts,hights]
    //
    
    // Multiply index by 4 (2) and add 1
    
    if (this.configuration.isCentroidEnabled()) {
      index <<= 2;      
    } else {
      index <<= 1;      
    }
    index++;

    double value = 0.0;
    
    for (int i = 0; i < bucketCounts.get(bucketspan); i++) {
      long ts = buckets[index + i * bucketSize];

      if (ts >= lowts && ts <= hights) {        
        // Retrieve value
        int v = buckets[index + i * bucketSize + 1];
        
        // Apply decay
        value = value + v * Math.pow(timedecay, (1000 * Math.abs(ts - hights)) / bucketspan);
      }
    }
  
    //
    // Extract centroid lat/lon
    //
    
    if (this.configuration.isCentroidEnabled()) {
      long centroid = (buckets[index + 2] & 0xffffffffL) << 32;
      centroid |= buckets[index + 3] & 0xffffffffL;
      
      HHCodeHelper.stableGetLatLon(centroid , HHCodeHelper.MAX_RESOLUTION, result, 0);      
    } else {
      // Use geocell center
      long hhcode = (geocell & 0x0fffffffffffffffL) << 4;
               
      long[] coords = HHCodeHelper.center(hhcode, (int) (((geocell >> 60) & 0xf) << 1));
            
      result[0] = HHCodeHelper.toLat(coords[0]);
      result[1] = HHCodeHelper.toLon(coords[1]);
    }
    
    result[2] = value;
    
    return result;
  }
  
  @Override
  public void clear() {
    this.geobuckets.clear();
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    
    sb.append(totalBuckets);
    sb.append(" total buckets.\n");
    for (long bucketspan: bucketCounts.keySet()) {
      sb.append(bucketspan);
      sb.append(":");
      sb.append(bucketCounts.get(bucketspan));
      sb.append("\n");
    }
    
    sb.append("\n");
    sb.append("HeatMap configuration ");
    sb.append(this.configuration);
    sb.append("\n\n");
    sb.append("Currently storing ");
    sb.append(geobuckets.size());
    sb.append(" GeoCells.\n");
    return sb.toString();
  }
  
  @Override
  public void setDoExpire(boolean doexpire) {
    this.doexpire = doexpire;
  }
  
  @Override
  public long getBucketCount() {
    return geobuckets.size() * totalBuckets;
  }
  
  @Override
  public HeatMapConfiguration getConfiguration() {
    return this.configuration;
  }  
  
  @Override
  public TileBuilder getTileBuilder() {
    return this.builder;
  }
  
  @Override
  public void setConfiguration(HeatMapConfiguration conf) {
    this.configuration = conf;
  }
}
