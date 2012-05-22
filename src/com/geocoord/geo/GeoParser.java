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
  private static final int QUADRANT_POLYGON_APPROX_SIDES = 16;
  
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
    return parsePolygon(polygon, resolution, new Coverage());
  }
  
  public static Coverage parsePolygon(String polygon, int resolution, Coverage coverage) {
    
    String[] latlons = polygon.split(",");

    // If there are less than 3 points, return an empty coverage.
    if (latlons.length < 3) {
      return new Coverage();
    }

    //List<Long> vertices = new ArrayList<Long>();
    
    List<Long> verticesLat = new ArrayList<Long>();
    List<Long> verticesLon = new ArrayList<Long>();
    
    for (String latlon: latlons) {
      String[] ll = latlon.split(":");
      
      if (ll.length == 2) {
        try {
          double lat = Double.valueOf(ll[0]);
          double lon = Double.valueOf(ll[1]);
          
          verticesLat.add(HHCodeHelper.toLongLat(lat));
          verticesLon.add(HHCodeHelper.toLongLon(lon));
          
          //vertices.add(HHCodeHelper.getHHCodeValue(lat, lon));          
        } catch (NumberFormatException nfe) {
          // Invalid point, return empty coverage
          return new Coverage();
        }        
      }
    }
    
    // Remove last point if it's the same as the first one
    if (verticesLat.get(0).equals(verticesLat.get(verticesLat.size() - 1)) && verticesLon.get(0).equals(verticesLon.get(verticesLon.size() - 1))) {
      verticesLat.remove(0);
      verticesLon.remove(0);
    }
    /*
    if (vertices.get(0).equals(vertices.get(vertices.size() - 1))) {
      vertices.remove(0);
    }
    */

    //return HHCodeHelper.coverPolygon(vertices, resolution);
    return HHCodeHelper.coverPolygon(verticesLat, verticesLon, resolution, coverage);
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
    return parsePath(path, resolution, new Coverage());
  }
  
  public static Coverage parsePath(String path, int resolution, Coverage coverage) {
    
    String[] coords = path.split(",");

    if (coords.length < 2) {
      return coverage;
    }
    
    double distance = 0L;

    long fromLat;
    long fromLon;
    
    long toLat;
    long toLon;
    
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
    
        fromLat = HHCodeHelper.toLongLat(Double.valueOf(from[0]));
        fromLon = HHCodeHelper.toLongLon(Double.valueOf(from[1]));
        
        // Extract 'to'
        toLat = HHCodeHelper.toLongLat(Double.valueOf(to[0]));
        toLon = HHCodeHelper.toLongLon(Double.valueOf(to[1]));
        
        if (distance > 0) {
          // Compute coverage
          Coverage segmentCoverage = HHCodeHelper.coverSegment(fromLat, fromLon, toLat, toLon, distance, resolution);
        
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
    return parseCircle(circle, resolution, new Coverage());
  }

  public static Coverage parseCircle(String circle, int resolution, Coverage coverage) {
    String[] tokens = circle.split(":");
    
    // Not three tokens? return an empty coverage.
    if (tokens.length != 3) {
      return coverage;
    }
    
    try {
      double lat = Double.valueOf(tokens[0]);
      double lon = Double.valueOf(tokens[1]);
      double radius = Math.abs(Double.valueOf(tokens[2]));
   
      long[] centercoords = new long[2];
      
      centercoords[0] = HHCodeHelper.toLongLat(lat);
      centercoords[1] = HHCodeHelper.toLongLon(lon);
      
      //
      // Build a polygon approximation with 12 sides
      //
      
      List<Long> verticesLat = new ArrayList<Long>(4 * QUADRANT_POLYGON_APPROX_SIDES);
      List<Long> verticesLon = new ArrayList<Long>(4 * QUADRANT_POLYGON_APPROX_SIDES);
      
      // Populate the list
      for (int i = 0; i < 4 * QUADRANT_POLYGON_APPROX_SIDES; i++) {
        verticesLat.add(0L);
        verticesLon.add(0L);
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
      
        verticesLat.set(i, (long) (centercoords[0] + s * latradius));
        verticesLon.set(i, (long) (centercoords[1] + c * lonradius));
        
        verticesLat.set(i + QUADRANT_POLYGON_APPROX_SIDES, (long) (centercoords[0] + c * latradius));
        verticesLon.set(i + QUADRANT_POLYGON_APPROX_SIDES, (long) (centercoords[1] - s * lonradius));
        
        verticesLat.set(i + 2 * QUADRANT_POLYGON_APPROX_SIDES, (long) (centercoords[0] - s * latradius));
        verticesLon.set(i + 2 * QUADRANT_POLYGON_APPROX_SIDES, (long) (centercoords[1] - c * lonradius));
        
        verticesLat.set(i + 3 * QUADRANT_POLYGON_APPROX_SIDES, (long) (centercoords[0] - c * latradius));
        verticesLon.set(i + 3 * QUADRANT_POLYGON_APPROX_SIDES, (long) (centercoords[1] + s * lonradius));        
      }
            
      return HHCodeHelper.coverPolygon(verticesLat, verticesLon, resolution, coverage);     
    } catch (NumberFormatException e) {
      // Return an empty coverage
      return coverage;
    }
  }
  
  /**
   * Parse a view port and return a coverage for it at the given resolution.
   * A viewport is represented as
   * 
   * SWLat:SWLon,NELat:NELon
   * 
   * @param viewport
   * @param resolution
   * @return
   */
  public static Coverage parseViewport(String viewport, int resolution) {
    return parseViewport(viewport, resolution, new Coverage());
  }

  public static Coverage parseViewport(String viewport, int resolution, Coverage coverage) {

    
    String[] latlon = viewport.split(",");
    
    // More or less than 2 points? bail out.
    if (latlon.length != 2) {
      return coverage;
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
      List<Long> verticesLat = new ArrayList<Long>();
      List<Long> verticesLon = new ArrayList<Long>();
      
      verticesLat.add(HHCodeHelper.toLongLat(swlat));
      verticesLon.add(HHCodeHelper.toLongLon(swlon));
      
      verticesLat.add(HHCodeHelper.toLongLat(swlat));
      verticesLon.add(HHCodeHelper.toLongLon(nelon));
      
      verticesLat.add(HHCodeHelper.toLongLat(nelat));
      verticesLon.add(HHCodeHelper.toLongLon(nelon));
      
      verticesLat.add(HHCodeHelper.toLongLat(nelat));
      verticesLon.add(HHCodeHelper.toLongLon(swlon));

      verticesLat.add(HHCodeHelper.toLongLat(swlat));
      verticesLon.add(HHCodeHelper.toLongLon(swlon));

      /*
      vertices.add(HHCodeHelper.getHHCodeValue(swlat,swlon));
      vertices.add(HHCodeHelper.getHHCodeValue(swlat,nelon));
      vertices.add(HHCodeHelper.getHHCodeValue(nelat,nelon));
      vertices.add(HHCodeHelper.getHHCodeValue(nelat,swlon));
      vertices.add(HHCodeHelper.getHHCodeValue(swlat,swlon));
      */
      
      return HHCodeHelper.coverPolygon(verticesLat, verticesLon, resolution, coverage);
    } catch (NumberFormatException nfe) {
      return coverage;
    }
  }
  
  /**
   * Parse a Google Maps encoded polyline.
   * @see http://code.google.com/apis/maps/documentation/utilities/polylinealgorithm.html
   * @see http://jeffreysambells.com/posts/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java/
   * 
   * @return a List of HHCodes.
   */
  public static List<Long>[] parseEncodedPolyline(String polyline) {
    
    int index = 0;
    int len = polyline.length();
    int lat = 0;
    int lng = 0;

    List<Long> verticesLat = new ArrayList<Long>();
    List<Long> verticesLon = new ArrayList<Long>();
    
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

      verticesLat.add(HHCodeHelper.toLongLat((double) lat / 1E5));
      verticesLon.add(HHCodeHelper.toLongLon((double) lng / 1E5));
      
      //hhcodes.add(HHCodeHelper.getHHCodeValue((double) lat / 1E5, (double) lng / 1E5)); 
    }
   
    List<Long>[] coords = new List[2];
    
    coords[0] = verticesLat;
    coords[1] = verticesLon;
    
    return coords;
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
    return parseArea(def, resolution, new Coverage());
  }
  public static Coverage parseArea(String def, int resolution, Coverage coverage) {
    if (def.startsWith("circle:")) {
      return parseCircle(def.substring(7), resolution, coverage);
    } else if (def.startsWith("polygon:")) {
      return parsePolygon(def.substring(8), resolution, coverage);
    } else if (def.startsWith("rect:")) {
      return parseViewport(def.substring(5), resolution, coverage);
    } else if (def.startsWith("path:")) {
      return parsePath(def.substring(5), resolution, coverage);
    } else if (def.startsWith("polyline:")) {
      // Extract distance
      int idx = def.substring(9).indexOf(":");
      
      Coverage cover = coverage;
      
      if (-1 == idx) {
        return cover;
      }
      
      try {
        double dist = Double.valueOf(def.substring(9,idx));
        List<Long>[] hhcoords = parseEncodedPolyline(def.substring(9 + idx + 1));
        
        for (int i = 0; i < hhcoords[0].size() - 1; i++) {
          HHCodeHelper.coverSegment(hhcoords[0].get(i), hhcoords[1].get(i), hhcoords[0].get(i+1), hhcoords[1].get(i + 1), dist, resolution, cover);
        }
        
        return cover;
      } catch (NumberFormatException nfe) {
        return cover;
      }
    } else {
      return coverage;
    }
  }
}
