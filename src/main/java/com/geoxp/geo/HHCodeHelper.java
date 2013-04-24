package com.geoxp.geo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to manipulate HHCodes.
 * 
 * HHCodes are Helical Hyperspatial Codes, invented by the Canadian Hydrographic Service.
 * 
 * It is a Z Space Filling curve (Morton order). Each point is represented as a long which
 * is created by interleaving the bits of the lat/lon long coordinates (2**32 steps on 180/360 degrees).
 * 
 * Resolution refers to the number of significant bits in each dimension of an HHCode.
 * Resolutions are always even, starting at 2 (the coarsest) and ending at 32 (the finest).
 * HHCodes at a given resolution R only have 2R significant bits.
 * The hexadecimal representation of an HHCode at resolution R has R/2 nibbles.
 * So for example HHCode 0xfedcba9876543210L has the following hex representations at various resolutions:
 * 
 * R=2  F
 * R=4  FE
 * R=6  FED
 * ...
 * R=30 FEDCBA987654321
 * R=32 FEDCBA9876543210
 * 
 * The precision (the height/width in meters of an HHCode cell) at the equator for various values of R is:
 * 
 * R=2  W=10000800 (10000 km)   H=5000400 (5000 km)
 * ..
 * R=8  W=156262                H=78131
 * ..
 * R=16 W=610                   H=305
 * ..
 * R=32 W=0.0093139708042144775 H=0.0046569854021072388
 * 
 * Between two values of R, the precision has a ratio of 4 (twice finer when R increases) in each direction, thus the HHCode cell is 16 times smaller.
 * 
 * @see http://en.wikipedia.org/wiki/HHCode
 * @see http://en.wikipedia.org/wiki/Z-order_(curve)
 * 
 */
public final class HHCodeHelper {

  /**
   * Square root of tolerance for Rhumb Line distance computation
   */
  private static final double TOLSQRT = Math.sqrt(0.000000000000001D);
  
  public static final int MIN_RESOLUTION = 2;
  public static final int MAX_RESOLUTION = 32;
  
  /**
   * Number of degrees per unit of latitude.
   */
  public static final double DEGREES_PER_LAT_UNIT = 180.0D / (1L << 32);
  
  /**
   * Number of radians per unit of latitude.
   */
  private static final double RADIANS_PER_LAT_UNIT = Math.PI / (1L << 32);
    
  /**
   * Number of degrees per unit of longitude.
   */
  public static final double DEGREES_PER_LON_UNIT = 360.0D / (1L << 32);
  
  /**
   * Number of radians per unit of longitude
   */
  private static final double RADIANS_PER_LON_UNIT = (Math.PI + Math.PI) / (1L << 32);

  /**
   * Number of lat units per meter at the equator
   */
  public static final double latUnitsPerMeter = (1L << 32) / ((double) (180L * 60L)) / ((double) 1852);

  /**
   * Number of lon units per meter at the equator
   */
  public static final double lonUnitsPerMeter = (1L << 32) / ((double) (360L * 60L)) / ((double) 1852);

  /**
   * Return the HHCode value of a combination of lat/lon expressed in degrees.
   * 
   * @param lat Latitude of point (-90.0/90.0)
   * @param lon Longitude of point (-180.0/180.0)
   * @return The computed HHCode value.
   */
  public static final long getHHCodeValue(double lat, double lon) {
    
    // Shift lat/lon
    
    lat += 90.0;
    lon += 180.0;
    
    long[] coords = new long[2];
    
    //
    // We use floor because we want to know the slot in which lies 'lat' or 'lon',
    // not the slot after (which might be returned if we called round).
    //
    
    coords[0] = (long) Math.floor(lat / DEGREES_PER_LAT_UNIT);
    coords[1] = (long) Math.floor(lon / DEGREES_PER_LON_UNIT);
    
    return buildHHCode(coords[0], coords[1]);
  }
  
  /**
   * Return the hhcode of the cell above the given one, at the given resolution
   * 
   * @param hhcode HHCode of the original cell
   * @param resolution Resolution at which to do the math, from 1to 32
   * @return
   */
  public static final long northHHCode(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // Add 1**(32 - resolution) to the lat
    //
    
    coords[0] = (coords[0] + (1 << (32 - resolution)));
    
    //
    // Rebuild HHCode
    //
    
    return buildHHCode(coords[0], coords[1]);
  }

  /**
   * Return the hhcode of the cell below the given one, at the given resolution
   * 
   * @param hhcode HHCode of the original cell
   * @param resolution Resolution at which to do the math, from 1to 32
   * @return
   */
  public static final long southHHCode(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // Add 1**(32 - resolution) to the lat
    //
    
    coords[0] = (coords[0] - (1 << (32 - resolution)));
    
    //
    // Rebuild HHCode
    //
    
    return buildHHCode(coords[0], coords[1]);
  }

  /**
   * Return the hhcode of the cell to the right of the given one, at the given resolution
   * 
   * @param hhcode HHCode of the original cell
   * @param resolution Resolution at which to do the math, from 1to 32
   * @return
   */
  public static final long eastHHCode(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // Add 1**(32 - resolution) to the lon
    //
    
    coords[1] = (coords[1] + (1 << (32 - resolution)));

    //
    // Rebuild HHCode
    //
    
    return buildHHCode(coords[0], coords[1]);
  }

  /**
   * Return the hhcode of the cell to the left of the given one, at the given resolution
   * 
   * @param hhcode HHCode of the original cell
   * @param resolution Resolution at which to do the math, from 1to 32
   * @return
   */
  public static final long westHHCode(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // Subtract 1**(32 - resolution) to the lon
    //
    
    coords[1] = (coords[1] - (1 << (32 - resolution)));

    //
    // Rebuild HHCode
    //
    
    return buildHHCode(coords[0], coords[1]);
  }

  public static final long northEastHHCode(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // add delta to lat/lon
    //
    
    coords[0] = (coords[0] + (1 << (32 - resolution)));
    coords[1] = (coords[1] + (1 << (32 - resolution)));

    //
    // Rebuild HHCode
    //
    
    return buildHHCode(coords[0], coords[1]);    
  }

  public static final long southEastHHCode(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // substract/add delta to lat/lon
    //
    
    coords[0] = (coords[0] - (1 << (32 - resolution)));
    coords[1] = (coords[1] + (1 << (32 - resolution)));

    //
    // Rebuild HHCode
    //
    
    return buildHHCode(coords[0], coords[1]);    
  }

  public static final long southWestHHCode(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // substract delta to lat/lon
    //
    
    coords[0] = (coords[0] - (1 << (32 - resolution)));
    coords[1] = (coords[1] - (1 << (32 - resolution)));

    //
    // Rebuild HHCode
    //
    
    return buildHHCode(coords[0], coords[1]);    
  }

  public static final long northWestHHCode(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // substract/add delta to lat/lon
    //
    
    coords[0] = (coords[0] - (1 << (32 - resolution)));
    coords[1] = (coords[1] + (1 << (32 - resolution)));

    //
    // Rebuild HHCode
    //
    
    return buildHHCode(coords[0], coords[1]);    
  }

  /**
   * Split a HHCode value into its lat/lon components expressed as long
   * and fill the provided array.
   * 
   * @param hhcode longHHCode value to split
   * @param resolution the resolution at which to do the math
   * @param coords an array to be filled by two longs (lat/long)
   */
  public static final void stableSplitHHCode(final long hhcode, final int resolution, final long[] coords) {

    long c0 = 0L;
    long c1 = 0L;
    
    for (int i = 32 - 1; i >= 32 - resolution; i--) {
      c0 <<= 1;
      c0 |= 0x1L & (hhcode >> (1 + (i << 1)));
      c1 <<= 1;
      c1 |= 0x1L & (hhcode >> (i << 1));
    }

    if (32 != resolution) {
      c0 <<= 32 - resolution;
      c1 <<= 32 - resolution;
    }
    
    coords[0] = c0;
    coords[1] = c1;
  }
 
  /**
   * Split a HHCode value into its lat/lon components expressed as long.
   * 
   * @param hhcode HHCode value to split
   * @return An array of two longs (lat/lon)
   */
  public static final long[] splitHHCode(long hhcode, int resolution) {
    long[] coords = new long[2];

    long c0 = 0L;
    long c1 = 0L;
    
    for (int i = 32 - 1; i >= 32 - resolution; i--) {
      c0 <<= 1;
      c0 |= 0x1L & (hhcode >> (1 + (i << 1)));
      c1 <<= 1;
      c1 |= 0x1L & (hhcode >> (i << 1));
    }

    if (32 != resolution) {
      c0 <<= 32 - resolution;
      c1 <<= 32 - resolution;
    }
    
    coords[0] = c0;
    coords[1] = c1;
    
    return coords;
  }
  
  private static final long[] splitHHCode(long hhcode) {
    return splitHHCode(hhcode, 32);
  }
  
  public static final double[] getLatLon(long hhcode, int resolution) {
    double[] latlon = new double[2];
    stableGetLatLon(hhcode, resolution, latlon, 0);
    return latlon;
  }
  
  public static final void stableGetLatLon(long hhcode, int resolution, double[] target, int offset) {
    long[] coords = splitHHCode(hhcode, resolution);
    target[offset] = coords[0] * DEGREES_PER_LAT_UNIT - 90.0;
    target[offset + 1] = coords[1] * DEGREES_PER_LON_UNIT - 180.0;    
  }
  
  /**
   * Build a HHCode value by interleaving bits of lat and lon
   * 
   * @param lat Latitude
   * @param lon Longitude
   * @param resolution Resolution (even 2->32)
   * @return the HHCode value
   */
  public static final long buildHHCode(long lat, long lon, int resolution) {
    long hhcode = 0L;

    //
    //
    // The wrapping of the HHCode planisphere is as follow:
    //
    // +---+---+---+---+
    // | 5 | 4 | 1 | 0 |
    // +---+---+---+---+
    // | 7 | 6 | 3 | 2 |
    // +---+---+---+---+    <---- north lat wrapping
    // | D | C | 9 | 8 |
    // +---+---+---+---+
    // | F | E | B | A |
    // +---+---+---+---+---------------------------
    // | A | B | E | F |
    // +---+---+---+---+
    // | 8 | 9 | C | D |
    // +---+---+---+---+    <---- main planisphere
    // | 2 | 3 | 6 | 7 |
    // +---+---+---+---+
    // | 0 | 1 | 4 | 5 |
    // +---+---+---+---+--------------------------
    // | 5 | 4 | 1 | 0 |
    // +---+---+---+---+
    // | 7 | 6 | 3 | 2 |
    // +---+---+---+---+    <---- south lat wrapping
    // | D | C | 9 | 8 |
    // +---+---+---+---+
    // | F | E | B | A |
    // +---+---+---+---+
    //
    // So we have two cases:
    //
    // * if bit 32 of the latitude is 1, we need to invert the lower 32 bits of the latitude and retain only those 32 bits (+91 is +89)
    //   The longitude needs to be offset by half the globe (+2**31) and have its lower 32 bits retained.
    //
    // * If bit 32 is 0, simply retain the lowest 32 bits as is.
    //   Only retain lowest 32 bits of longitue
    //


    if (0L != (lat & 0x100000000L)) {
      lat ^= 0xffffffffL;
      // Shifting lat by 2**31 can be done by adding 2**31 or simply flipping the 31st bit
      // since we only retain the lowest 32
      lon ^= 0x80000000L;
    }

    //
    // Keep lower 32 bits
    //
    
    lat &= 0xffffffffL;    
    lon &= 0xffffffffL;

    for (int i = 32 - 1; i >= 32 - resolution; i--) {
         hhcode <<= 1;
         hhcode |= (lat & (1L << i)) >> i;
         hhcode <<= 1;
         hhcode |= (lon & (1L << i)) >> i;
    }

    if (32 != resolution) {
      hhcode <<= 32 - resolution;
    }
    
    return hhcode;
  }

  private static final long buildHHCode(long lat, long lon) {
    return buildHHCode(lat, lon, 32); 
  }
  
  public static final String toString(long hhcode) {
    return toString(hhcode, 32);
  }
  
  /**
   * Converts a HHCode to 15 geocells.
   * 
   * A geocell is a long whose upper 4 bits encode the precision of
   * the HHCode stored in the lowest 60 bits.
   * 
   * Encoded precision ranges from 0b0001 (for precision 2) to 0b11111 (for precision 30)
   * 
   * The lowest 60 bits encode the HHCode value (with only 'encoded precision' * 4 upper significant bits)
   * 
   * Highest precision (32) is not encoded.
   * 
   * @param hhcode
   * @return
   */
  public static final long[] toGeoCells(long hhcode) {
    return toGeoCells(hhcode, MAX_RESOLUTION);
  }

  /**
   * Converts a HHCode to geocells at or below a given resolution.
   * 
   * A geocell is a long whose upper 4 bits encode the precision of
   * the HHCode stored in the lowest 60 bits.
   * 
   * Encoded precision ranges from 0b0001 (for precision 2) to 0b11111 (for precision 30)
   * 
   * The lowest 60 bits encode the HHCode value (with only 'encoded precision' * 4 upper significant bits)
   * 
   * Highest precision (32) is not encoded.
   * 
   * The number of returned geocells is N / 2 where N is the finest resolution (capped to 30).
   * 
   * @param hhcode HHCode to encode
   * @param finest finest resolution for which to generate a geocell
   * @return
   */
  public static final long[] toGeoCells(long hhcode, int finest) {
    
    if (finest >= 32) {
      finest = 30;
    }
    if (finest < 2) {
      finest = 2;
    }
    
    long[] geocells = new long[finest >> 1];
    
    for (int i = 0; i < (finest >> 1); i++) {
      // Encode resolution
      geocells[i] = ((long) (i+1)) << 60;
      // Encode HHCode
      geocells[i] |= (hhcode >> 4) & 0x0fffffffffffffffL;
      // Trim HHCode to resolution
      geocells[i] &= (0xffffffffffffffffL ^ ((1L << (4 * (15 - (i + 1)))) - 1)); 
    }
    
    return geocells;
  }

  /**
   * Return the 16 child geocells of the given geocell
   * 
   * @param geocell
   * @return
   */
  public static final long[] getSubGeoCells(long geocell) {
    
    long[] subcells = new long[16];
    
    if ((geocell & 0xf000000000000000L) == 0xf000000000000000L) {
      //
      // The case res = 15 (30) is an edge one as the sub cells
      // are at resolution 32 and cannot therefore be represented as
      // geocells
      //
      
      //
      // Shift HHCode left 4 bits and zero lower 4 bits
      //
      
      long hhcode = (geocell << 4) & 0xfffffffffffffff0L;
      
      for (int i = 0; i < 16; i++) {
        subcells[i] = hhcode | (i & 0xfL);
      }      
    } else {
      int res = (int) ((geocell & 0xf000000000000000L) >> 60);
      
      res = res + 1;
      
      //
      // Zero lower bits and upper 4 bits
      //
      
      long hhcode = geocell & (0xffffffffffffffffL ^ ((1L << (60 - 4 * (res - 1)))) - 1);
      hhcode = hhcode & 0x0fffffffffffffffL;
      
      //
      // Set new resolution
      //
          
      hhcode |= ((long) res) << 60;
       
      for (int i = 0; i < 16; i++) {
        subcells[i] = hhcode | (((long) i) << (60 - 4 * res));
      }
    }
    
    return subcells;
  }
  
  public static final long toGeoCell(long hhcode, int resolution) {
    
    // Only even resolution between 2 and 30 are supported for geocells.
    if (resolution > 31 || resolution < 2 || resolution % 2 == 1) {
      return 0L;
    }
    
    // Shift resolution 59 positions to the left (so we divide it by 2) and move it to the 4 MSBs
    long geocell = ((long) resolution) << 59;
    
    // Encode HHCode
    geocell |= (hhcode >> 4) & 0x0fffffffffffffffL;
    
    // Trim HHCode to resolution
    geocell &= (0xffffffffffffffffL ^ ((1L << (4 * (15 - (resolution >> 1)))) - 1));
    
    return geocell;
  }
  
  /**
   * Return a String representation of an hhcode at a given resolution
   * 
   * @param hhcode HHCode to represent
   * @param resolution resolution (event number between 2 and 32).
   * @return The string representation
   */
  public static final String toString(long hhcode, int resolution) {
    
    // Make resolution even
    resolution &= 0xfe;
    
    StringBuilder sb = new StringBuilder();
    
    sb.append(Long.toHexString(hhcode));
    
    while(sb.length() < 16) {
      sb.insert(0, "0");
    }
    
    sb.setLength(resolution >> 1);
    
    return sb.toString();
  }
  
  /**
   * Transform a coverage into its canonical form, that is remove duplicates at each resolution and
   * sort cells in each resolution.
   * 
   * @param coverage Coverage to canonicalize.
   * @return
   */
  public static final void canonicalizeCoverage(final Map<Integer,List<Long>> coverage) {
    
    Set<Long> cells = new HashSet<Long>();
    
    for (int res: coverage.keySet()) {
      cells.clear();
      for (long hhcode: coverage.get(res)) {
        cells.add(hhcode & (0xffffffffffffffffL ^ ((0x1L << (64 - 2 * res)) - 1)));
      }
      coverage.get(res).clear();
      coverage.get(res).addAll(cells);
      Collections.sort(coverage.get(res));
    }
  }
  
  /**
   * Optimize a coverage by merging clusters of adjacent cells
   *
   * @param coverage The coverage to optimize. It will be optimized in place.
   * @param thresholds Thresholds to consider when clustering cells. Threshold for resolution x
   *                   is stored on 4 bits (the upper 4 for resolution 2, then the next 4 for
   *                   resolution 4 ... then the lower 4 for resolution 32)
   *                   If 'threshold' cells out of 16 are contained in the coverage, then they are replaced by the containing cell at the coarser resolution.
   *                   A threshold of '0' is to be interpreted as 16.
   */
  
  public static final Map<Integer,List<Long>> optimize(final Map<Integer,List<Long>> coverage, long thresholds) {
    //
    // Loop on the resolution, from highest to lowest
    //
    
    int resolution = 32;
    
    while (resolution > 0) {

      // If no cells exist for the current resolution, skip to the lower one
      if (!coverage.containsKey(resolution)) {
        resolution -= 2;
        continue;
      }

      //
      // Sort the cells at this resolution
      //

      // Exit if resolution is 2
      if (2 == resolution) {
        Set<Long> unique = new HashSet<Long>();
        
        for (long hhcode: coverage.get(resolution)) {
          unique.add(hhcode & 0xf000000000000000L);
        }

        coverage.get(resolution).clear();
        coverage.get(resolution).addAll(unique);
        Collections.sort(coverage.get(resolution));
        break;
      }

      Collections.sort(coverage.get(resolution));

      //
      // Compute mask to extract the prefix (i.e. n-4 bits where n is the number of bits of this resolution)
      //
      
      // We use resolution + 2 here because we are interested in the prefix at the next lower resolution
      long prefixmask = 0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution + 2))) - 1);
      long offsetmask = (1L << (2 * (32 - resolution + 2))) - 1;
            
      
      //
      // Loop through the cells, counting the subcells of cells at a lower resolution
      // and clustering them if their count is above the threshold for their resolution
      //
      
      List<Long> cells = new ArrayList<Long>();
      
      int idx = 0;

      // Last cell prefix seen
      long prefix = 0;      
      
      // Bitmask of offsets encountered in the prefix
      int offsets = 0;
      
      // Number of different offsets
      int setbits = 0;
      
      boolean first = true;
      
      while (idx <= coverage.get(resolution).size()) {
      
        //
        // Extract prefix
        //
        
        long curprefix = 0;
        
        if (idx < coverage.get(resolution).size()) {
          curprefix = coverage.get(resolution).get(idx) & prefixmask;
        }        

        //
        // If this is not the first cell and the prefix has changed, check if
        // we need to downsample the cells.
        //

        if (!first && (idx == coverage.get(resolution).size() || prefix != curprefix)) {
          //
          // Compare number of setbits to resolution's threshold
          //
          
          long threshold = (thresholds >> (2 * (32 - resolution))) & 0xfL;
          
          if (0xffff == offsets || (threshold > 0 && setbits >= threshold)) {
            // We have crossed the threshold, simply record the prefix in the lower
            // resolution's cells
            if (!coverage.containsKey(resolution - 2)) {
              coverage.put(resolution - 2, new ArrayList<Long>());
            }
            coverage.get(resolution - 2).add(prefix);
          } else {
            // Threshold was not crossed, add all offsets we found in this resolution's cells
            for (int i = 0; i < 16; i++) {
              if (0 != (offsets & (1 << i))) {
                cells.add(prefix | ((long) i << (2 * (32 - resolution))));
              }
            }
          }
                    
          offsets = 0;
          setbits = 0;
          prefix = curprefix;
        }
        
        // We reached the end of this resolution's cell list
        if (idx == coverage.get(resolution).size()) {
          break;
        }
        
        // This is the first cell, init prefix and offsets;
        if (first) {
          first = false;
          prefix = curprefix;
          offsets = 0;
          setbits = 0;
        }
        
        // Set the bit of the current offset in 'prefix'
      
        int prevoffsets = offsets;
        
        offsets |= (int) (1 << ((coverage.get(resolution).get(idx) & offsetmask) >> (2 * (32 - resolution))));
        
        if (offsets != prevoffsets) {
          setbits++;
        }
        idx++;
      }
      
      if (cells.isEmpty()) {
        coverage.remove(resolution);
      } else {
        coverage.put(resolution, cells);
      }
      
      resolution -= 2;
    }
    
    //
    // Now scan the resolutions from lower to higher, removing cells at resolution R+4
    // that are covered by a cell at resolution R, this can happen when clustering cells
    // at R+2
    //
    
    resolution = 2;
    
    while(resolution < 28) {
      
      if (coverage.containsKey(resolution) && coverage.containsKey(resolution + 4)) {
        
        long prefixmask = (0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution))) - 1));

        List<Long> newcov = new ArrayList<Long>();
        
        for (long cell: coverage.get(resolution)) {
          
          long curprefix = cell & prefixmask;
          
          // Loop over cells in coverage at 'resolution+4'
          for (long cell2: coverage.get(resolution + 4)) {
            // If a cell in this coverage has a common prefix with the cell at 'resolution'
            // then ignore it.
            if ((cell2 & prefixmask) != curprefix && !newcov.contains(cell2)) {
              newcov.add(cell2);
            }
          }
        }
        
        if (!newcov.isEmpty()) {
          coverage.put(resolution + 4, newcov);
        } else {
          coverage.remove(resolution + 4);
        }
      }
      
      resolution += 2;
    }

    return coverage;
  }
    
  public static final long[] getBoundingBox(List<Long> lats, List<Long> lons) {
    
    long[] bbox = new long[4];
    
    bbox[0] = Long.MAX_VALUE; // SW lat
    bbox[1] = Long.MAX_VALUE; // SW lon
    bbox[2] = Long.MIN_VALUE; // NE lat
    bbox[3] = Long.MIN_VALUE; // NE lon

    for (int i = 0; i < Math.min(lats.size(), lons.size()); i++) {
      if (lats.get(i) < bbox[0]) {
        bbox[0] = lats.get(i);
      }
      if (lats.get(i) > bbox[2]) {
        bbox[2] = lats.get(i);
      }
      if (lons.get(i) > bbox[3]) {
        bbox[3] = lons.get(i);
      }
      if (lons.get(i) < bbox[1]) {
        bbox[1] = lons.get(i);
      }      
    }

    return bbox;    
  }
  
  /**
   * Return the bounding box of the list of nodes.
   * 
   * @param nodes List of nodes to compute the bounding box of.
   * @return an array of 4 long (SW lat, SW lon, NE lat, NE lon)
   */
  public static final long[] getBoundingBox(List<Long> nodes) {
    
    long[] bbox = new long[4];
    
    bbox[0] = Long.MAX_VALUE; // SW lat
    bbox[1] = Long.MAX_VALUE; // SW lon
    bbox[2] = Long.MIN_VALUE; // NE lat
    bbox[3] = Long.MIN_VALUE; // NE lon

    final long[] coords = new long[2];

    for (long hhcode: nodes) {
      HHCodeHelper.stableSplitHHCode(hhcode, 32, coords);

      if (coords[0] < bbox[0]) {
        bbox[0] = coords[0];
      }
      if (coords[0] > bbox[2]) {
        bbox[2] = coords[0];
      }
      if (coords[1] > bbox[3]) {
        bbox[3] = coords[1];
      }
      if (coords[1] < bbox[1]) {
        bbox[1] = coords[1];
      }
    }
    
    return bbox;
  }

  /**
   * Return the optimal resolution for covering an area enclosed in the given bbox
   * so the error is less than 'pctError' of the longest possible distance in the bbox.
   * 
   * @param bbox Bounding box to consider
   * @param pctError Maximum percentage of error
   * @return The optimal resolution
   */
  public static final int getOptimalResolution(long[] bbox, double pctError) {
    //
    // Compute longest distance using orthodromy
    //
    
    double d = HHCodeHelper.orthodromicDistance(bbox[0], bbox[1], bbox[2], bbox[3]);
    
    //
    // Compute error length
    //
    
    double err = d * pctError;
        
    //
    // Compute optimal resolution to be more precise than the computed error
    // Do so close to the equator, where the cells are bigger.
    //
    
    int r = HHCodeHelper.MAX_RESOLUTION;
    
    while(r > HHCodeHelper.MIN_RESOLUTION) {
      double lonspan = 2.0D * Math.PI / (1L << r);
      double latspan = Math.PI / (1L << r);
      
      double flat = -latspan / 2.0D;
      double flon = 0;
      double tlat = latspan / 2.0D;
      double tlon = lonspan;
      
      double diagonal = 2.0D * Math.asin(Math.sqrt(Math.pow(Math.sin((flat-tlat)/2.0D), 2.0D) + Math.cos(flat)*Math.cos(tlat)*Math.pow(Math.sin((flon-tlon)/2.0D),2.0D)));

      if (diagonal > err) {
        break;
      }
      
      r -= 2;
    }
    
    return r;
  }
  
  /**
   * Return the optimal resolution for covering a polygon.
   * 
   * @param bbox Bounding box of polygon to cover (may cross the IDL).
   * @param offset Offset to apply to optimal resolution, use -2 to have a resolution twice finer as the optimal one.
   * @return The computed optimal resolution (2-32 from coarsest to finest).
   */
  public static final int getOptimalPolygonResolution(long[] bbox, int offset) {
    
    int resoffset = -offset;
    
    int resolution;
    
    //
    // Limit the delta to 2**32 - 1 as over that value we wrap around
    //
    
    long deltaLat = Math.min(Math.abs(bbox[2] - bbox[0]), (1L << MAX_RESOLUTION) - 1);
    long deltaLon = Math.min(Math.abs(bbox[3] - bbox[1]), (1L << MAX_RESOLUTION) - 1);
      
    // Extract log2 of the deltas and keep smallest
    // This log is the resolution we must use to have cells that are just a little smaller than 
    // the one we try to cover. We could use 'ceil' instead of 'floor' to have cells a little bigger.
    
    // Actually we don't want to have too big a difference between max/min resolution, so we constraint the
    // difference to Coverage.MAX_RES_DIFF.
    //
        
    int latres = (int) Math.floor(Math.log(deltaLat) / Math.log(2.0));
    int lonres = (int) Math.floor(Math.log(deltaLon) / Math.log(2.0));

    //
    // There is more than MAX_RES_DIFF difference between lat/lon resolutions,
    // Use the max - MAX_RES_DIFF
    //
      
    if (Math.abs(latres - lonres) > Coverage.MAX_RES_DIFF) {
      resolution = Math.max(latres, lonres) - Coverage.MAX_RES_DIFF;
    } else {
      // Use the smallest of both
      resolution = Math.min(latres, lonres);
    }

    // Make log an even number.
    resolution = resolution & 0xfe;
    resolution = 32 - resolution;

    // Substract resoffset from computed resolution
    if (resolution + resoffset <= MAX_RESOLUTION) {
      resolution += resoffset;
      // Make resolution even
      resolution = resolution & 0x3e;
    }
    
    return resolution;
  }
  
  public static final int getOptimalPolylineResolution(long[] bbox, int offset) {

    //
    // We start by computing the offsetless optimal resolution for a polygon.
    //
    
    int resolution = getOptimalPolygonResolution(bbox, 0);
    
    //
    // Make the resolution a little finer so we don't cover the line too coarsely
    //
    resolution += 4;

    //
    // We just make sure that we do not exceed MAX_CELLS_PER_SIDE (which could be the case if a line is for example horizontal).
    //
    long MAX_CELLS_PER_SIDE = 64;
    
    while((Math.abs(bbox[2] - bbox[0]) >> (32 - resolution)) > MAX_CELLS_PER_SIDE || (Math.abs(bbox[3] - bbox[1]) >> (32 - resolution)) > MAX_CELLS_PER_SIDE) {
      resolution -= 2;
    }
          
    // Limit resolution to 26 (0.59m at the equator!)
    if (resolution > 26) {
      resolution = 26;
    }

    //
    // Apply offset
    //
    
    if (resolution - offset <= MAX_RESOLUTION && resolution - offset >= MIN_RESOLUTION) {
      resolution -= offset;
      // Make resolution even
      resolution = resolution & 0x3e;
    }

    return resolution;
  }
  
  /**
   * Determine a list of zones covering a polygon. Polygon need not be closed (i.e. last vertex can be != from first vertex).
   * 
   * @param vertices Vertices of the polygon (of hhcodes)
   * @param resolution The resolution at which to do the covering. If the resolution is 0, compute one from the bbox
   * @param geocells optional array of Geocells to intersect / exclude
   * @param excludeGeoCells if true, exclude 'geocells', otherwise intersect.
   * 
   * @return A map keyed by resolution and whose values are the list of zones covering the polygon
   */
  public static final Coverage coverPolygon(List<Long> vertices, int resolution, long[] geocells, boolean excludeGeoCells) {
    List<Long> verticesLat = new ArrayList<Long>();
    List<Long> verticesLon = new ArrayList<Long>();
    
    long[] coords = new long[2];
    
    for (long hhcode: vertices) {
      HHCodeHelper.stableSplitHHCode(hhcode, MAX_RESOLUTION, coords);
      verticesLat.add(coords[0]);
      verticesLon.add(coords[1]);
    }
    
    return coverPolygon(verticesLat, verticesLon, resolution, geocells, excludeGeoCells);
  }

  public static final Coverage coverPolygon(List<Long> vertices, int resolution) {
    return coverPolygon(vertices, resolution, null, false);
  }

  /**
   * Determine a list of zones covering a polygon. Polygon need not be closed (i.e. last vertex can be != from first vertex).
   * 
   * @param verticesLat Vertices latitudes (in long HHCode coordinates) of the polygon.
   * @param verticesLon Vertices longitudes (in long HHCode coordinates) of the polygon.
   * @param resolution The resolution at which to do the covering. If the resolution is 0, compute one from the bbox
   * @param geocells optional array of Geocells to intersect / exclude
   * @param excludeGeoCells if true, exclude 'geocells', otherwise intersect.
   * 
   * @return A map keyed by resolution and whose values are the list of zones covering the polygon
   */
  public static final Coverage coverPolygon(List<Long> verticesLat, List<Long> verticesLon, int resolution, long[] geocells, boolean excludeGeoCells) {
    Coverage coverage = new Coverage();
    return coverPolygon(verticesLat, verticesLon, resolution, coverage, geocells, excludeGeoCells);
  }

  public static final Coverage coverPolygon(List<Long> verticesLat, List<Long> verticesLon, int resolution) {
    return coverPolygon(verticesLat, verticesLon, resolution, null, false);
  }
  
  /**
   * Determine a list of zones covering a polygon. Polygon need not be closed (i.e. last vertex can be != from first vertex).
   * 
   * @param verticesLat Vertices latitudes (in long HHCode coordinates) of the polygon.
   * @param verticesLon Vertices longitudes (in long HHCode coordinates) of the polygon.
   * @param resolution The resolution at which to do the covering. If the resolution is 0, compute one from the bbox.
   *                   If resolution is < 0, compute optimal resolution from bbox then substract 'resolution', so 20 becomes 22 if resolution is -2
   * @param coverage Add the included cells to this coverage
   * @param geocells optional array of Geocells to intersect / exclude
   * @param excludeGeoCells if true, exclude 'geocells', otherwise intersect.
   * 
   * @return A map keyed by resolution and whose values are the list of zones covering the polygon
   */
  public static final Coverage coverPolygon(List<Long> verticesLat, List<Long> verticesLon, int resolution, Coverage coverage, long[] geocells, boolean excludeGeoCells) {
    
    //
    // Sanitize the data, making sure we have the same number of lat and lon,
    // silently ignoring extra data.
    //
    
    int size = Math.min(verticesLat.size(), verticesLon.size());
    
    verticesLat = verticesLat.subList(0, size);
    verticesLon = verticesLon.subList(0, size);

    int nvertices = verticesLat.size();
    
    //
    // Copy vertices lat/lon in arrays for faster access
    //
    
    long[] verticesLatArray = new long[size];
    long[] verticesLonArray = new long[size];
    
    for (int i = 0; i < size; i++) {
      verticesLatArray[i] = verticesLat.get(i);
      verticesLonArray[i] = verticesLon.get(i);
    }
        
    //
    // Determine bounding box of the polygon
    //
    
    long[] bbox = getBoundingBox(verticesLat, verticesLon);
    
    long topLat = bbox[2];
    long leftLon = bbox[1];
    long rightLon = bbox[3];
    long bottomLat = bbox[0];
        
    //
    // Determine the optimal resolution
    //
    
    if (0 >= resolution) {
      resolution = getOptimalPolygonResolution(bbox, resolution);
    }
    
    long resolutionprefixmask = 0xffffffffffffffffL ^ ((1L << (32 - resolution)) - 1);
    long resolutionoffsetmask = (1L << (32 - resolution)) - 1;
    
    // Normalize bbox according to resolution, basically replace vertices with sw corner of enclosing zone
    
    // Force toplat to be at the top of its cell by forcing lower bits to 1
    topLat = topLat | resolutionoffsetmask;
    
    // Force bottomLat to be at the bottom of its cell by forcing lower bits to 0
    bottomLat = bottomLat & resolutionprefixmask;
    
    // Force leftLong to the left of its enclosing cell by forcing lower bits to 0
    leftLon = leftLon & resolutionprefixmask;
    
    // Force rightLon to be at the far right of its cell by forcing lower bits to 1
    rightLon = rightLon | resolutionoffsetmask;
    
    //
    // @see http://alienryderflex.com/polygon_fill/
    //
    
    //
    // Loop from topLat to bottomLat, meeting all vertices lat on the way
    //

    final long[] icoords = new long[2];
    final long[] jcoords = new long[2];
 
    //List<Long> nodeLon = new ArrayList<Long>(40); 
    //List<Long> nodeLat = new ArrayList<Long>();  
    
    Set<Long> allLons = new HashSet<Long>();
    Set<Long> allLats = new HashSet<Long>();
    
    //
    // Add bottom of each cell from bottomLat to topLat
    //

    allLats.addAll(verticesLat);
    
    for (long lat = bottomLat; lat <= topLat; lat += 1L << (32 - resolution)) {
      allLats.add(lat & resolutionprefixmask);
    }

    // Store all lats, removing duplicates
    //nodeLat.addAll(allLats);
    
    long[] nodeLatArray = new long[allLats.size()];
    int idx = 0;
    for (long lat: allLats) {
      nodeLatArray[idx++] = lat;
    }
    
    allLats.clear();
    
    long[] nodeLonArray = new long[nvertices];
    
    // Sort lats from bottom to top
    //Collections.sort(nodeLat);
    Arrays.sort(nodeLatArray);
    
    // Loop over each cell bottom
    //for (long lat: nodeLat) {
    for (int latidx = 0; latidx < nodeLatArray.length; latidx++) {
      long lat = nodeLatArray[latidx];
      
      //
      // Scan the vertices
      //

      // Close the path by referencing the last vertex
      //int j = verticesLat.size() - 1;
      int j = nvertices - 1;
      
      // Clear the intersections
      //nodeLon.clear();
      int lonidx = 0;
      
      // Clear the set of all Longitudes considered.
      // This is necessary because we might otherwise have odd number of lons because a node intersects the lat at a Lon which is at the end of a group of lons already considered
      allLons.clear();
      
      for (int i = 0; i < nvertices; i++) {
        //icoords[0] = verticesLat.get(i);
        icoords[0] = verticesLatArray[i];
        //icoords[1] = verticesLon.get(i);
        icoords[1] = verticesLonArray[i];
        
        //jcoords[0] = verticesLat.get(j);
        jcoords[0] = verticesLatArray[j];
        //jcoords[1] = verticesLon.get(j);
        jcoords[1] = verticesLonArray[j];

        //
        // Only consider a segment if it crosses 'lat', otherwise rounding errors will produce weird artefacts
        //
        
        if (icoords[0] != jcoords[0]
            && ((icoords[0] >= lat && jcoords[0] <= lat || jcoords[0] >= lat && icoords[0] <= lat))) { // edge crosses the bottom of the cell row

          // Determine the lon of the cells at which the top and bottom lats intersect the edge
          
          // We MUST use a double to compute the slope as otherwise the computation on doubles might
          // wrap around and lead to incorrect results (when crossing the IDL and having lons > 2**32).
          
          double slope = ((double) (jcoords[1] - icoords[1])) / ((double) (jcoords[0] - icoords[0]));
          long bottomIntersection = icoords[1] + (long)((lat - icoords[0]) * slope);
          long topIntersection = icoords[1] + (long)(((lat|resolutionoffsetmask) - icoords[0]) * slope);
                    
          // Add all lons between top/bottom intersection as long as they contain the edge

          long startLng = topIntersection & resolutionprefixmask;
          long stopLng = bottomIntersection & resolutionprefixmask;

          if (startLng > stopLng) {
            startLng = bottomIntersection & resolutionprefixmask;
            stopLng = topIntersection & resolutionprefixmask;
          }

          long midlon = 0;
          
          long lowLon = Long.MAX_VALUE;
          long highLon = Long.MIN_VALUE;

          for (long lng = startLng; lng <= stopLng; lng += (1L << (32 - resolution))) {
            if ((lng >= (icoords[1] & resolutionprefixmask) && lng <= (jcoords[1] | resolutionoffsetmask))
                || (lng >= (jcoords[1] & resolutionprefixmask) && lng <= (icoords[1] | resolutionoffsetmask))) {
              coverage.addCell(resolution, lat, lng, geocells, excludeGeoCells);
              // Record low and high bounds of cell slice we just added.
              if ((lng & resolutionprefixmask) < lowLon) {
                lowLon = lng&resolutionprefixmask;
                midlon = lng;
              }
              if ((lng & resolutionprefixmask) > highLon) {
                highLon = lng&resolutionprefixmask;
              }
            }
          }

          
          //
          // If lat is the latitude of the start vertex of the current segment, then add the longitude ONLY if the vertex is a local
          // top/down extremum (the adjacent segments lie on the same side of the horizontal line passing through the vertex).
          //
          if (lat != icoords[0]
              //|| (lat == icoords[0] && (verticesLat.get(i + 1 < verticesLat.size() ? i + 1 : 0) - icoords[0]) * (jcoords[0] - icoords[0]) > 0.0)) { // check > 0.0 so we exclude horizontal segments
              || (lat == icoords[0] && (verticesLatArray[i + 1 < verticesLat.size() ? i + 1 : 0] - icoords[0]) * (jcoords[0] - icoords[0]) > 0.0)) { // check > 0.0 so we exclude horizontal segments
            //nodeLon.add(midlon);
            nodeLonArray[lonidx++] = midlon;
          }         
        } else if(icoords[0] == jcoords[0] && (lat & resolutionprefixmask) == (icoords[0] & resolutionprefixmask)) {
          // Handle the case where the polygon edge is horizontal, we add the cells on the edge to the coverage
          for (long lon = Math.min(icoords[1],jcoords[1]); lon <= Math.max(icoords[1], jcoords[1]); lon += (1L << (32 - resolution))) {
            coverage.addCell(resolution, lat, lon, geocells, excludeGeoCells);
          }
        }

        j = i;
      }

      // Sort nodeLon
      //Collections.sort(nodeLon);
      Arrays.sort(nodeLonArray, 0, lonidx);
      
      // Add the zones between node pairs, removing duplicates

      //int nnodes = nodeLon.size();
      int nnodes = lonidx;
      
      if (nnodes > 1) {
        for (int i = 0; i < nnodes; i += 2) {
          // Check for bounds if the user specified some weird polygon (with wrapping around the pole for example, as in circle:48:-4.5:50000000)
          if (i < nnodes - 1) {
            //for (long lon = nodeLon.get(i) & resolutionprefixmask; lon <= (nodeLon.get(i + 1) | resolutionoffsetmask); lon += (1L << (32 - resolution))) {
            for (long lon = nodeLonArray[i] & resolutionprefixmask; lon <= (nodeLonArray[i + 1] | resolutionoffsetmask); lon += (1L << (32 - resolution))) {
              // Add the cell
              coverage.addCell(resolution, lat, lon, geocells, excludeGeoCells);
            }
          }
        }        
      }
    }

    // FIME(hbs): we could compute the bbox area and the coverage area, if it differs and the resolution was initially set to 0
    // i.e. we were asked to guess, then we could increase it and try again so as to have a better area ratio.
        
    return coverage;
  }

  public static final Coverage coverPolygon(List<Long> verticesLat, List<Long> verticesLon, int resolution, Coverage coverage) {
    return coverPolygon(verticesLat, verticesLon, resolution, coverage, null, false);
  }
  
  /**
   * Cover a line with cells.
   * 
   * We invert to/from so the longitudes go increasing.
   * 
   * The algorithm goes like this:
   * 
   * Start with the first point of the line. Given the resolution, we are in a cell with a certain
   * side.
   * 
   * We need to determine on what side (north/east/south or ne/se corner) the line exits the cell, this will then
   * determine what next cell to add to the coverage.
   * 
   * Once this is determined, we move the current point to the entering point in the next cell and start over
   * until the end of the line is reached.
   * 
   * @param from Origin HHCode of line
   * @param to   Final HHCode of line
   * @param coverage Coverage to add the included cells in
   * @param resolution resolution to use
   * @param geocells optional array of Geocells to intersect / exclude
   * @param excludeGeoCells if true, exclude 'geocells', otherwise intersect.
   */
  
  public static final void coverLine(long from, long to, Coverage coverage, int resolution, long[] geocells, boolean excludeGeoCells) {
    long[] A = splitHHCode(from);
    long[] B = splitHHCode(to);
    
    coverLine(A[0], A[1], B[0], B[1], coverage, resolution, geocells, excludeGeoCells);
  }

  public static final void coverLine(long from, long to, Coverage coverage, int resolution) {
    coverLine(from, to, coverage, resolution, null, false);
  }
  
  /**
   * Compute coverage covering a line.
   * 
   * @param fromLat HH latitude of line origin
   * @param fromLon HH longitude of line origin
   * @param toLat HH latitude of final point
   * @param toLon HH longitude of final point
   * @param coverage Add the included cells to this coverage
   * @param resolution The resolution at which to do the covering. If the resolution is 0, compute one from the bbox.
   *                   If resolution is < 0, compute optimal resolution from bbox then substract 'resolution', so 20 becomes 22 if resolution is -2
   * @param geocells optional array of Geocells to intersect / exclude
   * @param excludeGeoCells if true, exclude 'geocells', otherwise intersect.
   */
  public static final void coverLine(long fromLat, long fromLon, long toLat, long toLon, Coverage coverage, int resolution, long[] geocells, boolean excludeGeoCells) {
    
    if (resolution <= 0) {
      long[] bbox = new long[4];
      bbox[0] = fromLat;
      bbox[1] = fromLon;
      bbox[2] = toLat;
      bbox[3] = toLon;
      
      resolution = getOptimalPolylineResolution(bbox, resolution);
    }
    
    //
    // Swap A and B if not increasing lon
    //
    
    if (fromLon > toLon) {
      long t = fromLat;
      fromLat = toLat;
      toLat = t;
      t = fromLon;
      fromLon = toLon;
      toLon = t;
    }
    
    //
    // Compute deltaLat and deltaLon
    //
    
    long dlat = Math.abs(toLat - fromLat);
    long dlon = Math.abs(toLon - fromLon);
    
    //
    // Determine if line is going north
    //
    
    long north = toLat - fromLat;    
    
    //
    // Handle the case of vertical and horizontal lines
    //

    long offset = 1L << (32 - resolution);
    long offsetmask = offset - 1;
    long prefixmask = 0xffffffffffffffffL ^ offsetmask;

    if (0 == north) {
      // Horizontal line
      long lat = fromLat;
      long lon = fromLon;
      
      while((lon & prefixmask) < toLon) {
        coverage.addCell(resolution, lat, lon, geocells, excludeGeoCells);
        lon += offset;
      }
    } else if (0 == toLon - fromLon) {
      // Vertical line
      
      long lat = fromLat;
      long lon = fromLon;
      
      if (north > 0) {
        while((lat & prefixmask) < toLat) {
          coverage.addCell(resolution, buildHHCode(lat, lon), geocells, excludeGeoCells);
          lat += offset;
        }        
      } else {
        while((lat | offsetmask) > toLat) {
          coverage.addCell(resolution, buildHHCode(lat, lon), geocells, excludeGeoCells);
          lat -= offset;
        }        
      }
    } else {
      long lat = fromLat;
      long lon = fromLon;

      boolean cont = true;

      while (cont) {
        coverage.addCell(resolution, lat, lon, geocells, excludeGeoCells);

        //
        // determine if the slope from the current point to the corner of the
        // cell in the direction of the slope is >, < or = to the line slope
        //
        
        long latoffset = north > 0 ? (lat | offsetmask) + 1 - lat : lat - (lat & prefixmask) + 1;
        long lonoffset = (lon | offsetmask) + 1 - lon;
        
        long latoffdlon = latoffset * dlon;
        long lonoffdlat = lonoffset * dlat;
        long delta = latoffdlon - lonoffdlat;
        
        if (delta > 0) {
          //
          // Slope from current point to upper(lower) right corner
          // is more than line's slope (i.e. the line will intersect with the east border of the cell)
          //
          
          // Going east
          lat = lat + Long.signum(north) * (lonoffdlat / dlon);
          lon = (lon | offsetmask) + 1;
        } else if (delta < 0) {
          // Going north / south
          if (north > 0) {
            lat = (lat | offsetmask) + 1;
          } else {
            lat = (lat & prefixmask) - 1;
          }
          lon = lon + (latoffdlon / dlat);
        } else {
          // Going north/east or south/east          
          if (north > 0) {
            lat = (lat | offsetmask) + 1;
          } else {
            lat = (lat & prefixmask) - 1;
          }
          lon = (lon | offsetmask) + 1;
        }        
        
        if (north > 0) {
          cont = ((lat & prefixmask) < toLat) && ((lon & prefixmask) < toLon);
        } else {
          cont = ((lat | offsetmask) > toLat) && ((lon & prefixmask) < toLon);
        }
        
        // We have this safety net in case the slope is so slow that the latitude does not grow,
        // so if we stay in the same cell twice, we end the loop.
//        if (((lon & prefixmask) >= B[1]) && next == hhcode) {
//          cont = false;
//        }
      }
    }
  }

  public static final void coverLine(long fromLat, long fromLon, long toLat, long toLon, Coverage coverage, int resolution) {
    coverLine(fromLat, fromLon, toLat, toLon, coverage, resolution, null, false);
  }
  
  public static final Coverage coverPolyline(List<Long> nodes, int resolution, boolean useBresenham, long[] geocells, boolean excludeGeoCells) {
    List<Long> lat = new ArrayList<Long>();
    List<Long> lon = new ArrayList<Long>();
    
    long[] coords = new long[2];
    
    for (long hhcode: nodes) {
      HHCodeHelper.stableSplitHHCode(hhcode, MAX_RESOLUTION, coords);
      lat.add(coords[0]);
      lon.add(coords[1]);
    }
    
    return coverPolyline(lat, lon, resolution, false, useBresenham, geocells, excludeGeoCells);
  }

  public static final Coverage coverPolyline(List<Long> nodes, int resolution, boolean useBresenham) {
    return coverPolyline(nodes, resolution, useBresenham, null, false);
  }
  
  public static final Coverage coverPolyline(List<Long> lat, List<Long> lon, int resolution, boolean perSegmentResolution, boolean useBresenham, long[] geocells, boolean excludeGeoCells) {
    //
    // Initialize the coverage
    //
    Coverage coverage = new Coverage();
    return coverPolyline(lat, lon, resolution, perSegmentResolution, useBresenham, coverage, geocells, excludeGeoCells);
  }

  public static final Coverage coverPolyline(List<Long> lat, List<Long> lon, int resolution, boolean perSegmentResolution, boolean useBresenham) {
    return coverPolyline(lat, lon, resolution, perSegmentResolution, useBresenham, null, false);
  }
  
  public static final Coverage coverPolyline(List<Long> lat, List<Long> lon, int resolution, boolean perSegmentResolution, boolean useBresenham, Coverage coverage, long[] geocells, boolean excludeGeoCells) {
    
    int resoffset = resolution;
        
    if (useBresenham) {
      coverPolylineBresenham(lat, lon, resolution, perSegmentResolution, coverage, geocells, excludeGeoCells);
    } else {
      //
      // Determine global resolution
      //
      
      if (resoffset <= 0 && !perSegmentResolution) {
        long[] bbox = getBoundingBox(lat, lon);
        resolution = getOptimalPolylineResolution(bbox, resoffset);
      }
      
      List<Long> segmentLat = new ArrayList<Long>();
      List<Long> segmentLon = new ArrayList<Long>();
      
      segmentLat.add(lat.get(0));
      segmentLat.add(lat.get(0));
      segmentLon.add(lon.get(0));
      segmentLon.add(lon.get(0));
      
      for (int i = 0; i <= Math.min(lat.size(),lon.size()) - 2; i++) {
        //
        // Shift vertex
        //
        
        segmentLat.remove(0);
        segmentLon.remove(0);
        
        segmentLat.add(lat.get(i + 1));
        segmentLon.add(lon.get(i+1));
        
        if (perSegmentResolution) {
          long[] bbox = getBoundingBox(segmentLat, segmentLon);
          resolution = getOptimalPolylineResolution(bbox, resoffset);          
        }
        
        coverLine(segmentLat.get(0), segmentLon.get(0), segmentLat.get(1), segmentLon.get(1), coverage, resolution, geocells, excludeGeoCells);
      }
    }
    
    return coverage;
  }

  public static final Coverage coverPolyline(List<Long> lat, List<Long> lon, int resolution, boolean perSegmentResolution, boolean useBresenham, Coverage coverage) {
    return coverPolyline(lat, lon, resolution, perSegmentResolution, useBresenham, coverage, null, false);
  }
  
  private static void coverPolylineBresenham(List<Long> lats, List<Long> lons, int resolution, boolean perSegmentResolution, Coverage coverage, long[] geocells, boolean excludeGeoCells) {
    
    //
    // Compute offset for lat/lon
    //
  
    long offset = 1L << (32 - resolution);
  
    //
    // Loop over the edges and apply the Bresenham's algorithm for each one
    // Adapted from http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    //

    long[] from = new long[2];
    long[] to = new long[2];
  
    int resoffset = resolution;
    
    if (resoffset <= 0 && !perSegmentResolution) {
      long[] bbox = getBoundingBox(lats, lons);
      resolution = getOptimalPolylineResolution(bbox, resoffset);
    }
    
    List<Long> segmentLat = null;
    List<Long> segmentLon = null;
    
    if (perSegmentResolution) {
      segmentLat = new ArrayList<Long>();
      segmentLon = new ArrayList<Long>();
      
      segmentLat.add(lats.get(0));
      segmentLat.add(lats.get(0));
      segmentLon.add(lons.get(0));
      segmentLon.add(lons.get(0));
    }

    for (int i = 0; i <= Math.min(lats.size(), lons.size()) - 2; i++) {

      if (perSegmentResolution) {
        //
        // Shift vertex
        //
        
        segmentLat.remove(0);
        segmentLon.remove(0);
        
        segmentLat.add(lats.get(i + 1));
        segmentLon.add(lons.get(i+1));
        
        long[] bbox = getBoundingBox(segmentLat, segmentLon);
        resolution = getOptimalPolylineResolution(bbox, resoffset);          
      }

      from[0] = lats.get(i);
      from[1] = lons.get(i);
      to[0] = lats.get(i+1);
      to[1] = lons.get(i+1);
      
      //
      // Determine if line is steep, i.e. its delta in lat is > than its delta in lon
      //
    
      boolean steep = Math.abs(to[0] - from[0]) > Math.abs(to[1] - from[1]);
    
      //
      // If the line is steep, exchange lat/lon
      //
    
      if (steep) {
        long t = from[1]; from[1] = from[0]; from[0] = t;
        t = to[1]; to[1] = to[0]; to[0] = t;
      }
    
      // If end point is on the right of the starting point, swap them
    
      if (from[1] > to[1]) {
        long t = from[1]; from[1] = to[1]; to[1] = t;
        t = from[0]; from[0] = to[0]; to[0] = t;        
      }

      long deltalat = Math.abs(to[0] - from[0]);
      long deltalon = to[1] - from[1];
    
      long error = deltalon >> 2; // was 1
  
      long lat = from[0];
    
      long latstep = (from[0] < to[0]) ? offset : -offset;
    
      long lon = from[1];
    
      long prefixmask = 0xffffffffffffffffL ^ (offset - 1);
    
      while ((lon & prefixmask) <= to[1]) {
      
        if (steep) {
          coverage.addCell(resolution, lat, lon, geocells, excludeGeoCells);
        
          // Add 8 cells around
          /*
          coverage.get(resolution).add(buildHHCode(lon + offset, lat,32));
          coverage.get(resolution).add(buildHHCode(lon - offset, lat,32));
          coverage.get(resolution).add(buildHHCode(lon, lat + offset,32));
          coverage.get(resolution).add(buildHHCode(lon, lat - offset,32));
          coverage.get(resolution).add(buildHHCode(lon + offset, lat + offset,32));
          coverage.get(resolution).add(buildHHCode(lon + offset, lat - offset,32));
          coverage.get(resolution).add(buildHHCode(lon - offset, lat + offset,32));
          coverage.get(resolution).add(buildHHCode(lon - offset, lat - offset,32));
           */
        } else {
          coverage.addCell(resolution, lat,lon, geocells, excludeGeoCells);

        /*
        coverage.get(resolution).add(buildHHCode(lat + offset, lon,32));
        coverage.get(resolution).add(buildHHCode(lat - offset, lon,32));
        coverage.get(resolution).add(buildHHCode(lat, lon + offset,32));
        coverage.get(resolution).add(buildHHCode(lat, lon - offset,32));
        coverage.get(resolution).add(buildHHCode(lat + offset, lon + offset,32));
        coverage.get(resolution).add(buildHHCode(lat + offset, lon - offset,32));
        coverage.get(resolution).add(buildHHCode(lat - offset, lon + offset,32));
        coverage.get(resolution).add(buildHHCode(lat - offset, lon - offset,32));
        */
        }
      
        error = error - deltalat;
      
        if (error < 0) {
          lat = lat + latstep;
          error = error + deltalon;
        }
      
        lon += offset;
      }
    }
  }
  
  private static void coverPolylineBresenham(List<Long> lats, List<Long> lons, int resolution, boolean perSegmentResolution, Coverage coverage) {
    coverPolylineBresenham(lats, lons, resolution, perSegmentResolution, coverage, null, false);
  }
  
  private static final void mergeCoverages(Map<Integer,List<Long>> a, Map<Integer,List<Long>> b) {
    //
    // For each resolution, add zones of b to those of a
    //
    
    for (int resolution: b.keySet()) {
      if (!a.containsKey(resolution)) {
        a.put(resolution, new ArrayList<Long>());
      }
      a.get(resolution).addAll(b.get(resolution));
    }
  }
    
  public static final void dumpCoverage(Map<Integer,List<Long>> coverage) {

    boolean first = true;
    long last = 0;
    
    for (int resolution: coverage.keySet()) {
      System.out.print(resolution + ": ");
      for (long hhcode: coverage.get(resolution)) {
        if (!first) {
          // Skip duplicates
          if (last == (hhcode & (0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution)) - 1))))) {
            continue;
          }
          System.out.print(" ");
        }
        System.out.print(toString(hhcode,resolution));
        last = hhcode & (0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution)) - 1)));
        first = false;
      }
      System.out.println();
    }
    
  }
  
  public static final Coverage coverRectangle(final double swlat, final double swlon, final double nelat, final double nelon, long[] geocells, boolean excludeGeoCells) {
    return coverRectangle(swlat, swlon, nelat, nelon, 0, geocells, excludeGeoCells);
  }
  public static final Coverage coverRectangle(final double swlat, final double swlon, final double nelat, final double nelon) {
    return coverRectangle(swlat, swlon, nelat, nelon, null, false);
  }
  
  public static final Coverage coverRectangle(final double swlat, final double swlon, final double nelat, final double nelon, int resolution) {
    return coverRectangle(swlat, swlon, nelat, nelon, resolution, null, false);
  }
  
  public static final Coverage coverRectangle(final double swlat, final double swlon, final double nelat, final double nelon, int resolution, long[] geocells, boolean excludeGeoCells) {
    
    List<Long> lat = new ArrayList<Long>();
    List<Long> lon = new ArrayList<Long>();
        
    lat.add(toLongLat(swlat));
    lat.add(toLongLat(swlat));
    lat.add(toLongLat(nelat));
    lat.add(toLongLat(nelat));
    
    lon.add(toLongLon(swlon));
    lon.add(toLongLon(nelon));
    lon.add(toLongLon(nelon));
    lon.add(toLongLon(swlon));
            
    return coverPolygon(lat, lon, resolution, geocells, excludeGeoCells);
    
    /*
    //
    // Take care of the case when the bbox contains the international date line
    //
        
    if (swlon > nelon) {
      // If sign differ, consider we crossed the IDL
      if (swlon * nelon <= 0) {
        
        Coverage a = coverPolygon(new ArrayList<Long>() {{
          add(getHHCodeValue(swlat, swlon));
          add(getHHCodeValue(nelat, swlon));
          add(getHHCodeValue(nelat,180.0));
          add(getHHCodeValue(swlat,180.0));
        }}, resolution);
        
        Coverage b = coverPolygon(new ArrayList<Long>() {{
          add(getHHCodeValue(swlat, nelon));
          add(getHHCodeValue(nelat, nelon));
          add(getHHCodeValue(nelat,-180.0));
          add(getHHCodeValue(swlat,-180.0));
        }}, resolution);

        a.merge(b);
        
        return a;
      } else {
        // Reorder minLat/minLon/maxLat/maxLon as they got tangled for an unknown reason!
        double lat = Math.max(swlat,nelat);
        final double swlat2 = Math.min(swlat, nelat);
        final double nelat2 = lat;
          
        double lon = Math.max(swlon,nelon);
        final double swlon2 = Math.min(swlon,nelon);
        final double nelon2 = lon;
        
        return coverPolygon(new ArrayList<Long>() {{
          add(getHHCodeValue(swlat2,swlon2));
          add(getHHCodeValue(swlat2,nelon2));
          add(getHHCodeValue(nelat2,nelon2));
          add(getHHCodeValue(nelat2,swlon2));
        }}, resolution);
      }
    }
    
    return coverPolygon(new ArrayList<Long>() {{
      add(getHHCodeValue(swlat,swlon));
      add(getHHCodeValue(swlat,nelon));
      add(getHHCodeValue(nelat,nelon));
      add(getHHCodeValue(nelat,swlon));
    }}, resolution);
    */    
  }
  
  /**
   * Resample a polyline, merging adjacent nodes that map to the same cell at the
   * given resolution.
   * 
   * @param nodes Nodes to resample
   * @param resolution Resolution to resample at
   * @return The list of resampled nodes. Each resampled node is placed in the center of its containing cell.
   */
  public static final List<Long> resamplePolyline(List<Long> nodes, int resolution) {
    
    List<Long> resampled = new ArrayList<Long>();
    
    long resolutionmask = (0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution)) - 1)));
    
    // Mask to OR with node to center it in the cell (only valid for resolutions < 32)
    long centermask = 32 == resolution ? 0L : 0xcL << (2 * (32 - 2 - resolution));
    long lastnode = 0L;
    
    boolean first = true;
    
    for (long node: nodes) {
      if (!first && (node & resolutionmask) == lastnode) {
        continue;
      }
      first = false;
      resampled.add((node & resolutionmask) | centermask);
      lastnode = node & resolutionmask;
    }
    
    return resampled;
  }
  
  public static final String toIndexableString(long hhcode) {
    return toIndexableString(hhcode, 2, 30);
  }

  /**
   * Return multiple resolutions of an HHCode in a form that will be
   * indexed by the GeoXP lucene analyzer.
   * 
   * @param hhcode
   * @param minResolution
   * @param maxResolution
   * @return
   */
  public static final String toIndexableString(long hhcode, int minResolution, int maxResolution) {
    StringBuilder sb = new StringBuilder();
    sb.append(Long.toHexString(hhcode));
    
    // Pad with leading 0s
    while(sb.length() < 16) {
      sb.insert(0, "0");
    }
   
    if (minResolution < 2) {
      minResolution = 2;
    }
    if (maxResolution > 30) {
      maxResolution = 30;
    }
    if (minResolution > maxResolution) {
      int tmp = minResolution;
      minResolution = maxResolution;
      maxResolution = tmp;
    }
    
    for (int i = minResolution; i <= maxResolution; i += 2) {
      sb.append(" ");
      sb.append(sb.subSequence(0, i >> 1));
    }
    
    return sb.toString();
  }
  
  /**
   * Return the long lat/lon of the center of a HHCode cell
   * at a given resolution.
   * 
   * @param hhcode
   * @param resolution
   * @return
   */
  public static long[] center(long hhcode, int resolution) {
    long[] ll = splitHHCode(hhcode);
    long mask = ((1L << (32 - resolution)) - 1) >> 1;
    ll[0] |= mask;
    ll[1] |= mask;
    return ll;    
  }
  
  public static double toLat(long longLat) {
    return DEGREES_PER_LAT_UNIT * longLat - 90.0;
  }
  public static double toLon(long longLon) {
    return DEGREES_PER_LON_UNIT * longLon - 180.0;
  }

  /**
   * Return the HH coordinate of the latitude.
   * HH Coordinates represent the id of the interval the degree lat falls in.
   * Interval 0 is [0,degreesPerLatUnit[.
   * Negative intervals are offset by -1.
   * There is an exception for 90 degrees which is considered in the last interval that
   * fits in 32 bits instead of the first to fit in 31.
   * 
   * @param lat
   * @return
   */
  public static long toLongLat(double lat) {
    
    long longLat = 0;
    
    if (lat == 90.0) {
      longLat = (1L << MAX_RESOLUTION) - 1;
    } else {
      longLat = (long) ((lat + 90.0) / DEGREES_PER_LAT_UNIT);
    }
    
    // Offset negative values to the bottom if they landed on 0
    if (lat + 90.0 < 0.0 && longLat == 0) {
      longLat--;
    }
    
    return longLat;
  }
  
  public static long toLongLon(double lon) {
    
    long longLon = 0;
    
    if (lon == 180.0) {
      longLon = (1L << MAX_RESOLUTION) - 1;
    } else {
      longLon = (long) ((lon + 180.0) / DEGREES_PER_LON_UNIT);
    }
    
    // Offset negative values to the left if they landed on 0
    if (lon + 180.0 < 0.0 && longLon == 0) {
      longLon--;
    }
    
    return longLon;
  }

  /**
   * Return the number of latitude/longitude units covering one meter at the given latitude.
   * 
   * @param hhcode Point where scale should be computed.
   * @return An array containing both scales.
   */
  public static long[] getScale(long hhcode)  {
    double[] latlon = HHCodeHelper.getLatLon(hhcode, HHCodeHelper.MAX_RESOLUTION);
    final long[] scales = new long[2];
    
    //
    // At latitute phi, the scale is cos(phi).
    //
    
    double scale = Math.cos(Math.toRadians(latlon[0]));
    
    // Latitude scale is not altered.
    scales[0] = Math.round(latUnitsPerMeter);
    // Longitude scale is altered by latitude, the bigger the latitude, the more units per meter as circles get smaller
    scales[1] = Math.round(lonUnitsPerMeter / scale);
    
    return scales;
  }

  public static long[] getScale(long lat, long lon) {
    return getScale(HHCodeHelper.buildHHCode(lat, lon, HHCodeHelper.MAX_RESOLUTION));
  }
  
  /**
   * Compute the distance in meters between two positions given a precomputed scale array.
   * 
   * @param from
   * @param to
   * @return The squared distance in meters between the two points.
   */
  public static double getSquaredDistance(long from, long to, long[] scales) {
    long[] f = HHCodeHelper.splitHHCode(from);
    long[] t = HHCodeHelper.splitHHCode(to);
    
    double deltaLat = Math.abs(((double) (f[0] - t[0])) / latUnitsPerMeter);
    double deltaLon = Math.abs(((double) (f[1] - t[1])) / lonUnitsPerMeter);
    
    return deltaLat*deltaLat + deltaLon*deltaLon;
  }

  /**
   * Optimized version of getSquaredDistance(long,long,long[]) where the hhcodes
   * have already been split.
   * 
   * @param from
   * @param to
   * @param scales
   * @return
   */
  public static double getSquaredDistance(long[] from, long[] to, long[] scales) {
    double deltaLat = Math.abs(((double) (from[0] - to[0])) / latUnitsPerMeter);
    double deltaLon = Math.abs(((double) (from[1] - to[1])) / lonUnitsPerMeter);
    
    return deltaLat*deltaLat + deltaLon*deltaLon;
  }
  
  /**
   * Return the bounding box of the given hhcode at the given resolution.
   * 
   * @param hhcode HHCode for which to compute the bbox.
   * @param resolution Resolution to consider.
   * @return An array of doubles representing the lat/lon of ll(sw)/ur(ne) corners of the bbox.
   */
  public static double[] getHHCodeBBox(long hhcode, int resolution) {
    
    // Split HHCode in lat/lon
    long[] ll = splitHHCode(hhcode);
    
    // Compute 'offset' mask for both lat/lon.
    // This is the mask to apply to retrieve the value within the cell.
    // This mask is 2**(32 - resolution) - 1.
    // 
    long offsetmask = ((1L << (32 - resolution)) - 1);
    
    double[] bbox = new double[4];

    // Compute top/right limit of bbox (lower bits set to 1)
    ll[0] |= offsetmask;
    ll[1] |= offsetmask;

    bbox[2] = ll[0] * DEGREES_PER_LAT_UNIT - 90.0;
    bbox[3] = ll[1] * DEGREES_PER_LON_UNIT - 180.0;

    // Now compute bottom/left limit of bbox (lower bits set to 0)
    ll[0] ^= offsetmask;
    ll[1] ^= offsetmask;
    
    bbox[0] = ll[0] * DEGREES_PER_LAT_UNIT - 90.0;
    bbox[1] = ll[1] * DEGREES_PER_LON_UNIT - 180.0;
    
    return bbox;
  }
  
  /**
   * Return a HHCode from its String representation.
   * 
   * @param hhstr String rep of HHCode.
   * @return
   */
  public static long fromString(String hhstr) {
    int resolution = hhstr.length() * 2;
    long hhcode = 0L;
    
    if (32 != resolution) {
      hhcode = Long.parseLong(hhstr, 16);
      hhcode <<= 2 * (32 - resolution);
    } else {
      hhcode = new BigInteger(hhstr, 16).longValue();
    }
    
    return hhcode;
  }
  
  /**
   * Return a Coverage covering a polygon around the given segment, at a distance of 'D' at the specified resolution.
   * 
   * +------------------------+
   * +            D           |
   * + D ================== D |
   * +            D           |
   * +------------------------+
   *
   * This is used to compute a coverage covering a certain distance along a path.
   * 
   * @param from First end of the segment
   * @param to Second end of the segment
   * @param distance Distance (D) in meters from the segment
   * @param resolution
   * @return
   */
  public static Coverage coverSegment(long from, long to, double distance, int resolution, long[] geocells, boolean excludeGeoCells) {
    long[] fromcoords = splitHHCode(from, MAX_RESOLUTION);
    long[] tocoords = splitHHCode(to, MAX_RESOLUTION);
    
    return coverSegment(fromcoords[0], fromcoords[1], tocoords[0], tocoords[1], distance, resolution, geocells, excludeGeoCells);
  }
  
  public static Coverage coverSegment(long from, long to, double distance, int resolution) {
    return coverSegment(from, to, distance, resolution, null, false);
  }
  
  public static Coverage coverSegment(long fromLat, long fromLon, long toLat, long toLon, double distance, int resolution, long[] geocells, boolean excludeGeoCells) {
    return coverSegment(fromLat, fromLon, toLat, toLon, distance, resolution, new Coverage(), null, false);
  }
  
  public static Coverage coverSegment(long fromLat, long fromLon, long toLat, long toLon, double distance, int resolution) {
    return coverSegment(fromLat, fromLon, toLat, toLon, distance, resolution, null, false);
  }
  
  public static Coverage coverSegment(long fromLat, long fromLon, long toLat, long toLon, double distance, int resolution, Coverage coverage, long[] geocells, boolean excludeGeoCells) {
    //
    // Split 'to' and 'from'
    //
    
    long[] fromcoords = new long[2];
    long[] tocoords = new long[2];
    
    fromcoords[0] = fromLat;
    fromcoords[1] = fromLon;
    
    tocoords[0] = toLat;
    tocoords[1] = toLon;
    
    
    // Compute scale at center
    
    long[] scales = getScale(buildHHCode((tocoords[0] + fromcoords[0]) / 2, (tocoords[1] + fromcoords[1]) / 2));
    
    //
    // Compute coordinates of vector from->to (ftvector)
    // and its orthogonal counterpart oftvector
    //
    
    long[] ftvector = new long[2];
    long[] oftvector = new long[2];
    
    ftvector[0] = tocoords[0] - fromcoords[0];
    ftvector[1] = tocoords[1] - fromcoords[1];
    
    oftvector[0] = -ftvector[1];
    oftvector[1] = ftvector[0];
    
    //
    // Compute the length of ftvector/oftvector in meters. This is an approximation at the
    // average latitude and using cartesian geometry, not trigonometric.
    //
        
    double ftlen = Math.sqrt(Math.pow(ftvector[0] / latUnitsPerMeter, 2.0) + Math.pow(ftvector[1] / scales[1], 2.0));
    double oftlen = Math.sqrt(Math.pow(oftvector[0] / latUnitsPerMeter, 2.0) + Math.pow(oftvector[1] / scales[1], 2.0));

    List<Long> verticesLat = new ArrayList<Long>(4);
    List<Long> verticesLon = new ArrayList<Long>(4);
    
    //
    // Build the polygon ABCD
    //
    //
    //    (A)                                  (B)
    //      +-D-+-----------ftlen----------+-D-+
    //      D                             to   D
    //      +   +==========================+   +
    //      D   from                           D
    //      +-D-+-----------ftlen----------+-D-+
    //    (D)                                  (C)
    //
    
    long[] A = new long[2];
    long[] B = new long[2];
    long[] C = new long[2];
    long[] D = new long[2];
    
    A[0] = fromcoords[0] + (long) (oftvector[0] * (distance / oftlen) - ftvector[0] * ( distance / ftlen));
    A[1] = fromcoords[1] + (long) (oftvector[1] * (distance / oftlen) - ftvector[1] * ( distance / ftlen));
   
    B[0] = A[0] + (long) (ftvector[0] * ((ftlen + 2.0 * distance) / ftlen));
    B[1] = A[1] + (long) (ftvector[1] * ((ftlen + 2.0 * distance) / ftlen));
    
    C[0] = B[0] - (long) (oftvector[0] * (2.0 * distance / oftlen));
    C[1] = B[1] - (long) (oftvector[1] * (2.0 * distance / oftlen));
    
    D[0] = C[0] - (long) (ftvector[0] * ((ftlen + 2.0 * distance) / ftlen));
    D[1] = C[1] - (long) (ftvector[1] * ((ftlen + 2.0 * distance) / ftlen));
    
    verticesLat.add(A[0]);
    verticesLon.add(A[1]);

    verticesLat.add(B[0]);
    verticesLon.add(B[1]);

    verticesLat.add(C[0]);
    verticesLon.add(C[1]);

    verticesLat.add(D[0]);
    verticesLon.add(D[1]);
    
    return coverPolygon(verticesLat, verticesLon, resolution, coverage, geocells, excludeGeoCells);
  }

  public static Coverage coverSegment(long fromLat, long fromLon, long toLat, long toLon, double distance, int resolution, Coverage coverage) {
    return coverSegment(fromLat, fromLon, toLat, toLon, distance, resolution, coverage, null, false);
  }
  
  public static double orthodromicDistance(long fromLat, long fromLon, long toLat, long toLon) {
    //
    // Compute orthodromic distance between endpoints
    // @see http://williams.best.vwh.net/avform.htm#Dist
    //
    
    double flat = fromLat * RADIANS_PER_LAT_UNIT - Math.PI / 2.0D;
    double flon = fromLon * RADIANS_PER_LON_UNIT - Math.PI;
    double tlat = toLat * RADIANS_PER_LAT_UNIT - Math.PI  / 2.0D;
    double tlon = toLon * RADIANS_PER_LON_UNIT - Math.PI;
    
    double d = 2.0D * Math.asin(Math.sqrt(Math.pow(Math.sin((flat-tlat)/2.0D), 2.0D) + Math.cos(flat)*Math.cos(tlat)*Math.pow(Math.sin((flon-tlon)/2.0D),2.0D)));
    
    return d;
  }
  
  /**
   * Return intermediate point on the great circle from 'from' to 'to'
   * 
   * @param fromLat HH lat of origin
   * @param fromLon HH lon of origin
   * @param toLat   HH lat of destination
   * @param toLon   HH lon of destination
   * @param fraction fraction ([0,1]) of the great circle whose lat/lon are to be computed.
   * @return
   */
  public static long[] gcIntermediate(long fromLat, long fromLon, long toLat, long toLon, double fraction) {
    
    //
    // We can't compute point if lat is not in -90/90
    //
    
    if (fromLat < 0 || toLat < 0 || fromLat >= (1L << 32) || toLat >= (1L <<32)) {
      return null;
    }
    
    //
    // We won't compute if longitude delta is more than 180 deg
    //
        
    if (Math.abs(fromLon - toLon) >= (1L << 31)) {
      return null;
    }

    long[] point = new long[2];
    
    //
    // If fraction is not in ]0,1[ return closest end point
    //
    
    if (fraction <= 0) {
      point[0] = fromLat;
      point[1] = fromLon;
      return point;
    } else if (fraction >= 1.0) {
      point[0] = toLat;
      point[1] = toLon;
      return point;
    }
    
    //
    // Compute offset so from OR to longitude falls within [0-2**32[
    //
    
    long lonoffset = 0L;
    
    if (fromLon < 0 && toLon < 0) {
      while (fromLon + lonoffset < 0) {
        lonoffset += (1L << 32);
      }
    } else if (fromLon >= (1L << 32) && toLon >= (1L << 32)) {
      while (fromLon + lonoffset >= (1L << 32)) {
        lonoffset -= (1L << 32);
      }
    }
    
    //
    // Offset longitudes
    //
    
    fromLon += lonoffset;
    toLon += lonoffset;
    
    //
    // Compute orthodromic distance between endpoints
    // @see http://williams.best.vwh.net/avform.htm#Dist
    //
    
    double flat = fromLat * RADIANS_PER_LAT_UNIT - Math.PI / 2.0D;
    double flon = fromLon * RADIANS_PER_LON_UNIT - Math.PI;
    double tlat = toLat * RADIANS_PER_LAT_UNIT - Math.PI  / 2.0D;
    double tlon = toLon * RADIANS_PER_LON_UNIT - Math.PI;
    
    double d = 2.0D * Math.asin(Math.sqrt(Math.pow(Math.sin((flat-tlat)/2.0D), 2.0D) + Math.cos(flat)*Math.cos(tlat)*Math.pow(Math.sin((flon-tlon)/2.0D),2.0D)));
    
    //System.out.println("OFFSET=" + lonoffset + " " + Math.toDegrees(flat) + "," + Math.toDegrees(flon) + "  " + Math.toDegrees(tlat) + "," + Math.toDegrees(tlon));
    //System.out.println("D=" + d);

    //
    // Compute intermediate position
    // @see http://williams.best.vwh.net/avform.htm#Intermediate
    //
    
    double sd = Math.sin(d);
    double A = Math.sin((1.0D - fraction) * d) / sd;
    double B = Math.sin(fraction * d) / sd;
    
    double x = A * Math.cos(flat) * Math.cos(flon) + B * Math.cos(tlat) * Math.cos(tlon);
    double y = A * Math.cos(flat) * Math.sin(flon) + B * Math.cos(tlat) * Math.sin(tlon);
    double z = A * Math.sin(flat) + B * Math.sin(tlat);
    
    double rlat = Math.atan2(z, Math.sqrt(x*x + y*y));
    double rlon = Math.atan2(y, x);
 
    //
    // Convert lat/lon to longs
    //
    
    long lat = (long) ((rlat + Math.PI / 2.0) / RADIANS_PER_LAT_UNIT);
    long lon = (long) ((rlon + Math.PI) / RADIANS_PER_LON_UNIT);
    
    //
    // Offset back
    //
    
    fromLon -= lonoffset;
    toLon -= lonoffset;
    lon -= lonoffset;
    
    //
    // Make sure we lie between from/to Lon
    //
    
    if (fromLon < toLon) {
      if (lon < fromLon) {
        lon += 1L << 32;
      } else if (lon > toLon) {
        lon -= 1L << 32;
      }
    } else {
      if (lon < toLon) {
        lon += 1L << 32;
      } else if (lon > fromLon) {
        lon -= 1L << 32;
      }
    }
    
    point[0] = lat;
    point[1] = lon;

    /*
    A=sin((1-f)*d)/sin(d)
    B=sin(f*d)/sin(d)
    x = A*cos(lat1)*cos(lon1) +  B*cos(lat2)*cos(lon2)
    y = A*cos(lat1)*sin(lon1) +  B*cos(lat2)*sin(lon2)
    z = A*sin(lat1)           +  B*sin(lat2)
    lat=atan2(z,sqrt(x^2+y^2))
    lon=atan2(y,x)
    */
    
    return point;
  }
  
  /**
   * Compute the orthodromic partition of a segment if the distance along
   * the rhumb line (loxodrome) is more than 'delta' times the distance along
   * the orthodromy.
   * 
   * If any endpoint's lat is not between [-90,90], then the original segment won't be split.
   * If the segment covers more than 180 degrees of longitude, the segment will first be
   * split in parts spanning no more than 180 degrees of longitude.
   * 
   * The return value is a list of coordinates (lat/long) of the segment transformation.
   *
   * @param from lat/lon of 'from' endpoint
   * @param to   lat/lon of 'to' endpoint
   * @param delta distance delta under which orthodromization won't be attempted
   * @return
   */
  public static List<Long> orthodromize(long fromLat, long fromLon, long toLat, long toLon, double delta) {
    
    //
    // Create a list that will hold the orthodromized segment
    //
    
    List<Long> result = new ArrayList<Long>();

    result.add(fromLat);
    result.add(fromLon);
    result.add(toLat);
    result.add(toLon);

    long[] coords = new long[4];
    
    coords[0] = fromLat;
    coords[1] = fromLon;
    coords[2] = toLat;
    coords[3] = toLon;
    
    //
    // Don't orthodromize if any lat is not in [-90,90]
    //
    
    if ((fromLat < 0 || fromLat > (1L << 32))
        || (toLat < 0 || toLat > (1L << 32))) {
      return result; 
    }
    
    int i = 0;
    
    //
    // Iterate over the result list.
    //
    
    while (i < result.size() - 2) {
      //
      // If lon span is more than 180 degrees then proceed
      // with splitting the segment by inserting a point
      //
      
      long dlon = Math.abs(result.get(i + 1) - result.get(i + 3));
      
      if (dlon > ((1L << 31) -1)) {
        //
        // Insert an intermediate point at 179.9999 degrees of lon
        // and orthodromize both
        //
        
        double ratio = ((1L << 31) - 100) / (1L << 31);
        long interLat = (long)(result.get(i) * (1.0D - ratio) + ratio * result.get(i + 2));
        long interLon = (long)(result.get(i + 1) * (1.0D - ratio) + ratio * result.get(i + 3));
        
        //
        // Add intermediate point
        //
        
        result.add(i + 2, interLon);
        result.add(i + 2, interLat);
        
        //
        // Continue iteration without shifting current point
        //

        //System.out.println("SPLIT " + i + ":" + result.size() + " ::= " + result.get(i) + "," + result.get(i + 1) + " >>> " + result.get(i + 2) + "," + result.get(i + 3) + " >>> " + result.get(i + 4) + "," + result.get(i + 5));

        continue;
      }
      
      //
      // Compute orthodromic (great circle) distance and rhumb line distance
      //
      
      //
      // Compute orthodromic distance between endpoints
      // @see http://williams.best.vwh.net/avform.htm#Dist
      //
      
      double flat = result.get(i) * RADIANS_PER_LAT_UNIT - Math.PI / 2.0D;
      double flon = result.get(i + 1) * RADIANS_PER_LON_UNIT - Math.PI;
      double tlat = result.get(i + 2) * RADIANS_PER_LAT_UNIT - Math.PI  / 2.0D;
      double tlon = result.get(i + 3) * RADIANS_PER_LON_UNIT - Math.PI;
      
      double gcd = 2.0D * Math.asin(Math.sqrt(Math.pow(Math.sin((flat-tlat)/2.0D), 2.0D) + Math.cos(flat)*Math.cos(tlat)*Math.pow(Math.sin((flon-tlon)/2.0D),2.0D)));

      //
      // Compute rhumb line distance
      // @see http://williams.best.vwh.net/avform.htm#Rhumb
      
      double q;
      
      if (Math.abs(tlat-flat) < TOLSQRT){
        q = Math.cos(flat);
      } else {
        q= (tlat-flat) / Math.log(Math.tan(tlat/2.0D + Math.PI/4.0D)/Math.tan(flat/2.0D + Math.PI/4.0D));
      }
      
      double rld= Math.sqrt((tlat-flat)*(tlat-flat) + (q*q)*(tlon-flon)*(tlon-flon));
      
      //
      // If the rhumb line distance is less than delta * orthodromic distance do nothing
      // to the current segment
      //
      
      double ratio = rld / gcd;
      
      if (ratio < delta) {
        //System.out.println("RATIO = " + ratio + " -> OK");
        i += 2;
        continue;
      }
      
      //
      // Insert the midpoint on the orthodromy
      //
      
      long[] midpoint = gcIntermediate(result.get(i), result.get(i + 1), result.get(i + 2), result.get(i + 3), 0.5D);

      result.add(i + 2, midpoint[1]);
      result.add(i + 2, midpoint[0]);

      //System.out.println(i + ":" + result.size() + " ::= " + result.get(i) + "," + result.get(i + 1) + " >>> " + result.get(i + 2) + "," + result.get(i + 3) + " >>> " + result.get(i + 4) + "," + result.get(i + 5));
      
      //
      // Continue iterating
      //
    }
    
    return result;
  }
  
  public static byte[] toByteArray(long hhcode) {
    byte[] bytes = new byte[8];
    
    for (int i = 7; i >= 0; i--) {
      bytes[i] = (byte) (hhcode & 0xff);
      hhcode >>= 8;      
    }
    
    return bytes;
  }
  
  public static long fromBytes(byte[] bytes) {
    long hhcode = 0L;
    
    for (int i = 0; i < 8; i++) {
      hhcode <<= 8;
      hhcode |= (bytes[i] & 0xffL);
    }
    
    return hhcode;
  }
}
