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

import junit.framework.TestCase;

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
