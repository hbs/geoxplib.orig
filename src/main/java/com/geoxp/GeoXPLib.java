package com.geoxp;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.geoxp.geo.Coverage;
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
	 * @return the resulting GeoXPShape
	 */
	public static GeoXPShape toGeoXPShape(Geometry geometry, double pctError) {
	  //
	  // Compute bbox of 'geometry'
	  //
	  
	  long[] bbox = HHCodeHelper.getBoundingBox(geometry);
	  
	  //
	  // Compute optimal resolution
	  //
	  
	  int res = HHCodeHelper.getOptimalResolution(bbox, pctError);
	  
	  //
	  // Compute Coverage and return its geocells
	  //
	  
	  GeoXPShape geoxpshape = new GeoXPShape();
	  
	  geoxpshape.geocells = JTSHelper.coverGeometry(geometry, res, false).toGeoCells(res);
	  
	  return geoxpshape;
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
}
