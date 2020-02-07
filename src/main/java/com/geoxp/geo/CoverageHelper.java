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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class CoverageHelper {
  private static final int MIN_LOD = 256;
  private static final int MAX_LOD = -1;

  public static Coverage fromGeoCells(long[] geocells) {
    return fromGeoCells(geocells, true);
  }
  
  public static Coverage fromGeoCells(long[] geocells, boolean optimize) {
    Coverage c = new Coverage();
    
    for (long geocell: geocells) {
      int resolution = ((int) (((geocell & 0xf000000000000000L) >> 60) & 0xf)) << 1;
      long hhcode = geocell << 4;
      c.addCell(resolution, hhcode);
    }
  
    if (optimize) {
      c.optimize(0L);
    }
    return c;
  }
  
  public static String toKML(Coverage coverage) throws IOException {
    StringWriter sw = new StringWriter();
    toKML(coverage, sw, true);
    return sw.toString();
  }
  
  public static void toKML(Coverage coverage, Writer writer, boolean outline) throws IOException {
    
    //
    // Extract cells to render
    //
    
    // FIXME(hbs): obfuscate cells so the use of HHCodes is not obvious
    //String[] cells = coverage.toString("#").split("#");

    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    writer.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
    writer.append("<Document>\n");
    writer.append("  <name>GeoXP Coverage</name>\n");
    writer.append("  <ScreenOverlay>\n");
    writer.append("    <overlayXY x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
    writer.append("    <screenXY x=\"20\" y=\"50\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
    writer.append("    <size>-1</size>\n");
    writer.append("    <Icon>\n");
    // FIXME(hbs): replace image URL
    writer.append("      <href>http://farm5.static.flickr.com/4056/4477523262_bfd831c564_o.png</href>\n");
    writer.append("    </Icon>\n");
    writer.append("  </ScreenOverlay>\n");

    boolean showpins = false;
    
    for (int res: coverage.getResolutions()) {
      for (long cell: coverage.getCells(res)) {
        double[] bbox = HHCodeHelper.getHHCodeBBox(cell, res);
        
        writer.append("  <Placemark>\n");
        writer.append("  <Style>\n");
        writer.append("    <LineStyle>\n");
        writer.append("      <color>c0008000</color>\n");        
        writer.append("      <width>1</width>\n");
        writer.append("    </LineStyle>\n");
        writer.append("    <PolyStyle>\n");
        writer.append("      <color>c0f0f0f0</color>\n");
        writer.append("      <fill>1</fill>\n");
        if (outline) {
          writer.append("      <outline>1</outline>\n");
        } else {
          writer.append("      <outline>0</outline>\n");
        }
        writer.append("    </PolyStyle>\n");    
        writer.append("  </Style>\n");        
        writer.append("    <name>");
        writer.append(HHCodeHelper.toString(cell, res));
        writer.append("</name>\n");
        writer.append("    <MultiGeometry>\n");

        writer.append("      <tessellate>1</tessellate>\n");
        writer.append("      <Polygon><outerBoundaryIs><LinearRing>\n");
        writer.append("        <coordinates>\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[1]));
        writer.append(",");
        writer.append(Double.toString(bbox[0]));
        writer.append(",0\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[1]));
        writer.append(",");
        writer.append(Double.toString(bbox[2]));
        writer.append(",0\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[3]));
        writer.append(",");
        writer.append(Double.toString(bbox[2]));
        writer.append(",0\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[3]));
        writer.append(",");
        writer.append(Double.toString(bbox[0]));
        writer.append(",0\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[1]));
        writer.append(",");
        writer.append(Double.toString(bbox[0]));
        writer.append(",0\n");
        
        writer.append("        </coordinates>\n");      
        writer.append("      </LinearRing></outerBoundaryIs></Polygon>\n");   
        
        if (showpins) {
          writer.append("      <Point>\n");
          writer.append("        <coordinates>\n");
          writer.append(Double.toString((bbox[3]+bbox[1])/2.0));
          writer.append(",");
          writer.append(Double.toString((bbox[2]+bbox[0])/2.0));
          writer.append(",0");
          writer.append("        </coordinates>\n");      
          writer.append("      </Point>\n");
        }
        writer.append("    </MultiGeometry>\n");
        writer.append("  </Placemark>\n");
      }
    }

    writer.append("</Document>\n");
    writer.append("</kml>\n");    
  }
  
  /**
   * Return a list of coordinates for polygons extracted from a coverage.
   * NaN indicates a switch from one polygon to the other, including as the first element of the array
   * 
   * There is no notion of inner/outer polygons, the polygons may intersect, an even number
   * of polygons intersecting means that the intersection is a hole, an odd number that it is
   * an outer polygon
   * 
   * If you need to determine outer and inner polygons, use clusters first and then toEnvelope
   * on each cluster, the first polygon will be an outer polygon, the others inner ones
   */
  public static float[] toEnvelope(Coverage c) {
    return toEnvelope(c.toGeoCells(30));
  }
    
  public static float[] toEnvelope(long[] cells) {
    Map<Long,Set<Long>> segmentsByX = getSegments(cells, false);
    Map<Long,Set<Long>> segmentsByY = getSegments(cells, true);

    //
    // Index all segments by their end points
    //
    
    Map<Long,List<Long>> sortedSegments = new HashMap<Long, List<Long>>();

    int nsegments = 0;
    
    // The first set of segments is indexed by lon
    for (long x: segmentsByX.keySet()) {
      x = x & 0xFFFFFFFFL;
      
      Set<Long> ycoords = segmentsByX.get(x);
      
      for (long coords: ycoords) {
        long start = coords >>> 32;
        long end = coords & 0xFFFFFFFFL;
        
        long from = (start << 32) | x;

        List<Long> ends = sortedSegments.get(from);
      
        if (null == ends) {
          ends = new ArrayList<Long>();
          sortedSegments.put(from, ends);
        }

        long to = (end << 32) | x;
            
        ends.add(to);
        
        ends = sortedSegments.get(to);
        
        if (null == ends) {
          ends = new ArrayList<Long>();
          sortedSegments.put(to, ends);
        }
        
        ends.add(from);
        
        nsegments++;
      }
    }

    // Second set is indexed by lat
    for (long y: segmentsByY.keySet()) {
      Set<Long> xcoords = segmentsByY.get(y);
      y = y << 32;
      for (long coords: xcoords) {
        long start = coords >>> 32;
        long end = coords & 0xFFFFFFFFL;
        
        long from = y | start;

        List<Long> ends = sortedSegments.get(from);
        
        if (null == ends) {
          ends = new ArrayList<Long>();
          sortedSegments.put(from, ends);
        }

        long to = y | end;

        ends.add(to);
        
        ends = sortedSegments.get(to);

        if (null == ends) {
          ends = new ArrayList<Long>();
          sortedSegments.put(to, ends);
        }
        
        ends.add(from);

        nsegments++;
      }
    }

    //
    // Now we have segments sorted by origins, attempt to build as many polygons as possible
    // The 'polygons' array contains lat,lon coordinates (encoded as a Long <LAT><LON>) of all polygons.
    // A null indicates the beginning of a new polygon.
    //
    
    List<Long> polygons = new ArrayList<Long>();
    
    Long lastcoords = null;
    Long lastPolygonOrigin = null;
    
    int npoly = 0;
    
    while (!sortedSegments.isEmpty()) {
      //
      // If we don't have a 'lastcoords' value, extract a segment at random
      //
      
      if (null == lastcoords) {
        polygons.add(null);
        npoly++;        
        lastcoords = sortedSegments.keySet().iterator().next();
        polygons.add(lastcoords);
        lastPolygonOrigin = lastcoords;
      }
      
      //
      // Extract one segment end (check lastcoords and lastcoords shifted by 1 in x/y
      //
      
      if (!sortedSegments.containsKey(lastcoords)) {
        lastcoords = null;
        continue;        
      }
      
      long end = sortedSegments.get(lastcoords).remove(sortedSegments.get(lastcoords).size() - 1);
      
      sortedSegments.get(end).remove(lastcoords);
      if (sortedSegments.get(end).isEmpty()) {
        sortedSegments.remove(end);
      }
      
      // Clean sortedSegments if we removed the last element at 'lastcoords'
      if (sortedSegments.get(lastcoords).isEmpty()) {
        sortedSegments.remove(lastcoords);
      }
      
      polygons.add(end);
      
      //
      // If we reached the origin of the polygon, change polygon
      //
      
      if (end == lastPolygonOrigin) {
        lastcoords = null;
      } else {
        lastcoords = end;
      }
    }
    
    //
    // Now convert the Long list into an array of floats
    //
    
    float[] coords = new float[polygons.size() * 2 - npoly];
    
    int idx = 0;
    
    for (Long l: polygons) {
      if (null == l) {
        coords[idx++] = Float.NaN;
      } else {
        float lat = (float) HHCodeHelper.toLat(l >>> 32);
        float lon = (float) HHCodeHelper.toLon(l & 0xFFFFFFFFL);
        coords[idx++] = lat;
        coords[idx++] = lon;
      }
    }
    
    return coords;
  }
  
  private static Map<Long,Set<Long>> getSegments(long[] cells, boolean xyswap) {        
    //
    // For each lower/upper X coordinate, store the lower/upper Y coordinates of the cell (as a 64 bit long using upper/lower 32 bits)
    //
    
    Comparator<Long> YBOUNDS_COMPARATOR = new Comparator<Long>() {
      public int compare(Long o1, Long o2) {
        long l1 = o1 >>> 32;
        long l2 = o2 >>> 32;
      
        if (l1 < l2) {
          return -1;
        } else if (l1 > l2) {
          return 1;
        } else {
          l1 = o1 & 0xFFFFFFFFL;
          l2 = o2 & 0xFFFFFFFFL;
          
          // The longest segment appears first
          if (l1 > l2) {
            return -1;
          } else if (l1 < l2) {
            return 1;
          } else {
            return 0;
          }
        }
      }
    };
    
    Map<Long,PriorityQueue<Long>> cellsX = new HashMap<Long, PriorityQueue<Long>>();
      
    for (long cell: cells) {
      // Extract resolution
      int res = (int) ((cell >>> 60) &0xFL);
      // Extract hhcode and lat/long
      long hhcode = cell << 4;
      long[] latlon = HHCodeHelper.splitHHCode(hhcode, res * 2);
      
      if (xyswap) {
        long tmp = latlon[0];
        latlon[0] = latlon[1];
        latlon[1] = tmp;
      }
      
      res = res - 1;
      
      long lowerX = latlon[1];
      long upperX = latlon[1] + Coverage.CELL_SIZE_BY_RES[res]; // We don't substract 1 as we want an overlap with the following cell
      
      // Adjust the max limit
      if (0x100000000L == upperX) {
        upperX--;
      }
      
      // Encode bounds as <LOWER><UPPER>
      // We don't subtract 1 as we want overlap with the next celle
      long up = latlon[0] + Coverage.CELL_SIZE_BY_RES[res];
      if (0x100000000L == up) {
        up--;
      }
      long Ybounds = (latlon[0] << 32) | up;
      
      PriorityQueue<Long> queue = cellsX.get(lowerX);
      
      if (null == queue) {
        queue = new PriorityQueue<Long>(YBOUNDS_COMPARATOR);
        cellsX.put(lowerX, queue);
      }
      
      queue.add(Ybounds);
      
      queue = cellsX.get(upperX);
      
      if (null == queue) {
        queue = new PriorityQueue<Long>(YBOUNDS_COMPARATOR);
        cellsX.put(upperX, queue);
      }
      
      queue.add(Ybounds);      
    }
    
    //
    // Now for each X coordinate, inspect the Y coordinates and retain only the segments which appear a single time
    //
    
    Map<Long,Set<Long>> segments = new HashMap<Long, Set<Long>>();
    
    for (long x: cellsX.keySet()) {
      PriorityQueue<Long> allSegments = cellsX.get(x);
      
      // Final segments
      PriorityQueue<Long> xSegments = new PriorityQueue<Long>(YBOUNDS_COMPARATOR);
      
      Long lowerY = null;
      Long upperY = null;

      while(!allSegments.isEmpty()) {
        long segment = allSegments.poll();
        long segStart = segment >>> 32;
        long segEnd = segment & 0xFFFFFFFFL;
        
        if (null == lowerY && null == upperY) {
          lowerY = segStart;
          upperY = segEnd;
          continue;
        }
        
        //
        // If the current segment is identical to the previous one, reset the current segment
        //
        
        if (segStart == lowerY && segEnd == upperY) {
          lowerY = null;
          upperY = null;
          continue;
        }
        
        //
        // If the current segment is contained in [lowerY,upperY], split [lowerY,upperY] by throwing away the intersection
        // as we won't retain it. Make sure the bounds overlap
        //
        
        if (segStart >= lowerY && segEnd <= upperY) {
          if (segStart > lowerY) {
            allSegments.add(lowerY << 32 | segStart);
          }
          
          if (segEnd < upperY) {
            allSegments.add(segEnd << 32 | upperY);
          }
          
          lowerY = null;
          upperY = null;
          continue;
        }
        
        //
        // If the current segment intersects [lowerY,upperY], cut it at upperY
        //
        
        if (segStart < upperY && segEnd > upperY) {
          if (segStart > lowerY) {
            allSegments.add(lowerY << 32 | segStart);
          }
          allSegments.add(upperY << 32 | segEnd);
          
          lowerY = null;
          upperY = null;          
          continue;
        }
        
        //
        // If the current segment is adjacent or passed upperY, emit [lowerY,upperY] and
        // change lowerY,upperY
        //
        
        if (segStart >= upperY) {
          xSegments.add((lowerY << 32) | upperY);
          lowerY = segStart;
          upperY = segEnd;
          continue;
        }
      }
      
      if (null != lowerY && null != upperY) {
        xSegments.add((lowerY << 32) | upperY);
      }
      
      //
      // Now merge adjacent segments
      //
      
      Long current = null;
      
      Set<Long> merged = new HashSet<Long>();
      
      while(!xSegments.isEmpty()) {
        if (null == current) {
          current = xSegments.poll();
          continue;
        }
                
        long next = xSegments.poll();

        //
        // If next starts where current ends, merge them.
        // Otherwise, add 'current' and set it to 'next'
        //
        
        if ((current & 0xFFFFFFFFL) == next >>> 32) {
          current = (current & 0xFFFFFFFF00000000L) | (next & 0xFFFFFFFFL);
        } else {
          merged.add(current);
          current = next;
        }                
      }
      
      if (null != current) {
        merged.add(current);
      }
        

      if (!merged.isEmpty()) {
        segments.put(x, merged);
      }
    }
        
    return segments;
  }
  
  public static void kmlEnvelope(Writer writer, long[] cells) throws IOException {
    
    float[] segments = toEnvelope(cells);
    
    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    writer.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
    writer.append("<Document>\n");
    writer.append("  <name>GeoXP Coverage</name>\n");
    writer.append("  <ScreenOverlay>\n");
    writer.append("    <overlayXY x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
    writer.append("    <screenXY x=\"20\" y=\"50\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
    writer.append("    <size>-1</size>\n");
    writer.append("    <Icon>\n");
    // FIXME(hbs): replace image URL
    writer.append("      <href>http://farm5.static.flickr.com/4056/4477523262_bfd831c564_o.png</href>\n");
    writer.append("    </Icon>\n");
    writer.append("  </ScreenOverlay>\n");

    writer.append("  <Placemark>\n");
    writer.append("  <Style>\n");
    // FILLSTYPE
    writer.append("    <LineStyle>\n");
    writer.append("      <color>ffffffff</color>\n");        
    writer.append("      <width>1</width>\n");
    writer.append("    </LineStyle>\n");
    writer.append("    <PolyStyle>\n");
    writer.append("      <color>c0f0f0f0</color>\n");
    writer.append("      <fill>1</fill>\n");
    writer.append("      <outline>1</outline>\n");
    writer.append("    </PolyStyle>\n");    
    writer.append("  </Style>\n");        
    writer.append("    <MultiGeometry>\n");

    writer.append("      <tessellate>1</tessellate>\n");

    int i = 0;
    boolean inpoly = false;
    boolean first = true;
    
    while (i < segments.length) {
      // NaN indicates a new polygon
      if (Float.isNaN(segments[i])) {
        if (inpoly) {
          writer.append("\n");
          writer.append("        </coordinates>\n");      
          writer.append("      </LinearRing></outerBoundaryIs></Polygon>\n");   
          inpoly = false;
        }
        writer.append("      <Polygon><outerBoundaryIs><LinearRing>\n");
        writer.append("        <coordinates>\n");
        inpoly = true;
        first = true;
        i++;
        continue;
      }


      if (!first) {
        writer.append(",");
      } else {
        writer.append("          ");        
      }

      first = false;

      writer.append(Float.toString(segments[i+1]));
      writer.append(",");
      writer.append(Float.toString(segments[i]));
      writer.append(",0");    
      
      i += 2;
    }

    if (inpoly) {
      writer.append("\n");
      writer.append("        </coordinates>\n");      
      writer.append("      </LinearRing></outerBoundaryIs></Polygon>\n");   
      inpoly = false;
    }

    writer.append("    </MultiGeometry>\n");
    writer.append("  </Placemark>\n");    
    writer.append("</Document>\n");
    writer.append("</kml>\n");    
  }

  /**
   * Extract clusters of cells from a Coverage
   * @param coverage
   * @return
   */
  public static List<Coverage> clusters(Coverage coverage) {
    
    long[] cells = coverage.toGeoCells(HHCodeHelper.MAX_RESOLUTION);
    
    // Sort the cell array
    Arrays.sort(cells);
    
    // Cluster id for each element
    int[] clusterid = new int[cells.length];
    
    List<Long> cluster = new ArrayList<Long>();
    List<Long> window = new ArrayList<Long>();
    
    // Array for the neighbors, up to 5 (including the cell itself) per resolution
    long[] neighbors = new long[150];
    
    //
    // While we have not scanned all cells
    //
        
    int clustered = 0;
    int currentcluster = 1;
    
    while(clustered != cells.length) {
      
      //
      // Clear the scanning window and the current cluster
      //
      window.clear();
      cluster.clear();

      //
      // Add the first unclustered cell
      //

      int next = -1;
      for (int i = 0; i < clusterid.length; i++) {
        if (0 == clusterid[i]) {
          next = i;
          break;
        }
      }
      
      window.add(cells[next]);
      clusterid[next] = currentcluster;
      clustered++;
      
      //
      // Iterate over the window to build a cluster
      //
      
      while(!window.isEmpty()) {
        //
        // Remove the first cell from the window
        //
        
        long cell = window.remove(0);
        int resCell = (int) ((cell >>> 60) & 0xFL);
        
        //
        // Add it to the current cluster as it is either the first one
        // or it was added to the window because it touches another cell of
        // the cluster
        //
        
        //
        // Now scan all the non connected cells and find all the 'touching' neighbors (N/S/E/W) for
        // 'cell'
        //

        //
        // Determine the neighbors of 'cell' at all the resolutions
        //
        
        // First store the neighbors N/S/E/W at the resolution of 'cell'
        // We also store the cell itself
        long hhcode = cell << 4;
        neighbors[0] = cell;
        // We add the resolution from cell
        neighbors[0] = (cell & 0xF000000000000000L) | (HHCodeHelper.northHHCode(hhcode, resCell << 1) >>> 4);
        neighbors[1] = (cell & 0xF000000000000000L) | (HHCodeHelper.eastHHCode(hhcode, resCell << 1) >>> 4);
        neighbors[2] = (cell & 0xF000000000000000L) | (HHCodeHelper.southHHCode(hhcode, resCell << 1) >>> 4);
        neighbors[3] = (cell & 0xF000000000000000L) | (HHCodeHelper.westHHCode(hhcode, resCell << 1) >>> 4);
        
        //
        // If one of the neighbors wraps around north,east,south or west, clear it
        //
        
        // If north neighbor is in top level cell 0,1,4 or 5 and cell in a, b, e or f, ignore it
        
        long topcell = (neighbors[0] & 0x0F00000000000000L) >>> 56;
            
        if ((topcell == 0L || topcell == 1L || topcell == 4L || topcell == 5L)
            && ((neighbors[0] & 0x0F00000000000000L) != (cell & 0x0F00000000000000L))) {
          neighbors[0] = 0L;
        }
        topcell = (neighbors[1] & 0x0F00000000000000L) >>> 56; 
        if ((topcell == 0xAL || topcell == 8L || topcell == 2L || topcell == 0L)
            && ((neighbors[1] & 0x0F00000000000000L) != (cell & 0x0F00000000000000L))) {
          neighbors[1] = 0L;
        }
        topcell = (neighbors[2] & 0x0F00000000000000L) >>> 56; 
        if ((topcell == 0xAL || topcell == 0xBL || topcell == 0xEL || topcell == 0xFL)
            && ((neighbors[2] & 0x0F00000000000000L) != (cell & 0x0F00000000000000L))) {
          neighbors[2] = 0L;
        }
        topcell = (neighbors[3] & 0x0F00000000000000L) >>> 56; 
        if ((topcell == 0xFL || topcell == 0xDL || topcell == 7L || topcell == 5L)
            && ((neighbors[3] & 0x0F00000000000000L) != (cell & 0x0F00000000000000L))) {
          neighbors[3] = 0L;
        }

        for (int i = resCell - 1; i >= 1; i--) {
          long res = (((long) i) << 60) & 0xF000000000000000L;

          // The actual HHCode bits of a cell occupy 4 * resCell bits with the MSB being bit 59
          int shift = 60 - 4 * i;
          int offset = i * 4;
          neighbors[offset] = (((neighbors[0] >>> shift) << shift) & 0x0FFFFFFFFFFFFFFFL) | res;
          neighbors[offset + 1] = (((neighbors[1] >>> shift) << shift) & 0x0FFFFFFFFFFFFFFFL) | res;
          neighbors[offset + 2] = (((neighbors[2] >>> shift) << shift) & 0x0FFFFFFFFFFFFFFFL) | res;
          neighbors[offset + 3] = (((neighbors[3] >>> shift) << shift) & 0x0FFFFFFFFFFFFFFFL) | res;
          //neighbors[offset] = (((neighbors[0] >>> shift) << shift) & 0x0FFFFFFFFFFFFFFFL) | res;          
        }
        
        int last = 4 + (resCell - 1) * 4;
    
        //
        // Now check if we can find some neighbors
        //
        
        // We start at the end so we start with the finest resolution
        for (int i = last - 1; i >= 0; i--) {
          long neighbor = neighbors[i];
          int index = Arrays.binarySearch(cells, neighbor);
          
          if (index >= 0) {
            // If the cell is already a member of a cluster with a different id, change the id of that cluster
            if (0 != clusterid[index] && clusterid[index] != currentcluster) {
              int id = clusterid[index];
              for (int j = 0; j < clusterid.length; j++) {
                if (clusterid[j] == id) {
                  clusterid[j] = currentcluster;
                }
              }
            } else if (0 == clusterid[index]) {
              clusterid[index] = currentcluster;
              clustered++;
              window.add(neighbor);
            }
          
          }
        }       
      }

      currentcluster++;
    }
    
    List<Coverage> clusters = new ArrayList<Coverage>(currentcluster - 1);
    
    for (int i = 1; i < currentcluster; i++) {
      Coverage cov = new Coverage();
      for (int j = 0; j < clusterid.length; j++) {
        if (i == clusterid[j]) {
          cov.addCell(cells[j]);
        }
      }
      if (cov.getCellCount() > 0) {
        clusters.add(cov);
      }
    }

    return clusters;
  }
  
  public static String toGeoJSON(Coverage c) {
    // Ensure there are no duplicate cells
    c.dedup();
    // Optimize the coverage so we do not have too many cells to scan
    c.optimize(0L);
    // Extract clusters
    List<Coverage> clusters = clusters(c);
    
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"type\":\"MultiPolygon\",");
    sb.append("\"coordinates\":[");
    
    // Compute envelope of each cluster
    boolean first = true;
    for(Coverage cluster: clusters) {
      
      if (!first) {
        sb.append(",");
      }
      
      sb.append("[");

      float[] segments = toEnvelope(cluster);
      
      //
      // We must now determine the list of polygons, draw the first one
      // in clockwise order, the others in counterclockwise order since
      // for each cluster the first polygon is the envelope and the others
      // the holes.
      //
      
      List<int[]> polygons = new ArrayList<int[]>();
      
      int[] fromto = null;
      
      int idx = 0;
      
      while(idx < segments.length) {
        if (Float.isNaN(segments[idx])) {
          if (null != fromto) {
            fromto[1] = idx - 2; // Index of the last latitude of the polygon
            polygons.add(fromto);            
          }
          fromto = new int[2];
          fromto[0] = idx + 1;
        }
        idx++;
      }
      
      fromto[1] = segments.length - 2;
      if (fromto[0] != fromto[1]) {
        polygons.add(fromto);
      }

      //
      // Now for each polygon, determine if it is clockwise or counter clockwise
      // and swap the indices so every polygon is clockwise
      //
      // @see https://stackoverflow.com/questions/1165647/how-to-determine-if-a-list-of-polygon-points-are-in-clockwise-order
      //
      
      for (int[] polygon: polygons) {
        double sum = 0.0D;
        
        for (int i = polygon[0]; i < polygon[1] - 2; i += 2) {
          float delta = (segments[i + 3] - segments[i + 1])/(segments[i + 2] + segments[i]);
          if (Float.isFinite(delta)) {
            sum += delta;
          }
        }
        
        //
        // If sum is > 0, then the polygon is clockwise and the direction should be reversed
        // by swapping the indices
        //
        
        if (sum > 0.0) {
          int tmp = polygon[0];
          polygon[0] = polygon[1];
          polygon[1] = tmp;
        }
      }

      //
      // Iterate over the polygons, adding the first one counter-clockwise and the
      // following clockwise to stick with GeoJSON spec
      //
      
      for (int i = 0; i < polygons.size(); i++) {
        int[] polygon = polygons.get(i);
        
        if (i > 0) {
          sb.append(",");
        }
        
        sb.append("[");
        
        int offset = 2;
        
        // If not the first polygon, invert the indices
        if (0 != i) {
          int tmp = polygon[0];
          polygon[0] = polygon[1];
          polygon[1] = tmp;
        }

        if (polygon[0] > polygon[1]) {
          offset = -2;
        }

        int j = polygon[0];
        
        while (true) {
          if (j != polygon[0]) {
            sb.append(",");
          }
          sb.append("[");
          sb.append(segments[j + 1]);
          sb.append(",");
          sb.append(segments[j]);
          sb.append("]");
          
          if (j == polygon[1]) {
            break;
          }
          j += offset;
        }
        
        sb.append("]");
      }

      sb.append(" ]");
      
      first = false;
    }
    
    sb.append("]");
    sb.append("}");
    
    return sb.toString();
  }
}
