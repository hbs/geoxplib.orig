//
//  GeoXP Lib, library for efficient geo data manipulation
//
//  Copyright (C) 1999-2016  Mathias Herberts
//
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Affero General Public License as
//  published by the Free Software Foundation, either version 3 of the
//  License, or (at your option) any later version and under the terms
//  of the GeoXP License Exception.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

package com.geoxp.geo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeoHashHelper {
  
  private static final String GEOHASH_CHAR_MAP = "0123456789bcdefghjkmnpqrstuvwxyz";
  
  public static final String fromHHCode(long hhcode, int resolution) {

    // Swap lat/lon
    long codehh = 0L;
    
    int bits = 64;
    //
    // Swap lat and lon
    // We look at the two LSB of 'hhcode' and reflect
    // them swapped as the two MSB of 'codehh'
    //
    
    while (bits > 0) {
      codehh >>>= 2;
      if (0 != (hhcode & 0x1L)) {
        // The LSB of hhcode is 1 so the MSB of codehh must be set to 1
        codehh |= 0x8000000000000000L;
      }
      // Shift hhcode one bit to the right
      hhcode >>>= 1;
      if (0 != (hhcode & 0x1L)) {
        // The bit left of the LSB is 1, so the bit right of the MSB of codehh must be set to 1
        codehh |= 0x4000000000000000L;
      }
      hhcode >>>= 1;
      bits -= 2;
    }

    // Shift hhcode to the right, ensuring resolution leads to a multiple of 5 bits so we can cluster bits by 5
    
    resolution = 2 * resolution;
    resolution -= resolution % 5;
    codehh >>>= 64 - resolution;
    
    // GeoHashes are groups of 5 bits.
    StringBuilder sb = new StringBuilder();

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
      long idx = GEOHASH_CHAR_MAP.indexOf(geohash.charAt(i));
      
      if (idx < 0) {
        throw new RuntimeException("Invalid GeoHash '" + geohash + "' at index " + i);
      }
      
      hhcode |= idx;
      i++;
    }    
    
    hhcode <<= 64 - (5 * i);
    
    // Swap lat/lon
    long[] coords = HHCodeHelper.splitHHCode(hhcode, 32);
    
    return HHCodeHelper.buildHHCode(coords[1], coords[0], 32);
  }
  
  /**
   * Converts a list of geohashes into a Coverage.
   * The geohashes are assumed to be lowercase
   */
  public static Coverage toCoverage(Collection<String> geohashes) {
    Coverage c = new Coverage();
    
    // Enable auto dedup and auto optimize to reduce space
    // consumption
    c.setAutoOptimize(true);
    c.setAutoThresholds(0L);
    c.setAutoDedup(true);    
    
    for (String geohash: geohashes) {
      
      long hhcode = toHHCode(geohash);
      // This is the number of bits from the geohash
      int nbits = 5 * Math.min(12, geohash.length());
      
      // This is a mask to select bits from the HHCode
      // which match those of the geohash. This is a contiguous
      // set of 1s in the case nbits is even, but a different
      // mask when nbits is odd since HHCodes interleave lat+lon
      // where geohashes interleave lon+lat. So for example a
      // single character geohash (5 bits) will have the following
      // value of mask: 0x111101 since the geohash encodes 3 bits of
      // longitude but only 2 bits of latitude
      long mask = (-1L >>> (64 - nbits)) << (64 - nbits);
      int resolution = nbits >>> 1;

      // This is how many bits we need to align to
      int target = nbits;
    
      switch (nbits) {
        case 5:
          nbits = 4;
          mask = 0xF4L << 56;
          target = 8;
          break;
        case 10:
          target = 12;
          break;
        case 15:
          nbits = 14;
          mask = 0xFFFDL << 48;
          target = 16;
          break;
        case 25:
          nbits = 24;
          mask = 0xFFFFFF4L << 36;
          target = 28;
          break;
        case 30:
          target = 32;
          break;
        case 35:
          nbits = 34;
          mask = 0xFFFFFFFFDL << 28;
          target = 36;
          break;
        case 45:
          nbits = 44;
          mask = 0xFFFFFFFFFFF4L << 16; 
          target = 48;
          break;
        case 50:        
          target = 52;
          break;
        case 55:
          nbits = 54;
          mask = 0xFFFFFFFFFFFFFDL << 8;
          target = 56;
          break;
      }
      
      int deltabits = target - nbits;
      resolution = target >>> 1;

      if (deltabits > 0) {
        long hh = hhcode;
        hhcode = (hhcode >>> (64 - nbits)) << deltabits;
        
        // Iterate over the values encoded in the extra bits
        for (long delta = 0; delta < 1L << deltabits; delta++) {
          long cell = (hhcode | delta) << (64 - target);
          // Only add a cell if the values extracted by 'mask'
          // match the bit values from the original geohash
          if ((cell & mask) == hh) {
            c.addCell(resolution, cell);
          }
        }
      } else {
        c.addCell(resolution, hhcode);        
      }
    }
    
    return c;
  }
  
  public static long[] toGeoCells(List<String> geohashes) {
    Coverage c = toCoverage(geohashes);
    long[] geocells = c.toGeoCells(HHCodeHelper.MAX_RESOLUTION);
    return geocells;
  }
  
  public static Collection<String> fromGeoCells(long[] geocells, boolean optimize) {
    // We will have at least gecells.length geohashes
    Set<String> geohashes = new HashSet<String>(geocells.length);
    
    long[] coords = new long[2];
    
    for (long geocell: geocells) {
      int resolution = (int) ((geocell >>> 60) << 1);
      long hhcode = geocell << 4;
      
      int nbits = resolution * 2;
      // clear lower bits of hhcode
      hhcode >>>= (64 - nbits);
      hhcode <<= (64 - nbits);
      
      if (0 == nbits % 5) {
        geohashes.add(fromHHCode(hhcode, resolution));        
      } else {
        int deltabits = 5 - (nbits % 5);
        // If the number of bits is odd, make it even
        if ((nbits + deltabits) % 2 == 1) {
          deltabits++;
        }
        int shift = 64 - (nbits + deltabits);
        resolution = (int) Math.round(Math.ceil((nbits + deltabits) / 2.0D));
        long hh = hhcode >>> shift;
        for (long offset = 0; offset < 1L << deltabits; offset++) {
          geohashes.add(fromHHCode((hh | offset) << shift, resolution));
        }
      }
      
    }
    
    if (optimize) {
      return optimize(geohashes);
    }
    
    return geohashes;
  }
  
  public static Collection<String> optimize(Collection<String> geohashes) {
    List<String> gh = new ArrayList<String>(geohashes);
    Collections.sort(gh);
    String lastprefix = "-";
    int lastlen = 0;
    int subcells = 0;
    int mask = 0;
    Set<String> optimized = new HashSet<String>();
    for (String geohash: gh) {
      if (geohash.length() != lastlen || !geohash.startsWith(lastprefix)) {
        if (32 == subcells) { 
          optimized.add(lastprefix);
        } else if (subcells > 0) {
          for (int i = 0; i < 32; i++) {
            if (0 != (mask & 1 << i)) {
              optimized.add(lastprefix + GEOHASH_CHAR_MAP.charAt(i));
            }
          }
        }
        subcells = 0;
        mask = 0;
        lastlen = geohash.length();
        lastprefix = geohash.substring(0, lastlen - 1);
      }
      
      mask |= (1 << GEOHASH_CHAR_MAP.indexOf(geohash.substring(lastlen - 1)));
      subcells++;
    }
    
    if (32 == subcells) { 
      optimized.add(lastprefix);
    } else if (subcells > 0) {
      for (int i = 0; i < 32; i++) {
        if (0 != (mask & 1 << i)) {
          optimized.add(lastprefix + GEOHASH_CHAR_MAP.charAt(i));
        }
      }
    }

    return optimized;
  }
}
