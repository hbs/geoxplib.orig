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
