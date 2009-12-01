package com.geocoord.geo;

public class GeoHashHelper {
  
  private static final String GEOHASH_CHAR_MAP = "0123456789bcdefghjkmnpqrstuvwxyz";
  
  public static final String fromHHCode(long hhcode, int resolution) {

    // Swap lat/lon
    long codehh = 0L;
    
    int bits = 64;
    


    while (bits > 0) {
      codehh >>= 2;
      if (0 != (hhcode & 0x1L)) {
        codehh |= 0x8000000000000000L;
      } else {
        codehh &= 0x7fffffffffffffffL;        
      }
      hhcode >>= 1;
      if (0 != (hhcode & 0x1L)) {
        codehh |= 0x4000000000000000L;
      } else {
        codehh &= 0xbfffffffffffffffL;
      }
      hhcode >>= 1;
      bits -= 2;
    }

    // Shift hhcode to the right, ensuring resolution is a multiple of 5 so we can cluster bits by 5
    resolution -= resolution % 5;
    codehh >>= 64 - 2 * resolution;
    
    // GeoHashes are groups of 5 bits.
    StringBuilder sb = new StringBuilder();

    resolution *= 2;
    while(resolution > 0) {
      sb.insert(0, GEOHASH_CHAR_MAP.charAt((int) (codehh & 0x1fL)));
      codehh >>= 5;
      resolution -= 5;
    }
    
    return sb.toString();        
  }
  
  public static long toHHCode(String geohash) {
    long hhcode = 0L;
    
    int i = 0;
    
    while (i < 12 && i < geohash.length()) {
      if (i > 0) {
        hhcode <<= 5;
      }
      hhcode |= GEOHASH_CHAR_MAP.indexOf(geohash.charAt(i));
      i++;
    }    
    
    hhcode <<= 64 - (5 * i);
    
    // Swap lat/lon
    long[] coords = HHCodeHelper.splitHHCode(hhcode, 32);
    
    return HHCodeHelper.buildHHCode(coords[1], coords[0], 32);
  }
}
