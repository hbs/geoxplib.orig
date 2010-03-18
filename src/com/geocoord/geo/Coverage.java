package com.geocoord.geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A coverage is a set of zones which cover a given area at the surface of
 * the globe.
 * 
 * A coverage has multiple resolutions (from R=2 the coarsest to R=32 the finest), with
 * steps of 2.
 * The resolution corresponds to the number of bits used to encode both latitude and
 * longitude.
 * 
 * When combined together, lat and lon lead to 2*R bits.
 * 
 * So at resolution R=2, a zone, represented by its HHCode is encoded on 1 hex digit.
 * At R=32, the same zone is encoded on 16 hex digits.
 * 
 * Internally, this class uses resolution r=(R >> 1) - 1
 * 
 * R=32 r=15  R=24 r=11  R=16 r= 7  R=8 r=3
 * R=30 r=14  R=22 r=10  R=14 r= 6  R=6 r=2
 * R=28 r=13  R=20 r= 9  R=12 r= 5  R=4 r=1
 * R=26 r=12  R=18 r= 8  R=10 r= 4  R=2 r=0
 */

public class Coverage {  
  /**
   * HHCode prefix extraction masks for various resolutions
   * (array index is resolution >> 1 - 1).
   */
  public static long[] PREFIX_MASK = new long[16];
  
  private static final String HEXDIGITS = "0123456789abcdef";
  
  static {
    for (int i = 0; i < 16; i++) {
      PREFIX_MASK[i] = 0xffffffffffffffffL << (60 - i * 4);
    }
  }
  
  private Set<Long>[] coverage = new HashSet[16];
  
  private Set<Integer> resolutions = new HashSet<Integer>();
  
  /**
   * Return a set of all resolutions in which this coverage has
   * cells.
   * 
   * @return The set of resolutions in this coverage
   */
  public Set<Integer> getResolutions() {
    for (int r = 0; r < 16; r++) {
      if (null == coverage[r] || coverage[r].isEmpty()) {
        this.resolutions.remove((r + 1) << 1);
      }
    }
    return this.resolutions;
  }
  
  /**
   * Return the cells at a given resolution.
   * 
   * @param resolution Resolution for which to return the cells (even in [2,32])
   * @return
   */
  public Set<Long> getCells(int resolution) {
    int r = (resolution >> 1) - 1;
    
    // Do nothing if resolution out of range
    if (0 != (r & 0xfffffff0)) {
      return null;
    }
    
    return internalGetCells(r);
  }
  
  private Set<Long> internalGetCells(int r) {
    // FIXME(hbs): this is not synchronized
    if (null == coverage[r]) {
      coverage[r] = new HashSet<Long>();
    }
    
    return coverage[r];
  }
  
  /**
   * Add a cell at a given resolution
   * 
   * @param resolution Resolution (even in [2,32])
   * @param hhcode HHCode of cell to add.
   */
  public void addCell(int resolution, long hhcode) {
    int r = (resolution >> 1) - 1;
    
    // Do nothing if resolution out of range
    if (0 != (r & 0xfffffff0)) {
      return;
    }
    
    // Add prefix of hhcode
    internalGetCells(r).add(hhcode & PREFIX_MASK[r]);
    // Add r to the set of resolutions
    // FIXME(hbs): This call is not synchronized...
    this.resolutions.add(resolution);
  }
  
  /**
   * Remove a cell at a given resolution.
   * 
   * @param resolution Resolutionn (even in [2,32])
   * @param hhcode HHCode of cell to remove
   */
  public void removeCell(int resolution, long hhcode) {
    int r = (resolution >> 1) - 1;
    
    // Do nothing if resolution out of range
    if (0 != (r & 0xfffffff0)) {
      return;
    }
    
    // Remove prefix of hhcode
    internalGetCells(r).remove(hhcode & PREFIX_MASK[r]);    
  }
  
  /**
   * Convert a coverage into a String of cells
   * 
   * @param separator Use this String to separate cells
   */
  public String toString(String separator) {
    StringBuilder sb = new StringBuilder();
    StringBuilder hhsb = new StringBuilder();
    
    for (int i = 0; i < 16; i++) {
      if (null == coverage[i]) {
        continue;
      }
  
      for (long hhcode: coverage[i]) {
        
        hhsb.setLength(0);
        hhsb.append(Long.toHexString(hhcode));
        
        // Pad with 0 on the left
        while (hhsb.length() < 16) {
          hhsb.insert(0, "0");
        }

        if (sb.length() > 0) {
          sb.append(separator);
        }
        
        sb.append(hhsb.subSequence(0, i + 1));
      }
    }
    
    return sb.toString();
  }
  
  public String toString() {
    return this.toString(" ");
  }
  
  /**
   * Optimize a coverage.
   * Cells at resolution R are merged into the enclosing cell at R-1 if the number of those cells reaches
   * the given threshold.
   * 
   * @param thresholds A long containing the thresholds for each resolution. Each threshold is on 4 bits, with 0 meaning 16.
   *                   Threshold for R=2 is on bits 63-60, R=2 on 59-56 ... R=32 on 3-0
   * @param minresolution Resolution below which no optimization will be done.
   */
  public Coverage optimize(long thresholds, int minresolution) {
    
    minresolution = (minresolution >> 1) - 1;
    
    for (int r = 15; r > minresolution; r--) {
      if (null == coverage[r]) {
        continue;   
      }
      
      // Extract threshold for this resolution
      long threshold = (thresholds >> (4 * (15 - r))) & 0xfL;
      
      // Sort the cells at resolution 'r'
      List<Long> sortedCells = new ArrayList<Long>();
      sortedCells.addAll(coverage[r]);
      Collections.sort(sortedCells);
      
      // Flag indicating the first cell
      boolean first = true;
      
      // Parent cell at R-2
      long parentCell = 0L;
      long lastParent = 0L;
      
      // Number of child cells of 'parentCell'
      int children = 0;
      
      // Loop over the cells
      int count = sortedCells.size();

      for (long hhcode: sortedCells) {
        count--;
        // Compute parent cell at r-1
        parentCell = hhcode & PREFIX_MASK[r - 1];

        if (first) {
          lastParent = parentCell;
          children = 1;
        }
        
        first = false;
        
        // The parent cell just changed or we reached the end of the cells, decide if we should include the current
        // parent or not.
        
        if (lastParent != parentCell || 0 == count) {
          if ((threshold > 0 && children >= threshold) || children == 16) {
            // Add parent cell at r - 1
            internalGetCells(r - 1).add(lastParent);
            Set<Long> s = internalGetCells(r);
            // Remove child cells at r
            for (long offset = 0L; offset < 16L; offset++) {
              s.remove(lastParent | (offset << (4 * (15 -r))));
            }
          }
          children = 1;
        } else {
          children++;
        }
      }      
    }
        
    //
    // Now check that no cell at r covers a cell a r+2, this can happen when clustering cells
    // at r+1.
    // E.g r+2 = { 000 }
    //     r+1 = { 01 02 03 }
    //     r = {}
    // Clustering at r+1 with a threshold of 3 will lead to cell '0' being included at r, so we end up with
    //     r+2 = { 000 }
    //     r+1 = {} (merged)
    //     r = {0} which covers 000
    //
    // So we need to get rid of 000
    //
    
    for (int r = 0; r < 14; r++) {
      Set<Long> sr = internalGetCells(r);
      Set<Long> sr2 = internalGetCells(r+2);
      
      if(sr.isEmpty() || sr2.isEmpty()) {
        continue;
      }
      
      Long[] cells = sr2.toArray(new Long[0]);
      
      for (long hhcode: cells) {
        if (sr.contains(hhcode & PREFIX_MASK[r])) {
          sr2.remove(hhcode);
        }
      }
    }
    
    return this;
  }
  
  public void optimize(long thresholds) {
    optimize(thresholds, HHCodeHelper.MIN_RESOLUTION);
  }
  
  /**
   * Merge another coverage with this one.
   * 
   * @param other Other coverage to merge.
   */
  public void merge(Coverage other) {
    for (int r = 0; r < 16; r++) {
      Set<Long> cells = this.internalGetCells(r);
      Set<Long> otherCells = other.internalGetCells(r);      
      cells.addAll(otherCells);
    }
    this.resolutions.addAll(other.getResolutions());
  }
  
  public long area() {    
    long area = 0L;
    
    for (int i = 0; i < 16; i++) {
      if (null == coverage[i]) {
        continue;
      }
      area += coverage[i].size() * (0x1L << (60 - 4*(i)));
    }
    
    // Return the result, shifted one bit to the right to make sure it is > 0.
    // This will have the side effect of having an area of 0 for a coverage of 1 HHCode at resolution 32.
    return (area >> 1) & 0x7fffffffffffffffL;
  }
  
  /**
   * Returns the coarsest resolution containing a cell which contains a hhcode.
   * 
   * @param hhcode HHCode to test.
   * @return The resolution or 0 if no cell in the Coverage contains the HHCode.
   */
  public int getCoarsestResolution(long hhcode) {
    for (int r = 0; r < 16; r++) {
      if (null == coverage[r]) {
        continue;
      }
      if (coverage[r].contains(hhcode & PREFIX_MASK[r])) {
        return (r + 1) << 1;
      }
    }
    return 0;
  }

  /**
   * Returns the finest resolution containing a cell which contains a hhcode.
   * 
   * @param hhcode HHCode to test.
   * @return The resolution or 0 if no cell in the Coverage contains the HHCode.
   */
  public int getFinestResolution(long hhcode) {
    for (int r = 15; r >= 0; r--) {
      if (null == coverage[r]) {
        continue;
      }
      if (coverage[r].contains(hhcode & PREFIX_MASK[r])) {
        return (r + 1) << 1;
      }
    }
    return 0;
  }

  /**
   * Normalize a coverage so it only contains cells at the given resolution.
   * For each cell at a lower resolution than the target one, replace it with
   * the 16 cells at the next higher resolution. Proceed until there are no
   * cells at any resolution lower than the target one.
   * 
   * For resolutions finer than the target one, apply optimization with a threshold
   * of 1.
   * 
   * The end result
   * @param resolution The resolution to normalize the coverage to.
   */
  public void normalize(int resolution) {
    
    // Convert resolution to internal value
    resolution = (resolution >> 1) - 1;
    
    //
    // Handle the coarser resolutions
    //
    
    long[] masks = new long[16];
    
    //
    // Loop over all resolutions above the target one
    //
    
    for (int r = 0; r < resolution; r++) {
      
      Set<Long> cells = internalGetCells(r);
      
      if (cells.isEmpty()) {
        continue;
      }
      
      // Compute target internal res for adding 16 cells per cell found at 'r'
      int res = (r + 1 + 1) << 1;
    
      //
      // Compute masks to add to next finer resolution
      //
      // i.e.  0xa000000000000000 will lead to
      //       0xa000000000000000
      //       0xa100000000000000
      //       0xa200000000000000
      //       ....
      //       0xaf00000000000000
      //
      
      for (int i = 1; i < 16; i++) {
        masks[i] = ((long) i) << (56 - r * 4);
      }
      
      //
      // Add 16 cells for each cell found.
      //
      
      for (long hhcode: cells) {
        addCell(res, hhcode);
        for (int i = 1; i < 16; i++) {
          addCell(res, hhcode|masks[i]);
        }
      }
      
      //
      // Clear cells at the current resolution
      //
      
      cells.clear();
      resolutions.remove((r + 1) << 1);
    }
    
    //
    // Optimize the smallest resolutions
    //
    
    long thresholds = 0x0111111111111111L >> (4 * resolution);
        
    optimize(thresholds, (resolution + 1) << 1);
    
    // Now if everything went well, we only have cells at 'resolution'
  }
  
  /**
   * Clone this coverage.
   * 
   * @return A clone of the current coverage.
   */
  public Coverage deepCopy() {
    Coverage clone = new Coverage();
    clone.resolutions.addAll(this.resolutions);
    for (int r = 0; r < 16; r++) {
      if (null != coverage[r] && !coverage[r].isEmpty()) {
        clone.coverage[r] = new HashSet<Long>();
        clone.coverage[r].addAll(coverage[r]);
      }
    }
    
    return clone;
  }
}
