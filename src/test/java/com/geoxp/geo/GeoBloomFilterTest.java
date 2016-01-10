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

import org.junit.Assert;
import org.junit.Test;

import com.geoxp.GeoXPLib;

public class GeoBloomFilterTest {
  @Test
  public void testPerf() {
    GeoBloomFilter gbf = new GeoBloomFilter(10, null, null, 6, true);
    
    int n = 100000000;
    
    long nano = System.nanoTime();
    
    for (int i = 0; i < n; i++) {
      double lat = 48.0 + Math.random();
      double lon = -4.55 + Math.random();
      
      long hhcode = GeoXPLib.toGeoXPPoint(lat, lon);
      
      gbf.add(hhcode);
      
      if (0 == i % 10000) {
        long[] cells = GeoXPLib.indexable(hhcode);
        for (long cell: cells) {
          Assert.assertTrue(gbf.contains(cell));
        }
      }
    }
    
    nano = System.nanoTime() - nano;
    
    System.out.println(nano / 1000000.0D);
  }
}
