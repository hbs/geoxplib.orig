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

package com.geoxp.geo;

public class LatLonUtils {
  
  /**
   * Earth radius (a) as defined for the WGS84 ellipsoid at the equator.
   */
  public static final double WGS84_EARTH_RADIUS_METERS = 6378137.0;
  
  /**
   * Compute the distance in radians between two points using sperical trigonometry.
   * This method comes directly from the original GeoCoord source dating back to 2003.
   *  
   * @param fromLat Latitude in radians of the starting point.
   * @param fromLon Longitude in radians of the starting point.
   * @param toLat Lat in rad of the end point.
   * @param toLon Lon in rad of the end point.
   * @return The computed distance in radians.
   */
  public static final double getRadDistance(double fromLat, double fromLon, double toLat, double toLon) {
    
    /*
     * We compute the distance using the well known Spherical Trigonometry
     * formula:
     *
     * d = ACOS(SIN(latA)*SIN(latB)+COS(latA)*COS(latB)*COS(lonB-lonA))
     *
     * All angles being expressed in radians, d is the distance in radians.
     * 
     * The distance can be converted to minutes of angle (* 180 * 60 / PI) to have it in Nautical Miles.
     */

    double cosdist = Math.sin(fromLat)
                     *Math.sin(toLat)
                     +Math.cos(fromLat)
                     *Math.cos(toLat)
                     *Math.cos(toLon-fromLon);

    double dist;

    if (cosdist <= 1.0) {
        dist = Math.acos(cosdist);
    } else {
        dist = 0.0;
    }
    
    return dist;
  }
  
  /*
  public static final double getBearing(double fromLat, double fromLon, double toLat, double toLon) {

    //
    // Start by converting dest to our datum.
    //

    LatLon p = getDatum().convert (dest);

    //
    // Bearing is computed according to the following formula:
    //
    //   cot Rv = tan(lat2)cos(lat1)/sin(lon2-lon1) - sin(lat1)cot(lon2-lon1)
    //
    // Where lat1,lon1 is the position of the user and lat2,lon2 the target
    // waypoint.
    //
    // if (lon2-lon1)*arccot(Rv) > 0 Rv = 360 - arccot(Rv)
    // if (lon2-lon1)*arccot(Rv) < 0 Rv = 180 - arccot(Rv)
    //
    
    double g = p.getRadLon () - getRadLon ();
    
    double clat = Math.cos (getRadLat());
    double slat = Math.sin (getRadLat());
    double slon = Math.sin (g);
    double clon = Math.cos (g);
    double slatb = Math.sin (p.getRadLat());
    double clatb = Math.cos (p.getRadLat());

    double dist = radDist(p);

    double bearing;
    
    double acot = Math.toDegrees(Math.atan(slon/((slat * clat)/clatb - slat*clon)));
    
    //
    // We need to adjust the above formula since we use atan
    //
    
    g = Math.toDegrees(g);
    
    if (g * acot < 0.0d) {
        bearing = 360.0d - Math.round(acot);
    } else {
        bearing = 180.0d - Math.round(acot);
    }
    
    //
    // If the two points are on the same longitude we compare the latitudes
    // 
    
    if (g == 0.0d) {
        if (getRadLat() <= p.getRadLat()) {
            bearing = 0.0d;
        } else {
            bearing = 180.0d;
        }
    }

    if (Math.sin (g) > 0.0d) {
        bearing = Math.round(
                             Math.toDegrees(
                                            Math.acos((slatb-slat*Math.cos(dist))/(Math.sin(dist)*clat))));
    } else {
        bearing = Math.round(Math.toDegrees(2.0d*Math.PI-Math.acos((slatb-slat*Math.cos(dist))/(Math.sin(dist)*clat))));
    }
    
    if (bearing >= 360.0d) {
        bearing -= 360.0d;
    }

    return bearing;
  }
  */
}
