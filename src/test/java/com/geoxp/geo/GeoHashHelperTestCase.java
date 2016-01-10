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

import org.junit.Assert;
import org.junit.Test;

public class GeoHashHelperTestCase extends TestCase {
  
  /**
   * Check with the examples from http://en.wikipedia.org/wiki/Geohash
   */
  @Test
  public void testToHHCode() {
    long hhcode = GeoHashHelper.toHHCode("ezs42");
    
    Assert.assertEquals(-6921889690756841472L, hhcode);
    
    hhcode = GeoHashHelper.toHHCode("u4pruydqqvj");
    Assert.assertEquals(-2155044206010576640L, hhcode);
    
    hhcode = GeoHashHelper.toHHCode("gbsc07bczzpf");
    Assert.assertEquals("b570702e87ffd5d0", HHCodeHelper.toString(hhcode));
  }
  
  public void testFromHHCode() {
    String geohash = GeoHashHelper.fromHHCode(-6921889690756841472L, 15);    
    Assert.assertEquals("ezs420", geohash);
    
    geohash = GeoHashHelper.fromHHCode(0xb570702e87ffd5d0L, 32);
    Assert.assertEquals("gbsc07bczzpf", geohash);
  }
}
