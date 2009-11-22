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

    // Shift hhcode to the right, ensuring resolution leads to a multiple of 5 bits so we can cluster bits by 5
    
    resolution = 2 * resolution;
    resolution -= resolution % 5;
    codehh >>= 64 - resolution;
    
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
      hhcode |= GEOHASH_CHAR_MAP.indexOf(geohash.charAt(i));
      i++;
    }    
    
    hhcode <<= 64 - (5 * i);
    
    // Swap lat/lon
    long[] coords = HHCodeHelper.splitHHCode(hhcode, 32);
    
    return HHCodeHelper.buildHHCode(coords[1], coords[0], 32);
  }
}
