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
