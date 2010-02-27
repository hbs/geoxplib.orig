package com.geocoord.geo;

public class CentroidHelper {
  public static final long centroid(long hhcodeA, double weightA, long hhcodeB, double weightB) {
    long a[] = HHCodeHelper.splitHHCode(hhcodeA, 32);
    long b[] = HHCodeHelper.splitHHCode(hhcodeB, 32);
    
    return HHCodeHelper.buildHHCode((long) ((weightA * a[0] + weightB * b[0]) / (weightA + weightB)), (long) ((weightA * a[1] + weightB * b[1]) / (weightA + weightB)), 32);
  }
  
}
