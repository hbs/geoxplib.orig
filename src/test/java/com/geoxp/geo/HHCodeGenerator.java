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

import java.security.SecureRandom;

public class HHCodeGenerator {
  public static void main(String[] args) throws Exception {
    
    long count = Long.valueOf(args[0]);
    
    SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
    
    StringBuilder sb = new StringBuilder();
    
    for (int i = 0; i < count; i++) {
      sb.setLength(0);
      sb.append(Long.toHexString(sr.nextLong()));
      while(sb.length() < 16) {
        sb.insert(0, "0");
      }
      System.out.println(sb.toString());
    }
  }
}
