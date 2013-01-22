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
