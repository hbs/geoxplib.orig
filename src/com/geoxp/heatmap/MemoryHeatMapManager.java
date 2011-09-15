package com.geoxp.heatmap;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.geocoord.geo.HHCodeHelper;

/**
 * Class that manages multiresolution geo data suitable for creating
 * heatmaps.
 */
public class MemoryHeatMapManager implements HeatMapManager {
  
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
   * Each bucket is a pair of ints, the first one being the
   * timestamp and the second one the value.
   */
  private final TLongObjectHashMap<byte[]> geobuckets;
  
  /**
   * Minimum resolution, standardized in [1->15]
   */
  private final int minr;
  
  /**
   * Maximum resolution, standardized in [1->15]
   */
  private final int maxr;
  
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
   * Maximum number of allowed buckets
   */
  private long maxBuckets = 0;
  
  /**
   * If the bucket count goes over bucketsHighWaterMark
   * we switch to fast expire mode where we will use a
   * lower TTL
   */
  private long bucketsHighWaterMark = 0;
  
  /**
   * When in fast expire mode, if the number of buckets falls
   * below this limit, we exit fast expire mode.
   */
  private long bucketsLowWaterMark = 0;
  
  /**
   * Flag indicating whether or not to perform
   * fast expires.
   */
  private boolean fastExpire = false;
  
  /**
   * TTL to apply when performing fast expires
   */
  private long fastExpireTTL = 0L;
  
  /**
   * Size of records in bytes
   */
  private final int recordSize;

  /**
   * Create an instance of HeatMapManager which will manage
   * data in the given buckets.
   * 
   * @param buckets Map of bucketspan (in ms) to bucket count.
   * @param minresolution Coarsest resolution considered (even 2->30)
   * @param maxresolution Finest resolution considered (even 2->30)
   */
  public MemoryHeatMapManager(Map<Long,Integer> buckets, int minresolution, int maxresolution) {
    int index = 0;
    
    bucketIndices = new HashMap<Long, Integer>();
    bucketCounts = new HashMap<Long, Integer>();
    
    for (long bucketspan: buckets.keySet()) {
      bucketCounts.put(bucketspan, buckets.get(bucketspan));
      bucketIndices.put(bucketspan, index);
      
      if (bucketspan * buckets.get(bucketspan) > maxspan) {
        maxspan = bucketspan * buckets.get(bucketspan);
      }
      
      index += buckets.get(bucketspan);
    }
    
    totalBuckets = index;
    
    //
    // Set record size:
    //
    // 1 int (4 bytes) for last update timestamp
    // 2 int (2*4 bytes) for each (timestamp,value) tuple
    //
    
    recordSize = 4 * (1 + 2 * totalBuckets);

    geobuckets = new TLongObjectHashMap<byte[]>();
    
    this.default_resolutions = new HashSet<Integer>();
    
    for (int r = minresolution; r <= maxresolution; r+=2) {
      default_resolutions.add(r);
    }
    
    minr = minresolution;
    maxr = maxresolution;
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
    
    if (maxBuckets > 0) {
      long nbuckets = geobuckets.size() * totalBuckets;

      if (!fastExpire && nbuckets > bucketsHighWaterMark) {        
        //
        // We've gone over the HWM, enable fast expire
        //
        fastExpire = true;
        System.out.println("NBuckets (" + nbuckets + ") reached HWM (" + bucketsHighWaterMark + "), enabling fast expire.");
      } else if (fastExpire && (nbuckets < bucketsLowWaterMark)) {
        //
        // We've gone back below the LWM, disable fast expire
        //
        fastExpire = false;
        System.out.println("NBuckets (" + nbuckets + ") fell below LWM (" + bucketsLowWaterMark + "), disabling fast expire.");
      } 
      
      //
      // We've reached the max, return now
      //

      if (nbuckets >= maxBuckets) {
        return;
      }
    }

    long[] geocells = HHCodeHelper.toGeoCells(hhcode);
    
    for (int r: resolutions) {
      
      if (r > maxr || r < minr) {
        continue;
      }
      
      //
      // Attempt to retrieve buckets for the geocell
      //
      byte[] buckets = geobuckets.get(geocells[(r >> 1) - 1]);
      
      //
      // Allocate buckets if they are null
      //
      if (null == buckets) {
        buckets = new byte[recordSize];
        geobuckets.put(geocells[(r >> 1) - 1], buckets);
      }
  
      // TODO(hbs): We should have an expiration mechanism so geocells not
      //            updated for a while simply get GCed.
      
      //
      // Store value in the correct bucket
      //
      store(buckets, timestamp, value, update);
    }      
  }
  
  private void store(byte[] buckets, long timestamp, int value, boolean update) {
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
    
      // Multiply by 2 and shift by 1 (to account for the last modified timestamp)
      
      index <<= 1;
      index++;
      
      //
      // Compare the first int of the bucket, if the boundary is >, ignore
      // the store.
      // If it's the same, update count (next int).
      // If it's <, clear the bucket, setting the new boundary
      //
      
      ByteBuffer bb = ByteBuffer.wrap(buckets);
      bb.order(ByteOrder.BIG_ENDIAN);
      
      if (bb.getInt(index * 4) > boundary) {
        continue;
      } else if (bb.getInt(index * 4) == boundary) {
        if (update) {
          bb.putInt(4 * (index + 1), bb.getInt(4 * (index + 1)) + value);
        } else {
          bb.putInt(4 * (index + 1), value);
        }
      } else {
        bb.putInt(index * 4, (int) boundary);
        bb.putInt(4 * (index + 1), value);
      }
      
      // Update buckets[0] with the current time
      bb.putInt(0, (int) (System.currentTimeMillis() / 1000));
    }
  }
  
  @Override
  public double getData(long geocell, long timestamp, long bucketspan, int bucketcount, double timedecay) {
    
    //
    // If there are no suitable buckets or not enough of them, return 0.0
    //
    
    if (!bucketCounts.containsKey(bucketspan) || bucketCounts.get(bucketspan) < bucketcount) {
      return 0.0;
    }
   
    byte[] bucket = geobuckets.get(geocell);
    
    if (null == bucket) {
      return 0.0;
    }
    
    ByteBuffer bb = ByteBuffer.wrap(bucket);
    bb.order(ByteOrder.BIG_ENDIAN);
    
    //
    // If data has expired, remove it and return 0.0
    //
    
    if (doexpire) {
      long now = System.currentTimeMillis();
      long age = now - (bb.getInt(0) * 1000L);
      
      if (age > maxspan) {
        geobuckets.remove(geocell);
        return 0.0;
      } else if (fastExpire && age > fastExpireTTL) {
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
    
    // Multiply index by 2 and add 1
    index <<= 1;
    index++;

    double value = 0.0;
    
    for (int i = 0; i < bucketCounts.get(bucketspan); i++) {
      long ts = bb.getInt(4 * (index + i * 2));

      if (ts >= lowts && ts <= hights) {        
        // Retrieve value
        int v = bb.getInt(4 * (index + i * 2 + 1));
        
        // Apply decay
        value = value + v * Math.pow(timedecay, (1000 * Math.abs(ts - hights)) / bucketspan);
      }
    }
  
    return value;
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
    sb.append("GeoCells resolutions ");
    sb.append(minr << 1);
    sb.append(":");
    sb.append(maxr << 1);
    sb.append("\n\n");
    sb.append("Currently storing ");
    sb.append(geobuckets.size());
    sb.append(" GeoCells.\n");
    return sb.toString();
  }
  
  public int getMaxResolution() {
    return maxr;
  }
  
  public int getMinResolution() {
    return minr;
  }
  
  @Override
  public void setDoExpire(boolean doexpire) {
    this.doexpire = doexpire;
  }
  
  public void configureLimits(long max, long hwm, long lwm, long ttl) {
    this.maxBuckets = max;
    this.bucketsHighWaterMark = hwm;
    this.bucketsLowWaterMark = lwm;
    this.fastExpireTTL = ttl;
  }
  
  @Override
  public long getBucketCount() {
    return geobuckets.size() * totalBuckets;
  }
}
