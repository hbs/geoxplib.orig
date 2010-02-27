package com.geocoord.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import sun.security.util.BigInt;

/**
 * Computes centroids at various resolutions given an input file.
 * The input file format MUST be sorted and each line has the format:
 * 
 * HHHHHHHHHHHHHHHH <LF>
 * 
 * where HHHHHHHHHHHHHHHH is the hex representation of a HHCode.
 * 
 * The output has the following format:
 * 
 * HHHHHHHHHHHHHHHH X NNNNNNNNNNNNNNNN CCCCCCCCCCCCCCCC
 * 
 * where HHH... is the cell HHCode
 *       X is the resolution 1 -> F (which is the equivalent of 2 to 30, 32 being ignored)
 *       NNN.... is the total weight at centroid (in hex)
 *       CCC.... is the HHCode of the centroid (in hex)
 *       
 */
public class CentroidGenerator {
  
  private static int minResolution = 2;
  private static int maxResolution = 30;
  
  /**
   * Map of currently computed centroids. Key is the cell for which we compute
   * the centroid. Value is the weight/lat/lon of the current centroid
   */
  private static Map<CharSequence, long[]> centroids = new HashMap<CharSequence, long[]>();
  
  /**
   * Cells currently being treated at resolutions 2->30 (1->15)
   */
  private static CharSequence[] currentCells = new CharSequence[15];
  
  private static StringBuilder sb = new StringBuilder();
  
  private static void updateCentroids(long hhcode) {
    // Convert hhcode to Hex
    sb.setLength(0);
    
    sb.append(Long.toHexString(hhcode));
    
    // Pad with leading 0s
    while(sb.length() < 16) {
      sb.insert(0, "0");
    }
    
    // Loop over the 15 enclosing cells (1 to 15 hex digits)    
    long[] h = HHCodeHelper.splitHHCode(hhcode, 32);

    for (int i = minResolution; i <= maxResolution ; i++) {
      CharSequence cs = sb.subSequence(0, i + 1);
      
      // Check if this CharSequence currently exists
      if (centroids.containsKey(cs)) {
        // It does, update centroid
        long[] values = centroids.get(cs);
        values[1] = values[1] * values[0] + h[0];
        values[2] = values[2] * values[0] + h[1];
        // Update total weight
        values[0]++;
        // Divide by new weight
        values[1] /= values[0];
        values[2] /= values[0];
      } else {
        // It does not, flush the current centroid at resolution 'i'
        // and update it with the new one
        if (null != currentCells[i]) {
          long[] values = centroids.get(currentCells[i]);
          // Output cell / weight / centroid
          System.out.printf("%s %d %x\n", currentCells[i].toString(), values[0], HHCodeHelper.buildHHCode(values[1], values[2], 32));
          centroids.remove(currentCells[i]);
        }
        currentCells[i] = cs;
        centroids.put(cs, new long[] { 1, h[0], h[1] });
      }
    }
  }
  
  private static final void flushCentroids() {
    for (int i = minResolution; i <= maxResolution; i++) {
      if (null != currentCells[i]) {
        long[] values = centroids.get(currentCells[i]);       
        // Output cell / weight / centroid
        System.out.printf("%s %d %x\n", currentCells[i].toString(), values[0], HHCodeHelper.buildHHCode(values[1], values[2], 32));
      }
    }
  }
  
  public static void main(String[] args) throws IOException {
    
    minResolution = Integer.valueOf(args[0]);
    
    if (minResolution % 2 != 0 || minResolution < 2 || minResolution > 30) {
      System.out.println("minResolution MUST be even and between 2 and 30");
    }
    
    maxResolution = Integer.valueOf(args[1]);
    
    if (maxResolution % 2 != 0 || maxResolution < 2 || maxResolution > 30 || maxResolution < minResolution) {
      System.out.println("maxResolution MUST be even and between 2 and 30 and greater or equal to minResolution");
    }

    minResolution >>= 1;
    minResolution -= 1;
    
    maxResolution >>= 1;
    maxResolution -= 1;
    
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    
    long hhcode;
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        break;
      }
      
      try {
        // Convert HHCode to a long
        hhcode = Long.parseLong(line, 16);
      } catch (NumberFormatException nfe) {
        hhcode = new BigInteger(line, 16).longValue();
      }
      // Update centroids
      updateCentroids(hhcode);
    }
    
    // Flush any remaining centroids
    flushCentroids();
  }
}
