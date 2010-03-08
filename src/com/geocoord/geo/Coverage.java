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
   */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    
    for (int i = 0; i < 16; i++) {
      if (null == coverage[i]) {
        continue;
      }
      
      for (long hhcode: coverage[i]) {
        for (int j = 60; j >= (60 - 4 *i); j -= 4) {
          sb.append(Long.toHexString((hhcode >> j) & 0xfL));
        }
        sb.append(" ");
      }
    }
    
    // Trim trailing WSP.
    if (sb.length() > 0) {
      sb.setLength(sb.length() - 1);
    }
    
    return sb.toString();
  }
  
  /**
   * Optimize a coverage.
   * Cells at resolution R are merged into the enclosing cell at R-1 if the number of those cells reaches
   * the given threshold.
   * 
   * @param thresholds A long containing the thresholds for each resolution. Each threshold is on 4 bits, with 0 meaning 16.
   *                   Threshold for R=2 is on bits 63-60, R=2 on 59-56 ... R=32 on 3-0
   */
  public Coverage optimize(long thresholds) {
    for (int r = 15; r > 0; r--) {
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
}
