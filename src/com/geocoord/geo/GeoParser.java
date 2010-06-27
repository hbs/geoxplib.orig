package com.geocoord.geo;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that parses various String representations of
 * geometric shapes.
 */
public class GeoParser {
  
  /**
   * Number of sides on each quadrant of the polygon approximation of a circle.
   */
  private static final int QUADRANT_POLYGON_APPROX_SIDES = 4;
  
  /**
   * Parse a textual representation of a polygon into a coverage.
   * The format of the polygon is:
   * 
   * lat:lon,lat:lon,lat:lon(,lat:lon)*
   * 
   * @param polygon
   * @param resolution
   * @return The coverage at the given resolution
   */
  public static Coverage parsePolygon(String polygon, int resolution) {
    
    String[] latlons = polygon.split(",");

    // If there are less than 3 points, return an empty coverage.
    if (latlons.length < 3) {
      return new Coverage();
    }

    List<Long> vertices = new ArrayList<Long>();
    
    for (String latlon: latlons) {
      String[] ll = latlon.split(":");
      
      if (ll.length == 2) {
        try {
          double lat = Double.valueOf(ll[0]);
          double lon = Double.valueOf(ll[1]);
          
          vertices.add(HHCodeHelper.getHHCodeValue(lat, lon));          
        } catch (NumberFormatException nfe) {
          // Invalid point, return empty coverage
          return new Coverage();
        }        
      }
    }
    
    // Remove last point if it's the same as the first one
    if (vertices.get(0).equals(vertices.get(vertices.size() - 1))) {
      vertices.remove(0);
    }

    return HHCodeHelper.coverPolygon(vertices, resolution);
  }
  
  /**
   * Return a Coverage covering a path at a given distance (at the given resolution)
   * 
   * The format is:
   * 
   * lat:lon:distance(,lat:lon(:distance)?)+
   * 
   * Distance can be changed per segment
   * 
   * @param path
   * @param resolution
   * @return
   */
  public static Coverage parsePath(String path, int resolution) {
    
    String[] coords = path.split(",");

    Coverage coverage = new Coverage();
    
    if (coords.length < 2) {
      return coverage;
    }
    
    double distance = 0L;
    
    long fromhh;
    long tohh;
    
    String[] from = coords[0].split(":");
    
    for (int i = 1; i < coords.length; i ++) {
      
      String[] to = coords[i].split(":");
      
      // Extract side distance for next segment,
      // if not set, use last distance found
      
      try {       
        // FIXME(hbs): we actually call getHHCodeValue twice for each coord, this could be optimized,
        //             but let's bet the JIT/JVM will do just fine.
        
        // A distance was specified
        if (from.length == 3) {
          distance = Double.valueOf(from[2]);
        }

        fromhh = HHCodeHelper.getHHCodeValue(Double.valueOf(from[0]), Double.valueOf(from[1]));
        
        // Extract 'to'
        tohh = HHCodeHelper.getHHCodeValue(Double.valueOf(to[0]), Double.valueOf(to[1]));

        if (distance > 0) {
          // Compute coverage
          Coverage segmentCoverage = HHCodeHelper.coverSegment(fromhh, tohh, distance, resolution);
        
          // Merge coverage with current one
          coverage.merge(segmentCoverage);
        }
        
        // Shift 'to' to 'from'
        from = to;
      } catch (NumberFormatException nfe) {
        // Invalid number, return empty coverage
        return new Coverage();
      }
    }

    return coverage;
  }
  
  /**
   * Parse a center/radius String and return a coverage covering it
   * at the specified resolution.
   * 
   * Format is lat:lon:radius with a radius in meters.
   * 
   * @param circle
   * @param resolution
   * @return
   */
  public static Coverage parseCircle(String circle, int resolution) {
    String[] tokens = circle.split(":");
    
    // Not three tokens? return an empty coverage.
    if (tokens.length != 3) {
      return new Coverage();
    }
    
    try {
      double lat = Double.valueOf(tokens[0]);
      double lon = Double.valueOf(tokens[1]);
      double radius = Math.abs(Double.valueOf(tokens[2]));
   
      long center = HHCodeHelper.getHHCodeValue(lat, lon);
      long[] centercoords = HHCodeHelper.splitHHCode(center, HHCodeHelper.MAX_RESOLUTION);
      
      //
      // Build a polygon approximation with 12 sides
      //
      
      List<Long> vertices = new ArrayList<Long>(4 * QUADRANT_POLYGON_APPROX_SIDES);
      
      // Populate the list
      for (int i = 0; i < 4 * QUADRANT_POLYGON_APPROX_SIDES; i++) {
        vertices.add(0L);
      }
      
      // Compute scale at center latitude
      double scale = Math.cos(Math.toRadians(lat));
      
      long latradius = Math.round(radius * HHCodeHelper.latUnitsPerMeter);
      long lonradius = Math.round(radius * HHCodeHelper.lonUnitsPerMeter / scale);

      double sideangle = Math.PI * 2.0 / (4.0 * QUADRANT_POLYGON_APPROX_SIDES);
      
      // Build a polygon approximation of the circle. Only loop through
      // one quadrant to speed up computations.
      
      for (int i = 0; i < QUADRANT_POLYGON_APPROX_SIDES; i++) {
        double a = i * sideangle;
        double c = Math.cos(a);
        double s = Math.sin(a);
        
        vertices.set(i,HHCodeHelper.buildHHCode((long) (centercoords[0] + s * latradius), (long) (centercoords[1] + c * lonradius), HHCodeHelper.MAX_RESOLUTION));
        vertices.set(i + QUADRANT_POLYGON_APPROX_SIDES,HHCodeHelper.buildHHCode((long) (centercoords[0] + c * latradius), (long) (centercoords[1] - s * lonradius), HHCodeHelper.MAX_RESOLUTION));
        vertices.set(i + 2 * QUADRANT_POLYGON_APPROX_SIDES,HHCodeHelper.buildHHCode((long) (centercoords[0] - s * latradius), (long) (centercoords[1] - c * lonradius), HHCodeHelper.MAX_RESOLUTION));
        vertices.set(i + 3 * QUADRANT_POLYGON_APPROX_SIDES,HHCodeHelper.buildHHCode((long) (centercoords[0] - c * latradius), (long) (centercoords[1] + s * lonradius), HHCodeHelper.MAX_RESOLUTION));
      }
            
      return HHCodeHelper.coverPolygon(vertices, resolution);     
    } catch (NumberFormatException e) {
      // Return an empty coverage
      return new Coverage();
    }
  }
  
  /**
   * Parse a view port and return a coverage for it at the given resolution.
   * A viewport is represented as
   * 
   * SWLat,SWLon:NELat,NELon
   * 
   * @param viewport
   * @param resolution
   * @return
   */
  public static Coverage parseViewport(String viewport, int resolution) {
    
    String[] latlon = viewport.split(",");
    
    // More or less than 2 points? bail out.
    if (latlon.length != 2) {
      return new Coverage();
    }
    
    String[] sw = latlon[0].split(":");
    String[] ne = latlon[1].split(":");
    
    try {
      double swlat = Double.valueOf(sw[0]);
      double swlon = Double.valueOf(sw[1]);
      
      double nelat = Double.valueOf(ne[0]);
      double nelon = Double.valueOf(ne[1]);
     
      if (swlat > nelat) {
        double tmp = swlat;
        swlat = nelat;
        nelat = tmp;
      }
      
      if (swlon > nelon) {
        double tmp = swlon;
        swlon = nelon;
        nelon = tmp;
      }
      
      List<Long> vertices = new ArrayList<Long>();
      vertices.add(HHCodeHelper.getHHCodeValue(swlat,swlon));
      vertices.add(HHCodeHelper.getHHCodeValue(swlat,nelon));
      vertices.add(HHCodeHelper.getHHCodeValue(nelat,nelon));
      vertices.add(HHCodeHelper.getHHCodeValue(nelat,swlon));
      vertices.add(HHCodeHelper.getHHCodeValue(swlat,swlon));
      
      return HHCodeHelper.coverPolygon(vertices, resolution);
    } catch (NumberFormatException nfe) {
      return new Coverage();
    }
  }
  
  /**
   * Parse a Google Maps encoded polyline.
   * @see http://code.google.com/apis/maps/documentation/utilities/polylinealgorithm.html
   * @see http://jeffreysambells.com/posts/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java/
   * 
   * @return a List of HHCodes.
   */
  public static List<Long> parseEncodedPolyline(String polyline) {
    
    int index = 0;
    int len = polyline.length();
    int lat = 0;
    int lng = 0;

    List<Long> hhcodes = new ArrayList<Long>();
    
    while (index < len) {
      int b;
      int shift = 0;
      int result = 0;

      do {
        b = polyline.charAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20 && index < len);

      int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lat += dlat;

      shift = 0;
      result = 0;
      
      if (index < len) {
        do {
          b = polyline.charAt(index++) - 63;
          result |= (b & 0x1f) << shift;
          shift += 5;
        } while (b >= 0x20 && index < len);
        
        int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
        lng += dlng;        
      }

      hhcodes.add(HHCodeHelper.getHHCodeValue((double) lat / 1E5, (double) lng / 1E5)); 
    }
   
    return hhcodes;
  }
  
  /**
   * Parse a simple lat/lon encoded as 'lat:lon'   
   * @param latlon
   * @return
   */
  public static long parseLatLon(String latlon) {
    String[] tokens = latlon.split(":");
    
    return HHCodeHelper.getHHCodeValue(Double.valueOf(tokens[0]), Double.valueOf(tokens[1]));
  }
  
  public static Coverage parseArea(String def, int resolution) {
    if (def.startsWith("circle:")) {
      return parseCircle(def.substring(7), resolution);
    } else if (def.startsWith("polygon:")) {
      return parsePolygon(def.substring(8), resolution);
    } else if (def.startsWith("rect:")) {
      return parseViewport(def.substring(5), resolution);
    } else if (def.startsWith("path:")) {
      return parsePath(def.substring(5), resolution);
    } else if (def.startsWith("polyline:")) {
      // Extract distance
      int idx = def.substring(9).indexOf(":");
      
      Coverage cover = new Coverage();
      
      if (-1 == idx) {
        return cover;
      }
      
      try {
        double dist = Double.valueOf(def.substring(9,idx));
        List<Long> hhcodes = parseEncodedPolyline(def.substring(9 + idx + 1));
        
        for (int i = 0; i < hhcodes.size() - 1; i++) {
          cover.merge(HHCodeHelper.coverSegment(hhcodes.get(i), hhcodes.get(i+1), dist, resolution));
        }
        
        return cover;
      } catch (NumberFormatException nfe) {
        return cover;
      }
    } else {
      return new Coverage();
    }
  }
}
