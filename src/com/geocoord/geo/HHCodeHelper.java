package com.geocoord.geo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
  
  public static final int MIN_RESOLUTION = 2;
  public static final int MAX_RESOLUTION = 32;
  
  /**
   * Number of degrees per unit of latitude.
   */
  private static final double degreesPerLatUnit = 180.0 / (1L << 32);
  
  /**
   * Number of degrees per unit of longitude.
   */
  private static final double degreesPerLonUnit = 360.0 / (1L << 32);
  
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
  public static final long northHHCode(long hhcode, int resolution) {
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
  public static final long southHHCode(long hhcode, int resolution) {
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
  public static final long eastHHCode(long hhcode, int resolution) {
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
  public static final long westHHCode(long hhcode, int resolution) {
    //
    // Split hhcode into lat/lon
    //
      
    long[] coords = splitHHCode(hhcode);
    
    //
    // Subtract 1**(32 - resolution) to the lon
    //
    
    coords[1] = (coords[1] - (1 << (32 - resolution))) & 0xffffffffL;

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
    
    coords[0] = (coords[0] + (1 << (32 - resolution))) & 0xffffffffL;
    coords[1] = (coords[1] + (1 << (32 - resolution))) & 0xffffffffL;

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
    
    coords[0] = (coords[0] - (1 << (32 - resolution))) & 0xffffffffL;
    coords[1] = (coords[1] + (1 << (32 - resolution))) & 0xffffffffL;

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
    
    coords[0] = (coords[0] - (1 << (32 - resolution))) & 0xffffffffL;
    coords[1] = (coords[1] - (1 << (32 - resolution))) & 0xffffffffL;

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
    
    coords[0] = (coords[0] - (1 << (32 - resolution))) & 0xffffffffL;
    coords[1] = (coords[1] + (1 << (32 - resolution))) & 0xffffffffL;

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
    long[] coords = splitHHCode(hhcode, resolution);
    double[] latlon = new double[2];
    latlon[0] = coords[0] * degreesPerLatUnit - 90.0;
    latlon[1] = coords[1] * degreesPerLonUnit - 180.0;
    
    return latlon;
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
   * Determine a list of zones covering a polygon. Polygon need not be closed (i.e. last vertex can be != from first vertex).
   * 
   * @param vertices Vertices of the polygon (of hhcodes)
   * @param resolution The resolution at which to do the covering. If the resolution is 0, compute one from the bbox
   * 
   * @return A map keyed by resolution and whose values are the list of zones covering the polygon
   */
  public static final Coverage coverPolygon(List<Long> vertices, int resolution) {
    
    // FIXME(hbs): won't work if the polygon lies on both sides of the international dateline
        
    //
    // Determine bounding box of the polygon
    //
    
    long topLat = Long.MIN_VALUE;
    long leftLon = Long.MAX_VALUE;
    long rightLon = Long.MIN_VALUE;
    long bottomLat = Long.MAX_VALUE;
    
    final long[] coords = new long[2];

    // List to gather the latitutes at which there are vertices
    Set<Long> verticesLat = new HashSet<Long>();
    
    for (long vertex: vertices) {
      HHCodeHelper.stableSplitHHCode(vertex, MAX_RESOLUTION, coords);
      
      verticesLat.add(coords[0]);
            
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
    
    if (0 >= resolution) {
      int resoffset = -resolution;
      
      long deltaLat = topLat - bottomLat;
      long deltaLon = rightLon - leftLon;
      
      // Extract log2 of the deltas and keep smallest
      // This log is the resolution we must use to have cells that are just a little smaller than 
      // the one we try to cover. We could use 'ceil' instead of 'floor' to have cells a little bigger.
               
      resolution = (int) Math.floor(Math.min(Math.log(deltaLat), Math.log(deltaLon))/Math.log(2.0));
      // Make log an even number.
      resolution = resolution & 0xfe;
      resolution = 32 - resolution;
      
      // Substract resoffset from computed resolution
      if (resolution + resoffset <= MAX_RESOLUTION) {
        resolution += resoffset;
        // Make resolution even
        resolution = resolution & 0x3e;
      }
    }

    long resolutionprefixmask = 0xffffffffL ^ ((1L << (32 - resolution)) - 1);
    long resolutionoffsetmask = (1L << (32 - resolution)) - 1;
    
    Coverage coverage = new Coverage();

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
 
    List<Long> nodeLon = new ArrayList<Long>(40);    
    List<Long> nodeLat = new ArrayList<Long>();  
    
    Set<Long> allLons = new HashSet<Long>();
    
    //
    // Add bottom of each cell from bottomLat to topLat
    //
    
    for (long lat = bottomLat & resolutionprefixmask; lat <= (topLat|resolutionoffsetmask); lat += 1L << (32 - resolution)) {
      verticesLat.add(lat & resolutionprefixmask);
      //verticesLat.add(lat | resolutionoffsetmask);
    }
    
    nodeLat.addAll(verticesLat);
    
    // Sort lats from bottom to top
    Collections.sort(nodeLat);

    // Loop over each cell bottom
    for (long lat: nodeLat) {
      //
      // Scan the vertices
      //

      // Close the path by referencing the last vertex
      int j = vertices.size() - 1;
      
      // Clear the intersections
      nodeLon.clear();
      
      // Clear the set of all Longitudes considered.
      // This is necessary because we might otherwise have odd number of lons because a node intersects the lat at a Lon which is at the end of a group of lons already considered
      allLons.clear();
      
      for (int i = 0; i < vertices.size(); i++) {
        HHCodeHelper.stableSplitHHCode(vertices.get(i), MAX_RESOLUTION, icoords);
        HHCodeHelper.stableSplitHHCode(vertices.get(j), MAX_RESOLUTION, jcoords);
        
        //
        // If the edge crosses the bottom of the cell (lat) OR the top of the cell (lat|resolutionoffsetmask), add all intersected cells in this row.
        //
        
        if (icoords[0] != jcoords[0]
            && ((icoords[0] >= lat && jcoords[0] <= lat || jcoords[0] >= lat && icoords[0] <= lat) // edge crosses the bottom of the row
               || ((icoords[0] >= (lat|resolutionoffsetmask) && jcoords[0] <= (lat|resolutionoffsetmask))
                   || (jcoords[0] >= (lat|resolutionoffsetmask) && icoords[0] <= (lat|resolutionoffsetmask))) // edge crosses the top of the row
               || (icoords[0] >= lat && icoords[0] <= (lat|resolutionoffsetmask) && jcoords[0] >= lat && jcoords[0] <= (lat|resolutionoffsetmask)))) { // edge is entirely in this row
          // Determine the lon of the cells at which the top and bottom lats intersect the edge
          
          long bottomIntersection = icoords[1] + (lat - icoords[0]) * (jcoords[1] - icoords[1]) /(jcoords[0] - icoords[0]);
          long topIntersection = icoords[1] + ((lat|resolutionoffsetmask) - icoords[0]) * (jcoords[1] - icoords[1]) /(jcoords[0] - icoords[0]);
          
          // Add all lons between top/bottom intersection as long as they contain the edge
          long startLng = topIntersection & resolutionprefixmask;
          long stopLng = bottomIntersection & resolutionprefixmask;
          
          boolean first = true;
          
          if (startLng > stopLng) {
            startLng = bottomIntersection & resolutionprefixmask;
            stopLng = topIntersection & resolutionprefixmask;
          }

          long lowLon = Long.MAX_VALUE;
          long highLon = Long.MIN_VALUE;
          
          for (long lng = startLng; lng <= stopLng; lng += (1L << (32 - resolution))) {
            if ((lng >= (icoords[1] & resolutionprefixmask) && lng <= (jcoords[1] | resolutionoffsetmask))
                || (lng >= (jcoords[1] & resolutionprefixmask) && lng <= (icoords[1] | resolutionoffsetmask))) {
              coverage.addCell(resolution, HHCodeHelper.buildHHCode(lat, lng));
              // Record low and high bounds of cell slice we just added.
              if ((lng&resolutionprefixmask) < lowLon) {
                lowLon = lng&resolutionprefixmask;
              }
              if ((lng&resolutionprefixmask) > highLon) {
                highLon = lng&resolutionprefixmask;
              }
            }
          }

          //
          // If the slice we just added is not adjacent to an already added slice,
          // add its low end to nodeLon.
          //
          
          /*
          if (!coverage.contains(resolution, HHCodeHelper.buildHHCode(lat, lowLon))
              && !coverage.contains(resolution, HHCodeHelper.buildHHCode(lat, lowLon - (1L << (32 - resolution))))
              && !coverage.contains(resolution, HHCodeHelper.buildHHCode(lat, highLon + (1L << (32 - resolution))))) {
            nodeLon.add(lowLon);
          }
          */
          
          boolean ok = true;
          
          for (long l = lowLon - (1L << (32 - resolution)); l <= highLon + (1L << (32 - resolution)); l += (1L << (32 - resolution))) {
            if (allLons.contains(l)) {
              ok = false;
              break;
            }
          }
          
          // Add all lons of the slice we just added
          for (long l = lowLon; l <= highLon; l += (1L << (32 - resolution))) {
            allLons.add(l);
          }

          if (ok) {
            nodeLon.add(lowLon);
          }
          //nodeLon.add(icoords[1] + (lat - icoords[0]) * (jcoords[1] - icoords[1]) /(jcoords[0] - icoords[0]));
        } else if(icoords[0] == jcoords[0] && lat == (icoords[0] & resolutionprefixmask)) {
          // Handle the case where the polygon edge is horizontal, we add the cells on the edge to the coverage
          for (long lon = Math.min(icoords[1],jcoords[1]); lon <= Math.max(icoords[1], jcoords[1]); lon += (1L << (32 - resolution))) {
            coverage.addCell(resolution, HHCodeHelper.buildHHCode(lat, lon));
          }
        }


        j = i;
      }
    
      // Sort nodeLon
      Collections.sort(nodeLon);

      // Add the zones between node pairs

      if (nodeLon.size() > 1) {
        for (int i = 0; i < nodeLon.size(); i += 2) {
          for (long lon = nodeLon.get(i); lon <= (nodeLon.get(i + 1) | resolutionoffsetmask); lon += (1L << (32 - resolution))) {
            // Add the cell
            coverage.addCell(resolution, HHCodeHelper.buildHHCode(lat, lon));
          }
        }
        
      }
    }

    // FIME(hbs): we could compute the bbox area and the coverage area, if it differs and the resolution was initially set to 0
    // i.e. we were asked to guess, then we could increase it and try again so as to have a better area ratio.
        
    return coverage;
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
   * @param from
   * @param to
   * @param coverage
   * @param resolution
   */
  
  public static final void coverLine(long from, long to, Coverage coverage, int resolution) {
    long[] A = splitHHCode(from);
    long[] B = splitHHCode(to);

    //
    // Swap A and B if not increasing lon
    //
    
    if (A[1] > B[1]) {
      long t = A[0]; A[0] = B[0]; B[0] = t;
      t = A[1]; A[1] = B[1]; B[1] = t;
    }
    
    //
    // Compute deltaLat and deltaLon
    //
    
    long dlat = Math.abs(B[0] - A[0]);
    long dlon = Math.abs(B[1] - A[1]);
    
    //
    // Determine if line is going north
    //
    
    long north = B[0] - A[0];    
    
    //
    // Handle the case of vertical and horizontal lines
    //

    long offset = 1L << (32 - resolution);
    long offsetmask = offset - 1;
    long prefixmask = 0xffffffffL ^ offsetmask;

    if (0 == north) {
      // Horizontal line
      long lat = A[0];
      long lon = A[1];
      
      while((lon & prefixmask) < B[1]) {
        coverage.addCell(resolution, buildHHCode(lat, lon));
        lon += offset;
      }
    } else if (0 == B[1] - A[1]) {
      // Vertical line
      
      long lat = A[0];
      long lon = A[1];
      
      if (north > 0) {
        while((lat & prefixmask) < B[0]) {
          coverage.addCell(resolution, buildHHCode(lat, lon));
          lat += offset;
        }        
      } else {
        while((lat | offsetmask) > B[0]) {
          coverage.addCell(resolution, buildHHCode(lat, lon));
          lat -= offset;
        }        
      }
    } else {
      long lat = A[0];
      long lon = A[1];

      long hhcode = buildHHCode(lat, lon);

      boolean cont = true;
      while (cont) {
        coverage.addCell(resolution, hhcode);

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
          cont = ((lat & prefixmask) < B[0]) && ((lon & prefixmask) < B[1]);
        } else {
          cont = ((lat | offsetmask) > B[0]) && ((lon & prefixmask) < B[1]);
        }
        
        long next = buildHHCode(lat, lon);

        // We have this safety net in case the slope is so slow that the latitude does not grow,
        // so if we stay in the same cell twice, we end the loop.
//        if (((lon & prefixmask) >= B[1]) && next == hhcode) {
//          cont = false;
//        }
        
        hhcode = next;
      }
    }
  }
  
  public static final Coverage coverPolyline(List<Long> nodes, int resolution, boolean useBresenham) {
    
    //
    // Retrieve boundingbox of nodes if resolution is 0 and compute optimal one
    //
    
    // FIXME(hbs): should we determine the resolution per edge instead?
    if (0 == resolution) {
      long[] bbox = getBoundingBox(nodes);

      // Extract log2 of the deltas and keep smallest
      // This log is the resolution we must use to have cells that are just a little smaller than 
      // the one we try to cover. We could use 'ceil' instead of 'floor' to have cells a little bigger.
               
      int log2 = (int) Math.floor(Math.min(Math.log(bbox[2] - bbox[0]), Math.log(bbox[3] - bbox[1]))/Math.log(2.0));
      
      // Make log an even number.
      log2 = log2 & 0x1e;

      resolution = 32 - log2;
      // Make the resolution a little finer so we don't cover the line too coarsely
      resolution += 4;

      // We just make sure that we do not exceed MAX_CELLS_PER_SIDE (which could be the case if a line is for example horizontal).
      long MAX_CELLS_PER_SIDE = 64;
      
      while(((bbox[2] - bbox[0]) >> (32 - resolution)) > MAX_CELLS_PER_SIDE || ((bbox[3] - bbox[1]) >> (32 - resolution)) > MAX_CELLS_PER_SIDE) {
        resolution -= 2;
      }
            
      // Limit resolution to 26 (0.59m at the equator!)
      if (resolution > 26) {
        resolution = 26;
      }
    }
    
    //
    // Initialize the coverage
    //
    
    Coverage coverage = new Coverage();
    
    if (useBresenham) {
      coverPolylineBresenham(nodes, resolution, coverage);
    } else {
      for (int i = 0; i <= nodes.size() - 2; i++) {
        coverLine(nodes.get(i), nodes.get(i+1), coverage, resolution);
      }
    }
    
    return coverage;
  }

  private static void coverPolylineBresenham(List<Long> nodes, int resolution, Coverage coverage) {
    
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
  
    for (int i = 0; i <= nodes.size() - 2; i++) {

      stableSplitHHCode(nodes.get(i), 32, from);
      stableSplitHHCode(nodes.get(i+1), 32, to);

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
    
      long prefixmask = 0xffffffffL ^ (offset - 1);
    
      while ((lon & prefixmask) <= to[1]) {
      
        if (steep) {
          coverage.addCell(resolution, buildHHCode(lon,lat,32));
        
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
          coverage.addCell(resolution, buildHHCode(lat,lon,32));

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
  
  public static final Coverage coverRectangle(final double swlat, final double swlon, final double nelat, final double nelon) {
    return coverRectangle(swlat, swlon, nelat, nelon, 0);
  }
  
  public static final Coverage coverRectangle(final double swlat, final double swlon, final double nelat, final double nelon, int resolution) {
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
  
  public static long[] center(long hhcode, int resolution) {
    long[] ll = splitHHCode(hhcode);
    long mask = ((1L << (32 - resolution)) - 1) >> 1;
    ll[0] |= mask;
    ll[1] |= mask;
    return ll;    
  }
  
  public static double toLat(long longLat) {
    return degreesPerLatUnit * longLat - 90.0;
  }
  public static double toLon(long longLon) {
    return degreesPerLonUnit * longLon - 180.0;
  }

  public static long toLongLat(double lat) {
    return (long) ((lat + 90.0) / degreesPerLatUnit);
  }
  
  public static long toLongLon(double lon) {
    return (long) ((lon + 180.0) / degreesPerLonUnit);
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

    bbox[2] = ll[0] * degreesPerLatUnit - 90.0;
    bbox[3] = ll[1] * degreesPerLonUnit - 180.0;

    // Now compute bottom/left limit of bbox (lower bits set to 0)
    ll[0] ^= offsetmask;
    ll[1] ^= offsetmask;
    
    bbox[0] = ll[0] * degreesPerLatUnit - 90.0;
    bbox[1] = ll[1] * degreesPerLonUnit - 180.0;
    
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
  public static Coverage coverSegment(long from, long to, double distance, int resolution) {
    
    //
    // Split 'to' and 'from'
    //
    
    long[] fromcoords = splitHHCode(from, MAX_RESOLUTION);
    long[] tocoords = splitHHCode(to, MAX_RESOLUTION);
    
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

    List<Long> vertices = new ArrayList<Long>(4);
    
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
    
    vertices.add(buildHHCode(A[0], A[1], MAX_RESOLUTION));
    vertices.add(buildHHCode(B[0], B[1], MAX_RESOLUTION));
    vertices.add(buildHHCode(C[0], C[1], MAX_RESOLUTION));
    vertices.add(buildHHCode(D[0], D[1], MAX_RESOLUTION));
    
    return coverPolygon(vertices, resolution);
  }
}
