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
