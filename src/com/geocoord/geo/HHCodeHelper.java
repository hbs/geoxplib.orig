package com.geocoord.geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HHCodeHelper {
  
  private static final double degreesPerLatUnit = 180.0 / (1L << 32);
  private static final double degreesPerLonUnit = 360.0 / (1L << 32);
  
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
  private static final void internalSplitHHCode(final long hhcode, final int resolution, final long[] coords) {

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
        Set<Long> unique = new HashSet();
        
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
      HHCodeHelper.internalSplitHHCode(hhcode, 32, coords);

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
   * Determine a list of zones covering a polygon
   * 
   * @param vertices Vertices of the polygon (of hhcodes)
   * @param resolution The resolution at which to do the covering. If the resolution is 0, compute one from the bbox
   * 
   * @return A map keyed by resolution and whose The list of zones covering the polygon
   */
  public static final Map<Integer,List<Long>> coverPolygon(List<Long> vertices, int resolution) {
    
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
      HHCodeHelper.internalSplitHHCode(vertex, 32, coords);
      
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
    
    if (0 == resolution) {
      long deltaLat = topLat - bottomLat;
      long deltaLon = rightLon - leftLon;
      
      // Extract log2 of the deltas and keep smallest
      // This log is the resolution we must use to have cells that are just a little smaller than 
      // the one we try to cover. We could use 'ceil' instead of 'floor' to have cells a little bigger.
               
      resolution = (int) Math.floor(Math.min(Math.log(deltaLat), Math.log(deltaLon))/Math.log(2.0));
      // Make log an even number.
      resolution = resolution & 0xfe;
      resolution = 32 - resolution;
    }

    long resolutionprefixmask = 0xffffffffL ^ ((1L << (32 - resolution)) - 1);
    long resolutionoffsetmask = (1L << (32 - resolution)) - 1;
    
    Map<Integer,List<Long>> coverage = new HashMap<Integer, List<Long>>(32);
    coverage.put(resolution, new ArrayList<Long>());

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
    
    // Add top/bottom of each cell between bottomLat and topLat so we are sure to catch the intersections
    
    for (long lat = bottomLat; lat < topLat; lat += 1L << (32 - resolution)) {
      verticesLat.add(lat & resolutionprefixmask);
      verticesLat.add(lat | resolutionoffsetmask);
    }
    
    nodeLat.addAll(verticesLat);
    Collections.sort(nodeLat);

    for (long lat: nodeLat) {
      //
      // Scan the vertices
      //

      // Close the path by referencing the last vertex
      int j = vertices.size() - 1;
      
      nodeLon.clear();
      
      for (int i = 0; i < vertices.size(); i++) {
        HHCodeHelper.internalSplitHHCode(vertices.get(i), 32, icoords);
        HHCodeHelper.internalSplitHHCode(vertices.get(j), 32, jcoords);
        
        if (icoords[0] > lat && jcoords[0] <= lat || jcoords[0] > lat && icoords[0] <= lat){
          nodeLon.add(icoords[1] + (lat - icoords[0]) * (jcoords[1] - icoords[1]) /(jcoords[0] - icoords[0]));
        } else if(icoords[0] == jcoords[0] && lat == icoords[0]) {
          // Handle the case where the polygon edge is horizontal, we add the cells on the edge to the coverage
          for (long lon = Math.min(icoords[1],jcoords[1]); lon <= Math.max(icoords[1], jcoords[1]); lon += (1L << (32 - resolution))) {
            coverage.get(resolution).add(HHCodeHelper.buildHHCode(lat, lon));
          }
        }
        j = i;
      }
    
      // Sort nodeLon
      Collections.sort(nodeLon);

      // Add the zones between node pairs
      
      for (int i = 0; i < nodeLon.size(); i += 2) {
        for (long lon = nodeLon.get(i); lon <= (nodeLon.get(i + 1) | resolutionoffsetmask); lon += (1L << (32 - resolution))) {
          // Add the cell
          coverage.get(resolution).add(HHCodeHelper.buildHHCode(lat, lon));
        }
      }
    }

    // FIME(hbs): we could compute the bbox area and the coverage area, if it differs and the resolution was initially set to 0
    // i.e. we were asked to guess, then we could increase it and try again so as to have a better area ratio.
    
    return coverage;
  }
    
  public static final Map<Integer,List<Long>> coverPolyline(List<Long> nodes, int resolution) {
    
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
      
      // Limit resolution to 26 (0.59m at the equator!)
      if (resolution > 26) {
        resolution = 26;
      }
    }
    
    //
    // Initialize the coverage
    //
    
    Map<Integer,List<Long>> coverage = new HashMap<Integer, List<Long>>();       
    coverage.put(resolution, new ArrayList<Long>());
    
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

      internalSplitHHCode(nodes.get(i), 32, from);
      internalSplitHHCode(nodes.get(i+1), 32, to);

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
      
      long error = deltalon >> 1;
    
      long lat = from[0];
      
      long latstep = (from[0] < to[0]) ? offset : -offset;
      
      long lon = from[1];
      
      long prefixmask = 0xffffffffL ^ (offset - 1);
      
      while ((lon & prefixmask) <= to[1]) {
        
        if (steep) {          
          coverage.get(resolution).add(buildHHCode(lon,lat,32));
        } else {
          coverage.get(resolution).add(buildHHCode(lat,lon,32));
        }
        
        error = error - deltalat;
        
        if (error < 0) {
          lat = lat + latstep;
          error = error + deltalon;
        }
        
        lon += offset;
      }
    }

    return coverage;
  }
  
  private static final void mergeCoverages(Map<Integer,List<Long>> a, Map<Integer,List<Long>> b) {
    for (int resolution: b.keySet()) {
      if (!a.containsKey(resolution)) {
        a.put(resolution, new ArrayList<Long>());
      }
      a.get(resolution).addAll(b.get(resolution));
    }
  }
  
  public static final String getCoverageString(Map<Integer,List<Long>> coverage) {
    
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
  public static final Map<Integer,List<Long>> coverRectangle(final double swlat, final double swlon, final double nelat, final double nelon) {
    //
    // Take care of the case when the bbox contains the international date line
    //
        
    if (swlon > nelon) {
      // If sign differ, consider we crossed the IDL
      if (swlon * nelon <= 0) {
        
        Map<Integer,List<Long>> a = coverPolygon(new ArrayList<Long>() {{
          add(getHHCodeValue(swlat, swlon));
          add(getHHCodeValue(nelat, swlon));
          add(getHHCodeValue(nelat,180.0));
          add(getHHCodeValue(swlat,180.0));
        }}, 0);
        
        Map<Integer,List<Long>> b = coverPolygon(new ArrayList<Long>() {{
          add(getHHCodeValue(swlat, nelon));
          add(getHHCodeValue(nelat, nelon));
          add(getHHCodeValue(nelat,-180.0));
          add(getHHCodeValue(swlat,-180.0));
        }}, 0);

        mergeCoverages(a,b);
        
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
        }}, 0);
      }
    }
    
    return coverPolygon(new ArrayList<Long>() {{
      add(getHHCodeValue(swlat,swlon));
      add(getHHCodeValue(swlat,nelon));
      add(getHHCodeValue(nelat,nelon));
      add(getHHCodeValue(nelat,swlon));
    }}, 0);    
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
}
