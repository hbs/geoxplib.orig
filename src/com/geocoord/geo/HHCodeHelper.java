package com.geocoord.geo;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HHCodeHelper {
  
  private static final double degreesPerLatUnit = 180.0 / (1L << 32);
  private static final double degreesPerLonUnit = 360.0 / (1L << 32);
  
  /**
   * Return the HHCode value of a combination of lat/lon expressed in degrees.
   * 
   * @param lat Latitude of point (-90.0/90.0)
   * @param lon Longitude of point (-180.0/180.0)
   * @return The computed HHCode value.
   */
  public static long getHHCodeValue(double lat, double lon) {
    // Shift lat/lon
    
    lat += 90.0;
    lon += 180.0;
    
    long[] coords = new long[2];
    
    coords[0] = Math.round(lat / degreesPerLatUnit);
    
    if (coords[0] > 0xffffffffL) {
      coords[0] = 0xffffffffL;
    }
    
    coords[1] = Math.round(lon / degreesPerLonUnit);
    if (coords[1] > 0xffffffffL) {
      coords[1] = 0xffffffffL;
    }
    
    return buildHHCode(coords[0], coords[1]);
  }
  
  /**
   * Return the hhcode of the cell above the given one, at the given resolution
   * 
   * @param hhcode HHCode of the original cell
   * @param resolution Resolution at which to do the math, from 1to 32
   * @return
   */
  public static long above(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // Add 1**(32 - resolution) to the lat
    //
    
    coords[0] = (coords[0] + (1 << (32 - resolution))) & 0xffffffffL;
    
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
  public static long below(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // Add 1**(32 - resolution) to the lat
    //
    
    coords[0] = (coords[0] - (1 << (32 - resolution))) & 0xffffffffL;
    
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
  public static long right(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // Add 1**(32 - resolution) to the lon
    //
    
    coords[1] = (coords[1] + (1 << (32 - resolution))) & 0xffffffffL;

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
  public static long left(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // Add 1**(32 - resolution) to the lon
    //
    
    coords[1] = (coords[1] - (1 << (32 - resolution))) & 0xffffffffL;

    //
    // Rebuild HHCode
    //
    
    return buildHHCode(coords[0], coords[1]);
  }


  /**
   * Split a HHCode value into its lat/lon components expressed as long.
   * 
   * @param hhcode HHCode value to split
   * @return An array of two longs (lat/lon)
   */
  public static long[] splitHHCode(long hhcode, int resolution) {
    long[] coords = new long[2];

    coords[0] = 0L;
    coords[1] = 0L;
    
    for (int i = 32 - 1; i >= 32 - resolution; i--) {
      coords[0] <<= 1;
      coords[0] |= 0x1L & (hhcode >> (1 + (i << 1)));
      coords[1] <<= 1;
      coords[1] |= 0x1L & (hhcode >> (i << 1));
    }
    
    coords[0] <<= 32 - resolution;
    coords[1] <<= 32 - resolution;

    return coords;
  }
  
  private static long[] splitHHCode(long hhcode) {
    return splitHHCode(hhcode, 32);
  }
  
  /**
   * Build a HHCode value by interleaving bits of lat and lon
   * 
   * @param coords long array of lat/lon
   * @return the HHCode value
   */
  private static long buildHHCode(long lat, long lon) {
    long hhcode = 0L;
    
    for (int i = 32 - 1; i >= 0; i--) {
         hhcode <<= 1;
         hhcode |= (lat & (1L << i)) >> i;
         hhcode <<= 1;
         hhcode |= (lon & (1L << i)) >> i;
    }

    return hhcode;
  }
  
  public static String toString(long hhcode) {
    return toString(hhcode, 32);
  }
  
  /**
   * Return a String representation of an hhcode at a given resolution
   * 
   * @param hhcode HHCode to represent
   * @param resolution resolution (event number between 2 and 32).
   * @return The string representation
   */
  public static String toString(long hhcode, int resolution) {
    
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
   * Optimize a coverage by merging clusters of adjacent cells
   *
   * @param coverage The coverage to optimize. It will be optimized in place.
   * @param thresholds Thresholds to consider when clustering cells. Threshold for resolution x
   *                   is stored on 4 bits (the upper 4 for resolution 2, then the next 4 for
   *                   resolution 4 ... then the lower 4 for resolution 32)
   */
  
  public static void optimize(Map<Integer,List<Long>> coverage, long thresholds) {
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

      Collections.sort(coverage.get(resolution));

      // Exit if resolution is 2
      if (2 == resolution) {
        break;
      }
      
      //
      // Compute mask to extract the prefix (i.e. n-4 bits wheren is the number of bits of this resolution)
      //
      
      long prefixmask = (0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution + 2))) - 1));
      long offsetmask = 0xfL << (2 * (32 - resolution));
            
      
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
      
      coverage.put(resolution, cells);
      resolution -= 2;
    }
  }
    
  /**
   * Determine a list of zones covering a polygon
   * 
   * @param vertices Vertices of the polygon (of hhcodes)
   * @param resolution The resolution at which to do the covering
   * 
   * @return A map keyed by resolution and whose The list of zones covering the polygon
   */
  public static Map<Integer,List<Long>> coverPolygon(List<Long> vertices, int resolution) {
    
    Map<Integer,List<Long>> coverage = new HashMap<Integer, List<Long>>();
    coverage.put(resolution, new ArrayList<Long>());
    
    //
    // Determine bounding box of the polygon
    //
    
    long topLat = Integer.MIN_VALUE;
    long leftLon = Integer.MAX_VALUE;
    long rightLon = Integer.MIN_VALUE;
    long bottomLat = Integer.MAX_VALUE;
    
    for (long vertex: vertices) {
      long[] coords = HHCodeHelper.splitHHCode(vertex, resolution);
      
      if (coords[0] < bottomLat) {
        bottomLat = coords[0];
      }
      if (coords[0] > topLat) {
        topLat = coords[0];
      }
      if (coords[1] < leftLon) {
        leftLon = coords[1];
      }
      if (coords[1] > rightLon) {
        rightLon = coords[1];
      }            
    }
    
    // Normalize bbox according to resolution, basically replace vertices with sw corner of enclosing zone
    
    topLat = topLat & (0xffffffff ^ ((1L << (32 - resolution)) - 1));
    bottomLat = bottomLat & (0xffffffff ^ ((1L << (32 - resolution)) - 1));
    leftLon = leftLon & (0xffffffff ^ ((1L << (32 - resolution)) - 1));
    rightLon = rightLon & (0xffffffff ^ ((1L << (32 - resolution)) - 1));
    
    //
    // @see http://alienryderflex.com/polygon_fill/
    //
    
    //
    // Loop from topLat to bottomLat
    //
    
    List<Long> nodeLon = new ArrayList<Long>();
    
    for (long lat = topLat; lat >= bottomLat; lat -= (1L << (32 - resolution))) {

      //
      // Scan the vertices
      //

      int j = vertices.size() - 1;
      
      nodeLon.clear();
      
      for (int i = 0; i < vertices.size(); i++) {
        long[] icoords = HHCodeHelper.splitHHCode(vertices.get(i), resolution);
        long[] jcoords = HHCodeHelper.splitHHCode(vertices.get(j), resolution);
        
        //if (icoords[0] < lat && jcoords[0] >= lat || jcoords[0] < lat && icoords[0] >= lat) {          
        if (icoords[0] > lat && jcoords[0] <= lat
            || jcoords[0] > lat && icoords[0] <= lat){
          nodeLon.add(icoords[1] + (lat-icoords[0])/(jcoords[0] - icoords[0]) * (jcoords[1] - icoords[1]));
        } else if(icoords[0] == jcoords[0] && lat == icoords[0]) {
          // Handle the case where the polygon edge is horizontal, we add the cells on the edge to the coverage
          for (long lon = Math.min(icoords[1],jcoords[1]); lon <= Math.max(icoords[1], jcoords[1]); lon += (1L << (32 - resolution))) {
            coverage.get(resolution).add(HHCodeHelper.buildHHCode(lat, lon));// & (0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution)) - 1))));            
          }
        }
        j = i;
      }
    
      // Sort nodeLon
      Collections.sort(nodeLon);

      // Add the zones between node pairs
      
      for (int i = 0; i < nodeLon.size(); i += 2) {
        for (long lon = nodeLon.get(i); lon <= nodeLon.get(i + 1); lon += (1L << (32 - resolution))) {
          // Add the zone, triming lower bits
          coverage.get(resolution).add(HHCodeHelper.buildHHCode(lat, lon));// & (0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution)) - 1))));
        }
      }
    }
    
    return coverage;
  }
  
  public static String getCoverageString(Map<Integer,List<Long>> coverage) {
    
    boolean first = true;
    StringBuilder sb = new StringBuilder();

    long last = 0;
    
    for (int resolution: coverage.keySet()) {
      for (long hhcode: coverage.get(resolution)) {
        if (!first) {
          // Skip duplicates
          if (last == (hhcode & (0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution)) - 1))))) {
            continue;
          }
          sb.append(" ");
        }
        sb.append(toString(hhcode,resolution));
        last = hhcode & (0xffffffffffffffffL ^ ((1L << (2 * (32 - resolution)) - 1)));
        first = false;
      }
    }
    
    return sb.toString();
  }  
}
