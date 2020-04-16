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

public class CentroidHelper {
  public static final long centroid(long hhcodeA, double weightA, long hhcodeB, double weightB) {
    long a[] = HHCodeHelper.splitHHCode(hhcodeA, 32);
    long b[] = HHCodeHelper.splitHHCode(hhcodeB, 32);
    
    return HHCodeHelper.buildHHCode((long) ((weightA * a[0] + weightB * b[0]) / (weightA + weightB)), (long) ((weightA * a[1] + weightB * b[1]) / (weightA + weightB)), 32);
  }
  
}
