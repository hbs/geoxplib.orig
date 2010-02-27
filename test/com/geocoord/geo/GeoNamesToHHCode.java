package com.geocoord.geo;

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
