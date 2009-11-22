package com.geocoord.geo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HHCode {
    
    private static final Log logger = LogFactory.getLog(HHCode.class);
    
    //
    // The Earth Radius used here is the major semi-axis of the WGS84
    // Ellipsoid, 6378137 meters.
    //
    
    public static final long EARTH_RADIUS = 6378137L;
        
    //
    // Maximum level of precision and current level of precision
    //
    
    public static final int MAX_HHLEVEL = 32;
    private static final int HHLEVEL = 32;
    
    private int level = HHLEVEL;
    
    private long lat = 0L;
    private long lon = 0L;
    
    private long hhcode = 0L;
    
    //
    // Latitute and longitude span of HHCode unit at MAX_HHLEVEL
    //
    
    private static double latStep = (double) 180.0 / ((1L << MAX_HHLEVEL) - 1);
    private static double lonStep = (double) 360.0 / ((1L << MAX_HHLEVEL) - 1);
    
    /**
     * Constructor for lat/lon expressed in degrees
     * 
     * @param lat
     * @param lon
     */
    
    public HHCode (double lat, double lon) {
        this(lat,lon,false);
    }
        
    public HHCode (long lat, long lon) {
      this.level = MAX_HHLEVEL;
      this.hhcode = buildHHCode(lat, lon, this.level);
      this.lat = lat;
      this.lon = lon;
    }
    
    public HHCode (double lat, double lon, boolean radians) {
        
        this.level = MAX_HHLEVEL;
                
        //
        // Convert lat/lon to degrees if needed 
        //
        
        double degLat = radians ? (lat * 180.0 / Math.PI) : lat;
        double degLon = radians ? (lon * 180.0 / Math.PI) : lon;
        
        this.lat = degToHHLat(degLat);
        this.lon = degToHHLon(degLon);

        this.hhcode = buildHHCode(this.lat, this.lon, this.level);
    }
    
    public HHCode (long hh) {
      this.hhcode = hh;
      this.level = MAX_HHLEVEL;
      this.lat = 0L;
      this.lon = 0L;
      
      for (int i = this.level - 1; i >= 0; i--) {
        this.lat <<= 1;
        this.lat |= 0x1L & (this.hhcode >> (1 + (i << 1)));
            this.lon <<= 1;
            this.lon |= 0x1L & (this.hhcode >> (i << 1));
      }
    }
    
    private static long degToHHLat (double lat) {
        //return (long) Math.floor((lat + 90.0) / latStep[level-1]);
        return (long) Math.floor((lat + 90.0) / latStep);
    }
    
    private static long degToHHLon (double lon) {
        //return (long) Math.floor((lon + 180.0) / lonStep[level-1]);
        return (long) Math.floor((lon + 180.0) / lonStep);
    }

    public String toString () {
        return toString(this.level);
    }
    
    public String toString(int level) {
        
      if (level % 2 == 1) {
        level++;
      }

      if (level > MAX_HHLEVEL) {
        level = MAX_HHLEVEL;
      }
      
      StringBuilder sb = new StringBuilder();

      sb.append("000000000000000");
      sb.append(Long.toHexString(this.hhcode));
      sb.delete(0, sb.length() - (this.level >> 1));
      sb.append("0000000000000000");
      sb.delete(level >> 1, sb.length());
      
      /*
      int nibbles = level >> 1;
             
      for (int i = 0; i < nibbles; i++) {
        sb.append("0");
      }
    
      sb.append(Long.toHexString(this.hhcode));
      
      sb.delete(0, sb.length() - (this.level >> 1));
      
      sb.delete(level >> 1, sb.length());
      */
      
      return sb.toString();         
    }
    
    public long value() {
        return this.hhcode;
    }
    
    public long value(int level) {
      
      //
      // Make sure level is even and less than 32
      //
      
      level = (level % 0x21) &0xfe;
      
      long hh = this.hhcode >> ((this.level - level) << 1);
    
      if (level != 32) {
        hh &= ((1L << (level << 1)) - 1L);
      }
      return hh;
    }
    
    public long getLatLong() {
      return this.lat;
    }
    
    public long getLonLong() {
      return this.lon;
    }
    
    public double getLat() {
      return this.lat * latStep - 90.0;
    }
    
    public double getLon() {
      return this.lon * lonStep - 180.0;
    }
    
    /**
     * Build a String of zones covering the given rectangle.
     * 
     * @param minLat
     * @param minLon
     * @param maxLat
     * @param maxLon
     * @return
     */
    public static String bbox(double minLat, double minLon, double maxLat, double maxLon) {
      
      //
      // Take care of the case when the bbox contains the international date line
      //
      
      if (minLon > maxLon) {
        // If sign differ, consider we crossed the IDL
        if (minLon * maxLon <= 0) {
          StringBuilder sb = new StringBuilder();
          sb.append(HHCode.bbox(minLat,minLon,maxLat,180.0));
          sb.append(" ");
          sb.append(HHCode.bbox(minLat,-180.0,maxLat,maxLon));
          return sb.toString();          
        } else {
          // Reorder minLat/minLon/maxLat/maxLon as they got tangled for an unknown reason!
          double lat = Math.max(minLat,maxLat);
          minLat = Math.min(minLat, maxLat);
          maxLat = lat;
          
          double lon = Math.max(minLon,maxLon);
          minLon = Math.min(minLon,maxLon);
          maxLon = lon;
        }
      }
      
      HHCode ll = new HHCode(minLat, minLon);
      HHCode ur = new HHCode(maxLat, maxLon);
      
      // Compute delta in latitude and longitude
      
      long deltaLat = ur.getLatLong() - ll.getLatLong();
      long deltaLon = ur.getLonLong() - ll.getLonLong();

      // Extract log2 of the deltas and keep smallest
      // This log is the resolution we must use to have cells that are just a little smaller than 
      // the one we try to cover. We could use 'ceil' instead of 'floor' to have cells a little bigger.
            
      int log = (int) Math.floor(Math.min(Math.log(deltaLat), Math.log(deltaLon))/Math.log(2.0));
      // Make log an even number.
      log = log & 0xfe;
      
      
      long startLat = ll.getLatLong() & (0xffffffff ^ ((1L << log) - 1));
      long startLon = ll.getLonLong() & (0xffffffff ^ ((1L << log) - 1));
      
      long lat = startLat;
      long lon = startLon;
      
      //
      // We then generate the list of cells needed to cover the bbox
      // starting from the lowest left corner and stepping up 1 << log at a time.
      //
      
      StringBuilder sb = new StringBuilder();
      
      while (lat <= ur.getLatLong()) {
        lon = startLon;
        
        while (lon <= ur.getLonLong()) {
          HHCode h = new HHCode(lat, lon);
          String hs = h.toString(32 - log);
          
          if (sb.indexOf(hs) == -1) {
            if (sb.length() > 0) {
              sb.append(" ");
            }
            sb.append(hs);
          }
          lon += (1L << log);
        }
        lat += (1L << log);
      }
      
     
      return sb.toString();
    }

    /**
     * Returns the bounds of the search zones covering the given bounding box.
     * This is a list of either 4 or 8 coordinates (2 or 4 pairs or lat/lon) depending
     * on whether or not the bbox crosses the international date line.
     */
    public static double[] bboxToBounds(double minLat, double minLon, double maxLat, double maxLon) {
      //
      // Take care of the case when the bbox contains the international date line
      //
      
      if (minLon > maxLon) {
        double[] bounds = new double[8];
        
        double[] bounds1 = HHCode.bboxToBounds(minLat,minLon,maxLat,180.0);
        double[] bounds2 = HHCode.bboxToBounds(minLat,-180.0,maxLat,maxLon);
      
        System.arraycopy(bounds1, 0, bounds, 0, 4);
        System.arraycopy(bounds2, 0, bounds, 4, 4);
        
        return bounds;
      }
      
      HHCode ll = new HHCode(minLat, minLon);
      HHCode ur = new HHCode(maxLat, maxLon);
      
      // Compute delta in latitude and longitude
      
      long deltaLat = ur.getLatLong() - ll.getLatLong();
      long deltaLon = ur.getLonLong() - ll.getLonLong();
      
      // Extract log2 of the deltas and keep smallest
      // This log is the resolution we must use to have cells that are just a little smaller than 
      // the one we try to cover. We could use 'ceil' instead of 'floor' to have cells a little bigger.
            
      int log = (int) Math.floor(Math.min(Math.log(deltaLat), Math.log(deltaLon))/Math.log(2.0));
      // Make log an even number.
      log = log & 0xfe;
      
      long startLat = ll.getLatLong() & (0xffffffff ^ ((1L << log) - 1));
      long startLon = ll.getLonLong() & (0xffffffff ^ ((1L << log) - 1));
      
      long lat = startLat;
      long lon = startLon;
      
      //
      // We then generate the list of cells needed to cover the bbox
      // starting from the lowest left corner and stepping up 1 << log at a time.
      //
      
      long latMin = 0xffffffffL;
      long latMax = 0;
      long lonMin = 0xffffffffL;
      long lonMax = 0;
      
      while (lat <= ur.getLatLong()) {
        lon = startLon;
        
        while (lon <= ur.getLonLong()) {
          
          if (lat < latMin) {
            latMin = lat;
          }
          if (lon < lonMin) {
            lonMin = lon;
          }
          
          long endLat = lat + ((1L << log) - 1);
          long endLon = lon + ((1L << log) - 1);
          
          if (endLat > latMax) {
            latMax = endLat;
          }
          if (endLon > lonMax) {
            lonMax = endLon;
          }
          lon += (1L << log);
        }
        lat += (1L << log);
      }

      double[] bounds = new double[4];
      
      bounds[0] = (latMin * latStep) - 90.0;
      bounds[1] = (lonMin * lonStep) - 180.0;
      bounds[2] = (latMax * latStep) - 90.0;
      bounds[3] = (lonMax * lonStep) - 180.0;
      
      return bounds;
    }
    
    
    public static List<Double> geoboxes(double minLat, double minLon, double maxLat, double maxLon) {
      //
      // Take care of the case when the bbox contains the international date line
      //
      
      if (minLon > maxLon) {
        List<Double> l = HHCode.geoboxes(minLat,minLon,maxLat,180.0);
        l.addAll(HHCode.geoboxes(minLat,-180.0,maxLat,maxLon));
        return l;
      }

      HHCode ll = new HHCode(minLat, minLon);
      HHCode ur = new HHCode(maxLat, maxLon);
      
      // Compute delta in latitude and longitude
      
      long deltaLat = ur.getLatLong() - ll.getLatLong();
      long deltaLon = ur.getLonLong() - ll.getLonLong();
      
      System.out.println("deltaLat=" + deltaLat);
      System.out.println("deltaLon=" + deltaLon);
      
      // Extract log2 of the deltas and keep smallest
      // This log is the resolution we must use to have cells that are just a little smaller than 
      // the one we try to cover. We could use 'ceil' instead of 'floor' to have cells a little bigger.
            
      int log = (int) Math.floor(Math.min(Math.log(deltaLat), Math.log(deltaLon))/Math.log(2.0));
      // Make log an even number.
      log = log & 0xfe;
      
      System.out.println("Log=" + log);
      long startLat = ll.getLatLong() & (0xffffffff ^ ((1L << log) - 1));
      long startLon = ll.getLonLong() & (0xffffffff ^ ((1L << log) - 1));
      
      long lat = startLat;
      long lon = startLon;
      
      System.out.println("startLon=" + lon);
      //
      // We then generate the list of cells needed to cover the bbox
      // starting from the lowest left corner and stepping up 1 << log at a time.
      //
      
      StringBuilder sb = new StringBuilder();
      
      List<Double> geoboxes = new ArrayList<Double>();
      
      while (lat <= ur.getLatLong()) {
        lon = startLon;
        
        while (lon <= ur.getLonLong()) {
          HHCode h = new HHCode(lat, lon);
          String hs = h.toString(32 - log);
     
          HHCode bbll = new HHCode(new BigInteger((hs + "000000000000000").substring(0,16),16).longValue());
          HHCode bbur = new HHCode(new BigInteger((hs + "fffffffffffffff").substring(0,16), 16).longValue());
          
          geoboxes.add(bbll.getLat());
          geoboxes.add(bbll.getLon());
          geoboxes.add(bbur.getLat());
          geoboxes.add(bbur.getLon());

          lon += (1L << log);
        }
        lat += (1L << log);
      }
      
      return geoboxes;
    }
    
    /**
     * Compute steps to go from a to b as fast as possible by adding only powers
     * of two.
     * 
     * @param a
     * @param b
     * @return
     */
    public static List<Long> steps(long a, long b) {
      
      // Initialize the current value
      long v = a;
      
      List<Long> steps = new ArrayList<Long>();
      steps.add(a);
      
      int maxp = 32;
      
      while (v < b) {
        
        // Find the largest power of two p so that v % p == 0

        int i;
      
        for (i = maxp - 1; i > 0; i--) {
          if ((v % (1L << i)) == 0) {
            break;
          }
        }
      
        // Add 2**i to a until v % 2**(i+1) == 0
      
        long p = 1L << i;
        long pp = p << 1;

        // If going up (maxp still at 32) then add p until v % pp == 0
        // If going down (maxp != 32) then add p until v > b
        
        while((maxp != 32) || (v % pp) != 0) {
          v += p;
          if (v > b) {
            v -= p;
            maxp = i;
            break;
          } else {
            //steps.add((long) i);
            steps.add(v);
          }
        }
      }

      for (Long l: steps) {
        System.out.printf("%x ", l);
      }

      System.out.println();
      return steps;
    }
    
    public static boolean contains(double[] bounds, double lat, double lon) {
      // If the bounds array does not contain an even number of lat,lon pairs
      // then return false.
      
      if (bounds.length % 4 != 0) {
        return false;
      } else {
        for (int i = 0; i < bounds.length; i += 4) {
          if (bounds[i] <= lat && lat <= bounds[i+2]
              && bounds[i+1] <= lon && lon <= bounds[i+3]) {
            return true;
          }
        }
        
        return false;
      }
    }
}
