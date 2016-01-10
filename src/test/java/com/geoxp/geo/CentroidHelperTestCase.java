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

import junit.framework.TestCase;

public class CentroidHelperTestCase extends TestCase {
  public void testCentroid_TwoPoints() {
    long a = HHCodeHelper.buildHHCode(0, 0, 32);
    long b = HHCodeHelper.buildHHCode(0xffffffffL, 0xffffffffL, 32);
    long c = CentroidHelper.centroid(a, 1, b, 1);
    
    assertEquals(0x3fffffffffffffffL, c);
    
    c = CentroidHelper.centroid(a, 2, b, 1);
    
    assertEquals(0x3333333333333333L, c);

    c = CentroidHelper.centroid(a, 4, b, 1);
    
    assertEquals(0x0f0f0f0f0f0f0f0fL, c);
    
    a = HHCodeHelper.buildHHCode(0, 0x7fffffffL, 32);
    b = HHCodeHelper.buildHHCode(0xffffffffL, 0x7fffffffL, 32);
    
    c = CentroidHelper.centroid(a,1,b,1);
    
    assertEquals(0x3fffffffffffffffL, c);

    a = HHCodeHelper.buildHHCode(0, 0x7fffffffL, 32);
    b = HHCodeHelper.buildHHCode(0x7fffffffL, 0xffffffffL, 32);

    c = CentroidHelper.centroid(a,1,b,1);

    assertEquals(0x4fffffffffffffffL, c);

    a = HHCodeHelper.buildHHCode(0xffffffffL, 0L, 32);
    b = HHCodeHelper.buildHHCode(0, 0xffffffffL, 32);

    c = CentroidHelper.centroid(a,1,b,1);

    assertEquals(0x3fffffffffffffffL, c);
  }
}
