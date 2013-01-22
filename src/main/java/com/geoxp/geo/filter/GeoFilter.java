package com.geoxp.geo.filter;

/**
 * Interface to define Geographic Filters.
 * 
 * A GeoFilter  is a mechanism to determine if a point is in or out of the
 * filtering zone.
 * 
 * GeoFilters are supposed to perform accurate checks, compared to Coverage which
 * do coarser checks due to their multiresolution nature.
 */
public interface GeoFilter {
  /**
   * Checks if the given hhcode is contained by the filter or not.
   * 
   * @param hhcode
   * @param resolution
   * @return
   */
  public boolean contains(long hhcode);
  
  /**
   * Checks if the point defined by its latitude and longitude is contained by the filter or not.
   * 
   * @param lat
   * @param lon
   * @return
   */
  public boolean contains(double lat, double lon);
  
  /**
   * Checks id the given long lat/lon point is contained by the filter or not. 
   * 
   * @param longlat
   * @param longlon
   * @return
   */
  public boolean contains(long longlat, long longlon);
}
