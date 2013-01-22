//package com.geoxp.heatmap;
//
//import java.math.BigInteger;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//
//import com.geocoord.geo.HHCodeHelper;
//import com.sleepycat.je.Database;
//import com.sleepycat.je.DatabaseEntry;
//import com.sleepycat.je.LockMode;
//
///**
// * Class that manages multiresolution geo data suitable for creating
// * heatmaps and stores it in a Berkeley DB.
// */
//public class BDBHeatMapManager implements HeatMapManager {
//
//  /**
//   * Map from bucketspan to index in the bucket array.
//   */
//  private final Map<Long,Integer> bucketIndices;
//  
//  /**
//   * Number of buckets per bucketspan
//   */
//  private final Map<Long, Integer> bucketCounts;
//  
//  /**
//   * Total number of buckets managed per geo-cell
//   */
//  private final int totalBuckets;
//  
//  private long nbuckets = 0;
//  
//  /**
//   * Resolution prefixed for geo-cells, indexed by resolution (even 2->30)
//   */
//  private static final long[] RESOLUTION_PREFIXES = { 0x0L,
//    0x0L, 0x1000000000000000L, // 2
//    0x0L, 0x2000000000000000L, // 4
//    0x0L, 0x3000000000000000L, // 6
//    0x0L, 0x4000000000000000L, // 8
//    0x0L, 0x5000000000000000L, // 10
//    0x0L, 0x6000000000000000L, // 12
//    0x0L, 0x7000000000000000L, // 14
//    0x0L, 0x8000000000000000L, // 16
//    0x0L, 0x9000000000000000L, // 18
//    0x0L, 0xa000000000000000L, // 20
//    0x0L, 0xb000000000000000L, // 22
//    0x0L, 0xc000000000000000L, // 24
//    0x0L, 0xd000000000000000L, // 26
//    0x0L, 0xe000000000000000L, // 28
//    0x0L, 0xf000000000000000L, // 30
//  };
//  
//  private final String dbname;
//  
//  /**
//   * Minimum resolution
//   */
//  private final int minr;
//  
//  /**
//   * Maximum resolution
//   */
//  private final int maxr;
//  
//  /**
//   * Default resolutions to use if none were specified.
//   */
//  private final HashSet<Integer> default_resolutions;
//  
//  /**
//   * Length of longer bucketspan * bucketcount
//   */
//  private long maxspan;
//  
//  /**
//   * Should we expire data as we read it
//   */
//  private boolean doexpire = true;
//  
//  /**
//   * Maximum number of allowed buckets
//   */
//  private long maxBuckets = 0;
//  
//  /**
//   * If the bucket count goes over bucketsHighWaterMark
//   * we switch to fast expire mode where we will use a
//   * lower TTL
//   */
//  private long bucketsHighWaterMark = 0;
//  
//  /**
//   * When in fast expire mode, if the number of buckets falls
//   * below this limit, we exit fast expire mode.
//   */
//  private long bucketsLowWaterMark = 0;
//  
//  /**
//   * Flag indicating whether or not to perform
//   * fast expires.
//   */
//  private boolean fastExpire = false;
//  
//  /**
//   * TTL to apply when performing fast expires
//   */
//  private long fastExpireTTL = 0L;
//  
//  /**
//   * Size of records in bytes
//   */
//  private final int recordSize;
//  
//  /**
//   * Create an instance of HeatMapManager which will manage
//   * data in the given buckets.
//   * 
//   * @param buckets Map of bucketspan (in ms) to bucket count.
//   * @param minresolution Coarsest resolution considered (even 2->30)
//   * @param maxresolution Finest resolution considered (even 2->30)
//   */
//  public BDBHeatMapManager(String dbname, Map<Long,Integer> buckets, int minresolution, int maxresolution) {
//    int index = 0;
//    
//    bucketIndices = new HashMap<Long, Integer>();
//    bucketCounts = new HashMap<Long, Integer>();
//  
//    maxspan = 0;
//    
//    for (long bucketspan: buckets.keySet()) {
//      bucketCounts.put(bucketspan, buckets.get(bucketspan));
//      bucketIndices.put(bucketspan, index);
//      
//      if (bucketspan * buckets.get(bucketspan) > maxspan) {
//        maxspan = bucketspan * buckets.get(bucketspan);
//      }
//      
//      index += buckets.get(bucketspan);
//    }
//    
//    totalBuckets = index;
//    
//    //
//    // Set record size:
//    //
//    // 1 int (4 bytes) for last update timestamp
//    // 2 int (2*4 bytes) for each (timestamp,value) tuple
//    //
//    
//    recordSize = 4 * (1 + 2 * totalBuckets);
//
//    this.dbname = dbname;
//    
//    this.default_resolutions = new HashSet<Integer>();
//    
//    for (int r = minresolution; r <= maxresolution; r+=2) {
//      default_resolutions.add(r);
//    }
//    
//    minr = minresolution;
//    maxr = maxresolution;        
//  }
//  
//  private Database getDb() {
//    return BDBManager.getDB(this.dbname);
//  }
//
//  @Override
//  public void store(double lat, double lon, long timestamp, int value, boolean update) {
//    store(lat, lon, timestamp, value, update, default_resolutions);
//  }
//  
//  @Override
//  public void store(double lat, double lon, long timestamp, int value, boolean update, Collection<Integer> resolutions) {
//    long hhcode = HHCodeHelper.getHHCodeValue(lat, lon);
//    store(hhcode, timestamp, value, update, resolutions);
//  }
//    
//  @Override
//  public void store(long hhcode, long timestamp, int value, boolean update) {
//    store(hhcode, timestamp, value, update, default_resolutions);
//  }
//  
//  @Override
//  public void store(long hhcode, long timestamp, int value, boolean update, Collection<Integer> resolutions) {
//    
//    //
//    // Retrieve Database
//    //
//    
//    Database db = getDb();
//
//    //
//    // Check if we've reached the maximum number of buckets (if set)
//    // or the high/low watermark
//    //
//    
//    if (maxBuckets > 0) {
//      nbuckets = db.count() * totalBuckets;
//
//      if (!fastExpire && nbuckets > bucketsHighWaterMark) {        
//        //
//        // We've gone over the HWM, enable fast expire
//        //
//        fastExpire = true;
//        System.out.println("[" + this.dbname + "] NBuckets (" + nbuckets + ") reached HWM (" + bucketsHighWaterMark + "), enabling fast expire.");
//      } else if (fastExpire && (nbuckets < bucketsLowWaterMark)) {
//        //
//        // We've gone back below the LWM, disable fast expire
//        //
//        fastExpire = false;
//        System.out.println("[" + this.dbname + "] NBuckets (" + nbuckets + ") fell below LWM (" + bucketsLowWaterMark + "), disabling fast expire.");
//      } 
//      
//      //
//      // We've reached the max, return now
//      //
//
//      if (nbuckets >= maxBuckets) {
//        return;
//      }
//    }
//    
//    //
//    // Compute all geocells
//    //
//    
//    long[] geocells = HHCodeHelper.toGeoCells(hhcode);
//    
//    for (int r: resolutions) {
//          
//      if (r > maxr || r < minr) {
//        continue;
//      }
//
//      //
//      // Attempt to retrieve buckets for the geocell
//      //
//      
//      DatabaseEntry key = new DatabaseEntry(new BigInteger(Long.toString(geocells[(r >> 1) - 1])).toByteArray()); 
//      DatabaseEntry data = new DatabaseEntry();
//      
//      db.get(null, key, data, LockMode.DEFAULT);
//      
//      if (data.getSize() != recordSize) {
//        data = new DatabaseEntry(new byte[recordSize]);
//      }
//      
//      //
//      // Store value in the correct bucket
//      //
//      store(key, data, timestamp, value, update);
//    }      
//  }
//  
//  private void store(DatabaseEntry key, DatabaseEntry data, long timestamp, int value, boolean update) {
//    
//    //
//    // Wrap data in a ByteBuffer
//    //
//    
//    ByteBuffer bb = ByteBuffer.wrap(data.getData());
//    
//    //
//    // Loop on all bucketspans
//    //
//    
//    for (long bucketspan: bucketIndices.keySet()) {
//      
//      //
//      // Compute bucket boundary (in s)
//      //
//      
//      long boundary = timestamp - (timestamp % bucketspan);
//      boundary /= 1000;
//      
//      //
//      // Compute index for 'timestamp' at the given bucketspan
//      //
//      
//      int index = bucketIndices.get(bucketspan) + (int) (boundary % bucketCounts.get(bucketspan));
//    
//      // Multiply by 2 and shift by 1 (to account for the last modified timestamp)
//      
//      index <<= 1;
//      index++;
//      
//      //
//      // Compare the first int of the bucket, if the boundary is >, ignore
//      // the store.
//      // If it's the same, update count (next int).
//      // If it's <, clear the bucket, setting the new boundary
//      //
//      
//      int ts = bb.getInt(index * 4);
//      
//      if (ts > boundary) {
//        continue;
//      } else if (ts == boundary) {
//        if (update) {
//          value += bb.get(4 * (index + 1));
//        }
//        bb.putInt(4 * (index + 1), value);
//      } else {
//        bb.putInt(4 * index, (int) boundary);
//        bb.putInt(4 * (index + 1), value);
//      }
//      
//      // Update buckets[0] with the current time
//      bb.putInt(0, (int) (System.currentTimeMillis() / 1000));
//    }
//    
//    //
//    // Store record in the db
//    //
//    
//    getDb().put(null, key, data);
//  }
//  
//  @Override
//  public double[] getData(long geocell, long timestamp, long bucketspan, int bucketcount, double timedecay) {
//    
//    // FIXME(hbs)
//    double[] result = null;
//    
//    return result;
//    
//    /*
//    //
//    // If there are no suitable buckets or not enough of them, return 0.0
//    //
//    
//    if (!bucketCounts.containsKey(bucketspan) || bucketCounts.get(bucketspan) < bucketcount) {
//      return 0.0;
//    }
//    
//    //
//    // Retrieve buckets
//    //
//    
//    DatabaseEntry key = new DatabaseEntry(new BigInteger(Long.toString(geocell)).toByteArray());
//    DatabaseEntry data = new DatabaseEntry();
//    
//    getDb().get(null, key, data, LockMode.DEFAULT);
//    
//    //
//    // If we don't have data, return 0.0
//    //
//    
//    if (0 == data.getSize()) {
//      return 0.0;
//    }
//
//    ByteBuffer bb = ByteBuffer.wrap(data.getData());
//    bb.order(ByteOrder.BIG_ENDIAN);
//    
//    //
//    // If data has expired, remove it and return 0.0
//    //
//    
//    if (doexpire) {
//      long now = System.currentTimeMillis();
//      long age = now - (bb.getInt(0) * 1000L);
//      
//      if (age > maxspan) {
//        getDb().delete(null, key);
//        return 0.0;
//      } else if (fastExpire && age > fastExpireTTL) {
//        //
//        // When fast expiring data, we still use the
//        // content we just found.
//        //
//        getDb().delete(null, key);
//      }
//    }
//    
//    int index = bucketIndices.get(bucketspan);
//    
//    //
//    // Compute first/last bucket boundary for the selected bucketspan
//    //
//    
//    long hights = timestamp - (timestamp % bucketspan);
//    long lowts = hights - (bucketcount - 1) * bucketspan;
//    
//    hights = hights / 1000;
//    lowts = lowts / 1000;
//    
//    //
//    // Scan the buckets of the given bucketspan,
//    // returning the value from the ones which lie in [lowts,hights]
//    //
//    
//    // Multiply index by 2 and add 1
//    index <<= 1;
//    index++;
//
//    double value = 0.0;
//    
//    for (int i = 0; i < bucketCounts.get(bucketspan); i++) {
//      long ts = bb.getInt(4 * (index + i * 2));
//
//      if (ts >= lowts && ts <= hights) {        
//        // Retrieve value
//        int v = bb.getInt(4 * (index + i * 2 + 1));
//        
//        // Apply decay
//        value = value + v * Math.pow(timedecay, (1000 * Math.abs(ts - hights)) / bucketspan);
//      }
//    }
//  
//    return value;
//    */
//  }
//  
//  public int getMaxResolution() {
//    return maxr;
//  }
//  
//  public int getMinResolution() {
//    return minr;
//  }
//  
//  public void setDoExpire(boolean doexpire) {
//    this.doexpire = doexpire;
//  }
//  
//  public void configureLimits(long max, long hwm, long lwm, long ttl) {
//    this.maxBuckets = max;
//    this.bucketsHighWaterMark = hwm;
//    this.bucketsLowWaterMark = lwm;
//    this.fastExpireTTL = ttl;
//  }
//  
//  @Override
//  public long getBucketCount() {
//    return nbuckets;
//  }
//}
