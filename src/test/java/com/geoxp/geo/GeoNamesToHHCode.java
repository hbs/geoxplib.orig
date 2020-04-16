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

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Output HHCodes of GeoNames database.
 */
public class GeoNamesToHHCode {

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        break;
      }

      if (line.startsWith("RC")) {
        continue;
      }
      
      String[] tokens = line.split("\\t");
            
      long hhcode = HHCodeHelper.getHHCodeValue(Double.valueOf(tokens[3]), Double.valueOf(tokens[4]));
      System.out.printf("%016x\n", hhcode);
    }

  }

}
