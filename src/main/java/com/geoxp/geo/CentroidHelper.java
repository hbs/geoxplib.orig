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

public class CentroidHelper {
  public static final long centroid(long hhcodeA, double weightA, long hhcodeB, double weightB) {
    long a[] = HHCodeHelper.splitHHCode(hhcodeA, 32);
    long b[] = HHCodeHelper.splitHHCode(hhcodeB, 32);
    
    return HHCodeHelper.buildHHCode((long) ((weightA * a[0] + weightB * b[0]) / (weightA + weightB)), (long) ((weightA * a[1] + weightB * b[1]) / (weightA + weightB)), 32);
  }
  
}
