package com.geocoord.geo.filter;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.geocoord.geo.HHCodeHelper;

public class ViewportGeoFilter implements GeoFilter {
  
  private final double swlat;
  private final double swlon;
  private final double nelat;
  private final double nelon;
  
  private final long longswlat;
  private final long longswlon;
  private final long longnelat;
  private final long longnelon;
  
  public ViewportGeoFilter(List<Double> viewport) {
    swlat = viewport.get(0);
    swlon = viewport.get(1);
    nelat = viewport.get(2);
    nelon = viewport.get(3);
    
    longswlat = HHCodeHelper.toLongLat(swlat);
    longnelat = HHCodeHelper.toLongLat(nelat);
    longswlon = HHCodeHelper.toLongLon(swlon);
    longnelon = HHCodeHelper.toLongLon(nelon);    
  }
  
  @Override
  public boolean contains(double lat, double lon) {
    return (lat >= swlat && lat <= nelat) && (lon >= swlon && lon <= nelon);
  }

  @Override
  public boolean contains(long longlat, long longlon) {
    return (longlat >= longswlat && longlat <= longnelat) && (longlon >= longswlon && longlon <= longnelon);
  }
  
  @Override
  public boolean contains(long hhcode) {
    throw new NotImplementedException();
    //return false;
  }
}
