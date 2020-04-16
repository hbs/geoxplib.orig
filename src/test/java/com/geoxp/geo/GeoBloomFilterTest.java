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

import org.junit.Assert;
import org.junit.Test;

import com.geoxp.GeoXPLib;

public class GeoBloomFilterTest {
  @Test
  public void notestPerf() {
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
