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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class CoverageHelper {
  private static final int MIN_LOD = 256;
  private static final int MAX_LOD = -1;

  public static Coverage fromGeoCells(long[] geocells) {
    Coverage c = new Coverage();
    
    for (long geocell: geocells) {
      int resolution = ((int) (((geocell & 0xf000000000000000L) >> 60) & 0xf)) << 1;
      long hhcode = geocell << 4;
      c.addCell(resolution, hhcode);
    }
    
    c.optimize(0L);
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
  
  public static float[] toEnvelope(Coverage c) {
    return toEnvelope(c.toGeoCells(30));
  }
    
  public static float[] toEnvelope(long[] cells) {
    Map<Long,Set<Long>> segmentsByX = getSegments(cells, false);
    Map<Long,Set<Long>> segmentsByY = getSegments(cells, true);
    
    int count = 0;
    
    for (long x: segmentsByX.keySet()) {
      count = count + segmentsByX.get(x).size() * 4;
    }

    for (long y: segmentsByY.keySet()) {
      count = count + segmentsByY.get(y).size() * 4;
    }

    float[] segments = new float[count];
    
    int idx = 0;
    
    for (long x: segmentsByX.keySet()) {
      Set<Long> ycoords = segmentsByX.get(x);
      for (long coords: ycoords) {
        long start = coords >>> 32;
        long end = (coords & 0xFFFFFFFFL) + 1;
        if (0x100000000L == end) {
          end--;
        }
        double lon = HHCodeHelper.toLon(x);
        double lat = HHCodeHelper.toLat(start);
        segments[idx++] = (float) lat;
        segments[idx++] = (float) lon;
        lat = HHCodeHelper.toLat(end);
        segments[idx++] = (float) lat;
        segments[idx++] = (float) lon;
      }
    }
        
    for (long y: segmentsByY.keySet()) {
      Set<Long> xcoords = segmentsByY.get(y);
      for (long coords: xcoords) {
        long start = coords >>> 32;
        long end = (coords & 0xFFFFFFFFL) + 1;
        if (0x100000000L == end) {
          end--;
        }
        double lat = HHCodeHelper.toLat(y);
        double lon = HHCodeHelper.toLon(start);
        segments[idx++] = (float) lat;
        segments[idx++] = (float) lon;
        lon = HHCodeHelper.toLon(end);
        segments[idx++] = (float) lat;
        segments[idx++] = (float) lon;
      }
    }
    
    return segments;
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
      
      if (0x100000000L == upperX) {
        upperX--;
      }
      
      // Encode bounds as <LOWER><UPPER>
      long Ybounds = (latlon[0] << 32) | (latlon[0] + Coverage.CELL_SIZE_BY_RES[res] - 1);
      
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
        // as we won't retain it
        //
        
        if (segStart >= lowerY && segEnd <= upperY) {
          if (segStart > lowerY) {
            allSegments.add(lowerY << 32 | (segStart - 1));
          }
          
          if (segEnd < upperY) {
            allSegments.add((segEnd + 1) << 32 | upperY);
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
            allSegments.add(lowerY << 32 | (segStart - 1));
          }
          allSegments.add((upperY + 1) << 32 | segEnd);
          
          lowerY = null;
          upperY = null;          
          continue;
        }
        
        //
        // If the current segment is passed upperY, emit [lowerY,upperY] and
        // change lowerY,upperY
        //
        
        if (segStart > upperY) {
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
        
        if ((current & 0xFFFFFFFFL) == (next >>> 32) - 1) {
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
    writer.append("    <LineStyle>\n");
    writer.append("      <color>ffffffff</color>\n");        
    writer.append("      <width>4</width>\n");
    writer.append("    </LineStyle>\n");
    writer.append("  </Style>\n");        
    writer.append("    <MultiGeometry>\n");

    writer.append("      <tessellate>1</tessellate>\n");

    for (int i = 0; i < segments.length; i += 4) {
      writer.append("      <LineString>\n");
      writer.append("        <coordinates>\n");
      writer.append("          ");
      writer.append(Float.toString(segments[i + 1]));
      writer.append(",");
      writer.append(Float.toString(segments[i]));
      writer.append(",10\n");
      writer.append("          ");
      writer.append(Float.toString(segments[i + 3]));
      writer.append(",");
      writer.append(Float.toString(segments[i + 2]));
      writer.append(",10\n");      
      writer.append("        </coordinates>\n");      
      writer.append("      </LineString>\n");         
    }

    writer.append("    </MultiGeometry>\n");
    writer.append("  </Placemark>\n");    
    writer.append("</Document>\n");
    writer.append("</kml>\n");    
  }
}
