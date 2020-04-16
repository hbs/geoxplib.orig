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

import org.junit.Test;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public class JTSHelperTest {
  @Test
  public void testCoverGeometry() throws Exception {
    
    String WKT = "POLYGON((-4.5 48.0, -4.5 49.0, -4.0 48.0, -4.5 48.0))";
    
    WKTReader reader = new WKTReader();
    
    long nano = System.nanoTime();
    Geometry geometry = null;
    int n = 10000;
    
    for (int i = 0; i < n; i++) {
      geometry = reader.read(WKT);
    }
    nano = System.nanoTime() - nano;
    System.out.println(nano / n / 1000000.0D);
    
    Coverage coverage = null;
    
    nano = System.nanoTime();

    for (int i = 0; i < n; i++) {
      coverage = JTSHelper.coverGeometry(geometry, 14, 14, false);
    }
    nano = System.nanoTime() - nano;
    System.out.println(nano / n / 1000000.0D);
    
    System.out.println(coverage.getCellCount());
    System.out.println(coverage.getResolutions());
    System.out.println(coverage);
  }
}
