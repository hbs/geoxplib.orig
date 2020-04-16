//
//   GeoXP Lib, library for efficient geo data manipulation
//
//   Copyright 2020-      SenX S.A.S.
//   Copyright 2019-2020  iroise.net S.A.S.
//   Copyright 1999-2019  Mathias Herberts
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package com.geoxp;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collection;

import com.geoxp.geo.Coverage;
import com.geoxp.geo.CoverageHelper;
import com.geoxp.geo.HHCodeHelper;
import com.geoxp.geo.JTSHelper;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Wrapper class for library methods.
 * 
 * Library manipulates the following entities
 * 
 *   GeoXPPoint A 64 bits value representing a location with sub centimeter precision
 *   
 *   GeoXPCell  A 64 bits value representing a rectangle area at one of 15 resolutions
 *              The smallest addressable area is about 1 square cm, the largest about 100M square km
 *
 *   GeoXPShape A set of GeoXPCells covering a shape
 *
 */
public final class GeoXPLib {
  
  private static final long[] LOWER_BITS;
  
  static {
    LOWER_BITS = new long[16];
    
    for (int i = 0; i < 16; i++) {
      LOWER_BITS[i] = 0xFFFFFFFFL >>> ((i + 1) * 2);
    }
  }
  public static final class GeoXPShape implements Serializable {
    long[] geocells;
  }
  
  /**
   * Converts (lat,lon) coordinates into a GeoXPPoint.
   * 
   * @param lat Latitude in decimal degrees
   * @param lon Longitude in decimal degrees
   * @return A GeoXPPoint representing the same location as (lat,lon)
   */
	public static long toGeoXPPoint(double lat, double lon) {
	  return HHCodeHelper.getHHCodeValue(lat,lon);
	}
	
	/**
	 * Converts (x,y) coordinates as returned by xyFromGeoXPPoint into
	 * a GeoXPPoint.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public static long toGeoXPPoint(long x, long y) {
	  return HHCodeHelper.buildHHCode(x, y, HHCodeHelper.MAX_RESOLUTION);
	}
	
	/**
	 * Return a GeoXPPoint which represents the center of the cell at
	 * 'resolution' (even from 2 to 32) which contains 'geoxppoint'
	 * 
	 * @param geoxppoint
	 * @param resolution
	 * @return
	 */
	public static long centerGeoXPPoint(long geoxppoint, int resolution) {
	  return HHCodeHelper.getCenter(geoxppoint, resolution);
	}
	
	/**
	 * Converts a GeoXPPoint to (lat,lon) coordinates
	 * 
	 * @param geoxppoint GeoXPPoint to convert
	 * @return A pair of lat,lon coordinates in decimal degrees, representing the same location as the GeoXPPoint
	 */
	public static double[] fromGeoXPPoint(long geoxppoint) {
	  return HHCodeHelper.getLatLon(geoxppoint, HHCodeHelper.MAX_RESOLUTION);
	}
	
	/**
	 * Converts a GeoXPPoint to long coordinates representing latitude and longitude
	 * 
	 * @param geoxppoint GeoXPPoint to conver
	 * @return A pair of long coordinates homeomorphous to lat,lon
	 */
	public static long[] xyFromGeoXPPoint(long geoxppoint) {
	  return HHCodeHelper.splitHHCode(geoxppoint, HHCodeHelper.MAX_RESOLUTION);
	}
	
	/**
	 * Determine if a GeoXPPoint is contained in a GeoXPShape
	 * 
	 * @param geoxppoint GeoXPPoint to check
	 * @param geoxpshape GeoXPShape to check
	 * @return true if geoxpshape contains geoxppoint, false otherwise 
	 */
	public static boolean isGeoXPPointInGeoXPShape(long geoxppoint, GeoXPShape geoxpshape) {
	  return Coverage.contains(geoxpshape.geocells, geoxppoint);	  
	}
	
	/**
	 * Converts a JTS Geometry into a GeoXPShape
	 * 
	 * @param geometry The JTS Geometry instance to convert.
	 * @param pctError The precision (in % of the geometry's envelope diagonal)
	 * @param inside Should the compute coverge be completely inside the Geometry (useful when subtracting)
	 * @param maximum number of cells
	 * 
	 * @return the resulting GeoXPShape
	 */
  public static GeoXPShape toGeoXPShape(Geometry geometry, double pctError, boolean inside, int maxcells) {
    //
    // Compute bbox of 'geometry'
    //
    
    long[] bbox = HHCodeHelper.getBoundingBox(geometry);
    
    //
    // Compute optimal resolution
    //
    
    int res = HHCodeHelper.getOptimalResolution(bbox, pctError);

    return toGeoXPShape(geometry, res, inside, maxcells);
  }
  
  public static GeoXPShape toGeoXPShape(Geometry geometry, int maxres, boolean inside, int maxcells) {
    //
    // Compute Coverage and return its geocells
    //
    
    GeoXPShape geoxpshape = new GeoXPShape();
    
    Coverage c = JTSHelper.coverGeometry(geometry, 2, maxres, inside, maxcells);
    
    if (null == c) {
      return null;
    }
    
    c.optimize(0L);
    geoxpshape.geocells = c.toGeoCells(maxres);  
    
    return geoxpshape;
  }

  public static GeoXPShape toGeoXPShape(Geometry geometry, double pctError, boolean inside) {
    return toGeoXPShape(geometry, pctError, inside, Integer.MAX_VALUE);
  }
  
  /**
   * Converts a JTS Geometry into a GeoXPShape by using a single resolution
   * computed so the error is less or equal to pctError percent of the geometry's envelope diagonal.
   * 
   * @param geometry The JTS Geometry instance to convert.
   * @param pctError The precision (in % of the geometry's envelope diagonal)
   * @param inside Should the compute coverage be completely inside the Geometry (useful when subtracting)
   * 
   * @return
   */
  public static GeoXPShape toUniformGeoXPShape(Geometry geometry, double pctError, boolean inside, int maxcells) {
    //
    // Compute bbox of 'geometry'
    //
    
    long[] bbox = HHCodeHelper.getBoundingBox(geometry);
    
    //
    // Compute optimal resolution
    //
    
    int res = HHCodeHelper.getOptimalResolution(bbox, pctError);

    return toUniformGeoXPShape(geometry, res, inside, maxcells);
  }

  public static GeoXPShape toUniformGeoXPShape(Geometry geometry, int res, boolean inside, int maxcells) {
    //
    // Compute Coverage at 'res' and return its geocells
    //
    
    GeoXPShape geoxpshape = new GeoXPShape();
    
    Coverage c = JTSHelper.coverGeometry(geometry, res, res, inside, maxcells);

    if (null == c) {
      return null;
    }
    
    geoxpshape.geocells = c.toGeoCells(res);    

    return geoxpshape;
  }
  
  /**
   * Return the bounding box of a GeoXPShape
   */
  public static double[] bbox(GeoXPShape shape) {
    long[] coords = new long[2];
    long[] bbox = new long[4];
    
    bbox[0] = Long.MAX_VALUE;
    bbox[1] = Long.MAX_VALUE;
    bbox[2] = Long.MIN_VALUE;
    bbox[3] = Long.MIN_VALUE;
    
    for (long cell: shape.geocells) {
      // Extract resolution
      int res = (int) (cell >>> 60);
      
      // Split HHCode
      HHCodeHelper.stableSplitHHCode(cell << 4, 32, coords);
      
      if (coords[0] < bbox[0]) {
        bbox[0] = coords[0];
      }
      if (coords[1] < bbox[1]) {
        bbox[1] = coords[1];
      }
      long tmp = coords[0] | LOWER_BITS[res - 1];
      if (tmp > bbox[2]) {
        bbox[2] = tmp;
      }
      tmp = coords[1] | LOWER_BITS[res - 1];
      if (tmp > bbox[3]) {
        bbox[3] = tmp;
      }
    }
    
    double[] dbbox = new double[4];
    
    dbbox[0] = HHCodeHelper.toLat(bbox[0]);
    dbbox[1] = HHCodeHelper.toLon(bbox[1]);
    dbbox[2] = HHCodeHelper.toLat(bbox[2]);
    dbbox[3] = HHCodeHelper.toLon(bbox[3]);
    
    return dbbox;
  }
  
	/**
	 * Return a GeoXPShape which is the intersection of two GeoXPShapes
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static GeoXPShape intersection(GeoXPShape a, GeoXPShape b) {
	  Coverage ca = new Coverage(a.geocells);
	  Coverage cb = new Coverage(b.geocells);
	  
	  Coverage c = Coverage.intersection(ca, cb, false);
	  c.optimize(0L);
	  
	  GeoXPShape intersection = new GeoXPShape();
	  intersection.geocells = c.toGeoCells(HHCodeHelper.MAX_RESOLUTION);
	  
	  return intersection;
	}
	
	/**
	 * Return a GeoXPShape which is the union of two GeoXPShapes
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static GeoXPShape union(GeoXPShape a, GeoXPShape b) {
	  Coverage ca = new Coverage(a.geocells);
	  Coverage cb = new Coverage(b.geocells);
	  
	  ca.merge(cb);
	  ca.dedup();
	  ca.optimize(0L);
	  
	  GeoXPShape union = new GeoXPShape();
	  union.geocells = ca.toGeoCells(HHCodeHelper.MAX_RESOLUTION);
	  
	  return union;
	}
	
	/**
	 * Return a GeoXPShape which is the result of subtraction the second
	 * GeoXP Shape from the first one.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static GeoXPShape subtraction(GeoXPShape a, GeoXPShape b) {
	  Coverage ca = new Coverage(a.geocells);
	  Coverage cb = new Coverage(b.geocells);
	  
	  Coverage c = Coverage.minus(ca, cb, false);
	  c.optimize(0L);
	  
	  GeoXPShape subtraction = new GeoXPShape();
	  subtraction.geocells = c.toGeoCells(HHCodeHelper.MAX_RESOLUTION);
	  
	  return subtraction;
	}
	
	/**
	 * Compute the loxodromic (rhumb line) distance in meters between locations
	 * 'from' and 'to'.
	 * 
	 * @param from First point
	 * @param to Second point
	 * @return The loxodromic (rhumb line) distance between the first and second points
	 */
	public static double loxodromicDistance(long from, long to) {
	  return HHCodeHelper.loxodromicDistance(from, to);
	}
	
	/**
	 * Compute the orthodromic (great circle) distance in meters between locations
	 * 
	 * @param from
	 * @param to
	 * @return The orthodromic distance between the two locations
	 */
	public static double orthodromicDistance(long from, long to) {
	  return HHCodeHelper.orthodromicDistance(from, to);
	}
	
	public static byte[] serializeGeoXPShape(GeoXPShape geoxpshape) {
	  byte[] buf = new byte[geoxpshape.geocells.length * 8];
	  ByteBuffer bb = ByteBuffer.wrap(buf);
	  bb.order(ByteOrder.BIG_ENDIAN);
	  for (int i = 0; i < geoxpshape.geocells.length; i++) {
	    bb.putLong(geoxpshape.geocells[i]);
	  }
	  return buf;
	}
	
	public static byte[] bytesFromGeoXPPoint(long geoxppoint, int resolution) {
	  // Ignore odd resolutions or resolution below 2 and above 32
	  if (resolution < 2 || resolution > 32 || 0 != (resolution & 0x1)) {
	    return null;
	  }
	  
	  byte[] bytes = new byte[(resolution >>> 2) + (0 == (resolution & 2) ? 0 : 1)];
	  
	  int idx = 0;
	  int res = 0;
	  
	  while(res < (resolution << 1)) {
	    bytes[idx] |= (geoxppoint >> (60 - res)) & 0x0f;
	    if (0 == res % 8) {
	      bytes[idx] = (byte) (bytes[idx] << 4);
	    } else {
	      idx++;
	    }
	    res += 4;
	  }
	  
	  return bytes;
	}
	
	public static long[] indexable(long geoxppoint) {
	  return HHCodeHelper.toGeoCells(geoxppoint);
	}
	
	public static String[] indexableStrings(long geoxppoint) {
	  return HHCodeHelper.toIndexableStrings(geoxppoint);
	}
	
	public static String toRegexp(GeoXPShape shape) {
	  return HHCodeHelper.geocellsToRegexp(shape.geocells);
	}
	
	public static long[] getCells(GeoXPShape shape) {
	  return shape.geocells;
	}
	
	public static GeoXPShape fromCells(long[] cells, boolean copy) {
	  GeoXPShape shape = new GeoXPShape();
	  shape.geocells = copy ? Arrays.copyOf(cells, cells.length) : cells;
	  return shape;
	}
	
	public static GeoXPShape fromCells(Collection<Long> cells) {
	  long[] lcells = new long[cells.size()];
	  int idx = 0;
	  for (long cell: cells) {
	    lcells[idx++] = cell;
	  }
	  return fromCells(lcells, false);
	}
	
	public static long parentCell(long cell) {
	  return HHCodeHelper.parentGeoCell(cell);
	}
	
	/**
	 * Limit the number of cells in GeoXPShape 'shape' to 'count'
	 * 
	 * @param shape
	 * @param count
	 * @return
	 */
	public static GeoXPShape limit(GeoXPShape shape, int count) {
	  if (shape.geocells.length <= count) {
	    return shape;
	  }
    Coverage c = CoverageHelper.fromGeoCells(shape.geocells);
    c.reduce(count);
    GeoXPShape reduced = new GeoXPShape();
    reduced.geocells = c.toGeoCells(30);
    return reduced;
	}
	
	/**
	 * Limit the resolution of a GeoXPShape to a minimum of 'res'
	 * 
	 * @param shape
	 * @param res Finest 
	 * @return
	 */
	public static GeoXPShape limitResolution(GeoXPShape shape, int res) {
    Coverage c = CoverageHelper.fromGeoCells(shape.geocells);
    long thresholds = 0x1111111111111111L;
    c.optimize(thresholds, res);
    GeoXPShape reduced = new GeoXPShape();
    reduced.geocells = c.toGeoCells(30);
    return reduced;
	}
	
	public static GeoXPShape limitResolution(GeoXPShape shape, int minresolution, int maxresolution) {
	  Coverage c = CoverageHelper.fromGeoCells(shape.geocells);
	  long thresholds = 0x1111111111111111L;
	  c.optimize(thresholds, minresolution, maxresolution, 0);
	  GeoXPShape reduced = new GeoXPShape();
	  reduced.geocells = c.toGeoCells(30);
	  return reduced;
	}
}
