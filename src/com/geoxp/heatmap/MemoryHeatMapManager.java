package com.geoxp.heatmap;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.management.monitor.Monitor;

import org.apache.tools.ant.taskdefs.Available;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.thrift.data.HeatMapAggregationType;
import com.geocoord.thrift.data.HeatMapConfiguration;

/**
 * Class that manages multiresolution geo data suitable for creating
 * heatmaps.
 */
public class MemoryHeatMapManager implements HeatMapManager {
  
  private static final Logger logger = LoggerFactory.getLogger(MemoryHeatMapManager.class);  
  
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
  
  private HeatMapManager parent = null;
  
  private List<HeatMapManager> children = new ArrayList<HeatMapManager>();
  
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
   * Current number of allocated buckets
   */
  private long nbuckets = 0L;
  
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
    
    bucketSize = 2 + (this.configuration.isCentroidEnabled() ? 2 : 0) + (HeatMapAggregationType.AVG.equals(this.configuration.getAggregationType()) ? 1 : 0);

    recordSize = 1 + bucketSize * totalBuckets;
    
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
    // and set fastExpire if there is a limit to the number of buckets
    //
    
    if (getMaxBuckets() > 0) {
      setFastExpire();

      //
      // Not enough available buckets, return now
      //
      
      if (availableBuckets() < totalBuckets) {
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
          updateBucketCount(totalBuckets);
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
      // Compute bucket boundary (in ms)
      //
      
      long boundary = timestamp - (timestamp % bucketspan);
      
      //
      // Compute index for 'timestamp' at the given bucketspan
      //
      
      int index = bucketIndices.get(bucketspan) + (int) ((boundary / bucketspan) % ((long) bucketCounts.get(bucketspan)));

      // Convert boundary to s
      boundary /= 1000;

      // Multiply by 4 (or 2) and shift by 1 (to account for the last modified timestamp)

      index *= bucketSize;
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
        // The bucket is the correct one for the timestamp.
        
        if (!update) {
          buckets[index + 1] = value;
          
          if (this.configuration.isCentroidEnabled()) {
            // Set centroid to be the current hhcode
            buckets[index + 2] = (int) ((hhcode >> 32) & 0xffffffffL);
            buckets[index + 3] = (int) (hhcode & 0xffffffffL);            
          }          
        }

        // Take care of the value
        switch(this.configuration.getAggregationType()) {
          case AVG:
            if (this.configuration.isCentroidEnabled() ) {
              // Update centroid
              long centroidhhcode = buckets[index + 2] & 0xffffffffL;
              centroidhhcode <<= 32;
              centroidhhcode |= buckets[index + 3] & 0xffffffffL;
              
              long[] centroid = HHCodeHelper.splitHHCode(centroidhhcode, HHCodeHelper.MAX_RESOLUTION);
              long[] source = HHCodeHelper.splitHHCode(hhcode, HHCodeHelper.MAX_RESOLUTION);
              
              long currentvalue = buckets[index + 1] * buckets[index + 4];
              
              centroid[0] = centroid[0] * ((long) currentvalue) + ((long) value) * source[0];
              centroid[1] = centroid[1] * ((long) currentvalue) + ((long) value) * source[1];
              
              // Update value
              currentvalue += value;
              buckets[index + 4]++;
              buckets[index + 1] = (int) (currentvalue / buckets[index + 4]);
              
              if (0 != currentvalue) {
                centroidhhcode = HHCodeHelper.buildHHCode(centroid[0] / currentvalue, centroid[1] / currentvalue, HHCodeHelper.MAX_RESOLUTION);
                
                // Store centroid
                buckets[index + 2] = (int) ((centroidhhcode >> 32) & 0xffffffffL);
                buckets[index + 3] = (int) (centroidhhcode & 0xffffffffL);                                        
              }
            } else {
              long currentvalue = buckets[index + 1] * buckets[index + 2];
              currentvalue += value;
              buckets[index + 2]++;
              buckets[index + 1] = (int) (currentvalue / buckets[index + 2]);              
            }
            break;
          case LAST:
            buckets[index + 1] = value;
            if (this.configuration.isCentroidEnabled()) {
              // Set centroid to be the current hhcode
              buckets[index + 2] = (int) ((hhcode >> 32) & 0xffffffffL);
              buckets[index + 3] = (int) (hhcode & 0xffffffffL);          
            }
            break;
          case MAX:
            if (value > buckets[index + 1]) {
              buckets[index + 1] = value;
              if (this.configuration.isCentroidEnabled()) {
                // Set centroid to be the current hhcode
                buckets[index + 2] = (int) ((hhcode >> 32) & 0xffffffffL);
                buckets[index + 3] = (int) (hhcode & 0xffffffffL);          
              }
            }
            break;
          case MIN:
            if (value < buckets[index + 1]) {
              buckets[index + 1] = value;
              if (this.configuration.isCentroidEnabled()) {
                // Set centroid to be the current hhcode
                buckets[index + 2] = (int) ((hhcode >> 32) & 0xffffffffL);
                buckets[index + 3] = (int) (hhcode & 0xffffffffL);          
              }
            }
            break;
          case SUM:    
            if (this.configuration.isCentroidEnabled() ) {
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
              buckets[index + 1] += value;
            }
            break;        
        }        
      } else {
        //
        // We need to initialize this bucket
        //
        
        buckets[index] = (int) boundary;
        buckets[index + 1] = value;
        
        if (this.configuration.isCentroidEnabled()) {
          // Set centroid to be the current hhcode
          buckets[index + 2] = (int) ((hhcode >> 32) & 0xffffffffL);
          buckets[index + 3] = (int) (hhcode & 0xffffffffL);
          if (HeatMapAggregationType.AVG.equals(this.configuration.getAggregationType())) {
            buckets[index + 4] = 1;
          }
        } else {
          if (HeatMapAggregationType.AVG.equals(this.configuration.getAggregationType())) {
            buckets[index + 2] = 1;
          }          
        }
      }      
    }
    
    // Update buckets[0] with the current time
    buckets[0] = (int) (System.currentTimeMillis() / 1000);      
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
        synchronized(this) {
          geobuckets.remove(geocell);
          updateBucketCount(-totalBuckets);
        }
        return result;
      } else if (fastExpire && age > getFastExpireTTL()) {
        //
        // When fast expiring data, we still use the
        // content we just found.
        //

        synchronized(this) {
          geobuckets.remove(geocell);
          updateBucketCount(-totalBuckets);
        }
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
    
    // Multiply index by bucketSize and add 1
    
    index *= bucketSize;
    index++;

    double value = 0.0;
    
    long latestTimestamp = 0;
    int latestIndex = -1;
    
    for (int i = 0; i < bucketCounts.get(bucketspan); i++) {
      long ts = buckets[index + i * bucketSize];

      if (ts >= lowts && ts <= hights) {        
        // Retrieve value
        int v = buckets[index + i * bucketSize + 1];
        
        // Apply decay
        value = value + v * Math.pow(timedecay, (1000 * Math.abs(ts - hights)) / bucketspan);
        
        // Record latest bucket so we can later extract its centroid
        if (ts > latestTimestamp) {
          latestIndex = i;
          latestTimestamp = ts;
        }
      }
    }
  
    //
    // Extract centroid lat/lon
    //
    
    if (this.configuration.isCentroidEnabled() && latestIndex >= 0) {
      long centroid = (buckets[index + (latestIndex * bucketSize) + 2] & 0xffffffffL) << 32;
      centroid |= (buckets[index + (latestIndex * bucketSize) + 3] & 0xffffffffL);
      
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
    updateBucketCount(-this.geobuckets.size() * totalBuckets);
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
    return nbuckets;
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
  
  @Override
  public void snapshot(OutputStream out, Collection<Integer> resolutions) throws IOException {
    long[] geocells = this.geobuckets.keys();
    
    boolean includeAll = false;
    
    if (null == resolutions || resolutions.isEmpty()) {
      includeAll = true;
    }

    PrintStream ps = new PrintStream(out);
    
    ps.append("CLEAR");
    ps.append("\n");
    ps.append("RESTORE");
    ps.append("\n");
    
    //
    // Sort the bucketspans from smallest to largest so we can STORE values when
    // loading a SNAPSHOT without loosing informations
    //
    
    List<Long> bucketspans = new ArrayList<Long>(bucketIndices.keySet());
    Collections.sort(bucketspans);
    
    //
    // Allocate buffer for geocell + record
    //
    
    byte[] buf = new byte[recordSize * 4 + 8];
    ByteBuffer bb = ByteBuffer.wrap(buf);
    bb.order(ByteOrder.BIG_ENDIAN);
    
    for (long geocell: geocells) {
      int resolution = (int) (((geocell >> 60) & 0x0f) << 1);
      
      if (resolutions.contains(resolution) || includeAll) {
        int[] buckets = geobuckets.get(geocell);
        
        if (null != buckets) {
          bb.rewind();
          bb.putLong(geocell);
          for (int bucket: buckets) {
            bb.putInt(bucket);
          }
          
          Base64.encode(buf, out);
          out.write('\n');
        }
      }        
    }
  }

  @Override
  public void restore(String encoded) {
    
    if (availableBuckets() < totalBuckets) {
      return;
    }
    
    //
    // Check that encoded data length is compatible with recordSize
    //
    
    // Compute size of each snapshot record in base64 bytes

    int expectedlen = recordSize * 4 * 8 + 8 * 8; // Compute size of records in bits
    expectedlen = (expectedlen / 6) + ((expectedlen % 6) == 0 ? 0 : ((expectedlen % 6) == 2 ? 3 : 2)); 
    
    if (encoded.length() != expectedlen) {
      return;
    }
        
    byte[] buf = Base64.decode(encoded);
    ByteBuffer bb = ByteBuffer.wrap(buf);
    
    long geocell = bb.getLong();
    
    int[] buckets = new int[recordSize];

    for (int i = 0; i < recordSize; i++) {
      buckets[i] = bb.getInt();
    }
        
    geobuckets.put(geocell, buckets);
    updateBucketCount(totalBuckets);
  }
  
  @Override
  public void expire(long threshold) {
    
    long nano = System.nanoTime();
    
    //
    // If threshold is 0, use maxspan
    //
    
    if (threshold <= 0) {
      threshold = maxspan;
    }
    
    //
    // Determine timestamp before which data will be
    // expired.
    //
    
    int cutoff = (int) ((System.currentTimeMillis() - threshold) / 1000);
    
    //
    // Loop over all the buckets
    //
    
    int precount = geobuckets.size() * bucketSize;
    
    for (long geocell: geobuckets.keys()) {
      int[] buckets = geobuckets.get(geocell);
      
      if (null != buckets && buckets[0] < cutoff) {
        geobuckets.remove(geocell);
        updateBucketCount(-totalBuckets);
      }
    }
    
    int postcount = geobuckets.size() * bucketSize;
    
    nano = System.nanoTime() - nano;
    
    logger.info("Expired " + (precount - postcount) + " buckets in " + (nano / 1000000.0D) + " ms, new bucket count is " + postcount);
  }
  
  @Override
  public void setParent(HeatMapManager manager) {
    this.parent = manager;    
  }
  
  @Override
  public void updateBucketCount(long count) {
    if (null != this.parent) {
      this.parent.updateBucketCount(count);
    }
    nbuckets += count;
  }
  
  @Override
  public long getFastExpireTTL() {
    if (null == this.parent) {
      return this.configuration.getFastExpireTTL();
    } else {
      return this.parent.getFastExpireTTL();
    }
  }
  
  @Override
  public long getMaxBuckets() {
    long confmax = this.configuration.getMaxBuckets();

    if (null != this.parent) {
      long parentmax = this.parent.getMaxBuckets();
      
      if (parentmax == 0) {
        return confmax;
      } else {
        if (confmax > 0 && confmax < parentmax) {
          return confmax;
        } else {
          return parentmax;
        }
      }      
    } else {
      return confmax;
    }
  }
  
  @Override
  public long getHighWaterMark() {
    long confmax = this.configuration.getMaxBuckets();

    if (null != this.parent) {
      long parentmax = this.parent.getMaxBuckets();
      
      if (parentmax == 0) {
        return this.configuration.getHighWaterMark();
      } else {
        if (confmax > 0 && confmax < parentmax) {
          return this.configuration.getHighWaterMark();
        } else {
          return this.parent.getHighWaterMark();
        }
      }      
    } else {
      return this.configuration.getHighWaterMark();
    }
  }
  
  @Override
  public long getLowWaterMark() {
    long confmax = this.configuration.getMaxBuckets();

    if (null != this.parent) {
      long parentmax = this.parent.getMaxBuckets();
      
      if (parentmax == 0) {
        return this.configuration.getLowWaterMark();
      } else {
        if (confmax > 0 && confmax < parentmax) {
          return this.configuration.getLowWaterMark();
        } else {
          return this.parent.getLowWaterMark();
        }
      }      
    } else {
      return this.configuration.getLowWaterMark();
    }
  }
  
  private void setFastExpire() {
    long n = this.getBucketCount();

    long confmax = this.configuration.getMaxBuckets();

    if (null != this.parent) {
      long parentmax = this.parent.getMaxBuckets();
      
      if (parentmax == 0) {
        n = this.parent.getBucketCount();
      } else {
        if (confmax > 0 && confmax < parentmax) {
          n = this.getBucketCount();
        } else {
          n = this.parent.getBucketCount();
        }
      }      
    } else {
      n = this.getBucketCount();
    }

    
    if (!fastExpire && getHighWaterMark() > 0 && n > getHighWaterMark()) {        
      //
      // We've gone over the HWM, enable fast expire
      //
      fastExpire = true;
      logger.info("NBuckets (" + nbuckets + ") reached HWM (" + getHighWaterMark() + "), enabling fast expire.");
    } else if (fastExpire && (n < getLowWaterMark())) {
      //
      // We've gone back below the LWM, disable fast expire
      //
      fastExpire = false;
      logger.info("NBuckets (" + nbuckets + ") fell below LWM (" + getLowWaterMark() + "), disabling fast expire.");
    }    
  }
  
  private long availableBuckets() {
    long confmax = this.configuration.getMaxBuckets();

    if (null != this.parent) {
      long parentmax = this.parent.getMaxBuckets();
      
      if (parentmax == 0) {
        if (0 == confmax) {
          return Long.MAX_VALUE;
        } else {
          return confmax - nbuckets;
        }
      } else {
        if (confmax > 0 && confmax < parentmax) {
          return confmax - nbuckets;
        } else {
          return parentmax - this.parent.getBucketCount();
        }
      }      
    } else {
      if (0 == confmax) {
        return Long.MAX_VALUE;
      } else {
        return confmax - nbuckets;
      }
    }
  }
  
  @Override
  public void addChild(HeatMapManager manager) {
    //
    // Don't allow circular deps
    //
    
    if (this == manager) {
      return;
    }

    //
    // Only allow one parent
    //
    if (null != manager.getParent()) {
      return;
    }
    
    manager.setParent(this);
    this.children.add(manager);
  }
  
  @Override
  public List<HeatMapManager> getChildren() {
    return Collections.unmodifiableList(this.children);
  }
  
  @Override
  public void removeChild(HeatMapManager manager) {
    this.children.remove(manager);
    manager.setParent(null);    
  }
  
  @Override
  public HeatMapManager getParent() {
    return this.parent;
  }
}
