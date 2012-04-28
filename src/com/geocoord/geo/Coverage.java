package com.geocoord.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  
  /**
   * Maximum difference in resolutions when determining a resolution
   * to normalize coverages in minus/intersection.
   */
  public static final int MAX_RES_DIFF = 4;
  
  static {
    for (int i = 0; i < 16; i++) {
      PREFIX_MASK[i] = 0xffffffffffffffffL << (60 - i * 4);
    }
  }
  
  private Set<Long>[] coverage = new HashSet[16];
  
  private Set<Integer> resolutions = new HashSet<Integer>();
  
  public Coverage() {
    
  }
  
  public Coverage(Map<Integer,Set<Long>> c) {
    for (int i = 0; i < HHCodeHelper.MAX_RESOLUTION; i++) {
      int r = (i >> 1) - 1;
      if (c.containsKey(i)) {
        coverage[r] = new HashSet<Long>();
        coverage[r].addAll(c.get(i));
      }
    }
  }
  
  /**
   * Return a set of all resolutions in which this coverage has
   * cells.
   * 
   * @return The set of resolutions in this coverage
   */
  public Set<Integer> getResolutions() {
    this.resolutions.clear();
    
    for (int r = 0; r < 16; r++) {
      if (null != coverage[r] && !coverage[r].isEmpty()) {
        this.resolutions.add((r + 1) << 1);
      }
    }
    return this.resolutions;
  }
  
  /**
   * Compute cell cardinalities of the coverage.
   * 
   * @return a map of cardinalities (number of cells) by resolution.
   */
  public Map<Integer,Integer> getCardinalities() {
    
    Map<Integer,Integer> cardinalities = new HashMap<Integer, Integer>();
    
    for (int r = 0; r < 16; r++) {
      if (null != coverage[r] && coverage[r].isEmpty()) {
        cardinalities.put((r + 1) << 1, coverage[r].size());
      }
    }

    return cardinalities;
  }
  
  /**
   * Return the number of cells at the given resolution.
   * 
   * @param resolution Resolution to return the count of cells for.
   * @return The number of cells at the given resolution.
   */
  public int getCellCount(int resolution) {
    int r = (resolution >> 1) - 1;
    
    // Do nothing if resolution out of range
    if (0 != (r & 0xfffffff0)) {
      return 0;
    }
    
    return internalGetCells(r).size();    
  }
  
  /**
   * Return the total number of cells in the coverage.
   * 
   * @return
   */
  public int getCellCount() {
    
    int count = 0;
    
    for (int r = 0; r < 16; r++) {
      if (null != coverage[r]) {
        count += coverage[r].size();
      }
    }
    
    return count;
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
  
  /**
   * Return a copy of the coverage's cells.
   * 
   * @return
   */
  public Map<Integer,Set<Long>> getAllCells() {
    Map<Integer,Set<Long>> cells = new HashMap<Integer, Set<Long>>();
    
    for (int r = 0; r < 16; r++) {
      if (null != coverage[r] && !coverage[r].isEmpty()) {
        Set<Long> rcells = new HashSet<Long>();
        rcells.addAll(coverage[r]);
        cells.put((r + 1) << 1, rcells);
      }
    }
    
    return cells;
  }
  
  private Set<Long> internalGetCells(int r) {
    // FIXME(hbs): this is not synchronized
    if (null == coverage[r]) {
      coverage[r] = new HashSet<Long>();
    }
    
    return coverage[r];
  }
  
  /**
   * Add a cell at the given lat/lon and resolution
   */
  public void addCell(int resolution, long lat, long lon, long[] geocells, boolean excludeGeoCells) {
    //
    // Make sure lat/lon are in the 0->2**32-1 range
    //
    
    lat = ((lat % (1L << HHCodeHelper.MAX_RESOLUTION)) + (1L << HHCodeHelper.MAX_RESOLUTION)) % (1L << HHCodeHelper.MAX_RESOLUTION);
    lon = ((lon % (1L << HHCodeHelper.MAX_RESOLUTION)) + (1L << HHCodeHelper.MAX_RESOLUTION)) % (1L << HHCodeHelper.MAX_RESOLUTION);
    
    addCell(resolution, HHCodeHelper.buildHHCode(lat, lon, HHCodeHelper.MAX_RESOLUTION), geocells, excludeGeoCells);
  }
  
  public void addCell(int resolution, long lat, long lon) {
    addCell(resolution,lat,lon,null,false);
  }
  
  /**
   * Add a cell at a given resolution
   * 
   * @param resolution Resolution (even in [2,32])
   * @param hhcode HHCode of cell to add.
   */
  public void addCell(int resolution, long hhcode, long[] geocells, boolean excludeGeoCells) {
    int r = (resolution >> 1) - 1;
    
    // Do nothing if resolution out of range
    if (0 != (r & 0xfffffff0)) {
      return;
    }
    
    //
    // Set lower bits to 0
    //
    
    hhcode = hhcode & PREFIX_MASK[r];
    
    //
    // If geocells is not null, check if hhcode is included/excluded
    // TODO(hbs): this won't work if geocells has a finer resolution
    // than 'resolution'. This won't be fixed though...
    //
        
    if (null != geocells) {
      boolean ingeocells = contains(geocells, hhcode, 2, resolution);
      
      // If hhcode is included when we should exclude geocells or
      // excluded when we should intersect, return without adding the
      // cell to the coverage
      if ((ingeocells && excludeGeoCells) || (!ingeocells && !excludeGeoCells)) {
        return;
      }      
    }
 
    // Add prefix of hhcode
    internalGetCells(r).add(hhcode);

    // Add r to the set of resolutions
    // FIXME(hbs): This call is not synchronized...
    this.resolutions.add(resolution);
  }
  
  public void addCell(int resolution, long hhcode) {
    addCell(resolution, hhcode, null, false);
  }
  
  public boolean contains(int resolution, long hhcode) {
    int r = (resolution >> 1) - 1;
    
    // Do nothing if resolution out of range
    if (0 != (r & 0xfffffff0)) {
      return false;
    }
    
    if (internalGetCells(r).isEmpty() || !internalGetCells(r).contains(hhcode & PREFIX_MASK[r])) {
      return false;
    }
    
    return true;
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
    
  public String toString(String separator) {
    return toString(separator, "");
  }
  
  /**
   * Convert a coverage into a String of cells
   * 
   * @param separator Use this String to separate cells
   * @param prefix Prefix to prepend to each cell
   */
  public String toString(String separator, String prefix) {
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
        
        sb.append(prefix);
        sb.append(hhsb.subSequence(0, i + 1));
      }
    }
    
    return sb.toString();
  }
  
  public String toString() {
    return this.toString(" ");
  }
  
  /**
   * Convert the Coverage into a list of GeoCells.
   * 
   * @param finestresolution The finest resolution (even, 2 -> 30) to include.
   * @return
   */
  public long[] toGeoCells(int finestresolution) {
    
    if (finestresolution > 30) {
      finestresolution = 30;
    }
    
    //
    // Count cells up to the given resolution
    //
    
    int count = 0;
    for (int i = 0; i < finestresolution >> 1; i++) {
      if (null != coverage[i]) {
        count += coverage[i].size();
      }
    }
    
    long[] geocells = new long[count];
    
    // Generate geocells
    int idx = 0;
    
    //
    // We scan the resolutions from 8 to 14 (which lead to negative longs)
    // then from 0 to 7
    //

    for (int i = 7; i < finestresolution >> 1; i++) {
      if (null != coverage[i] && !coverage[i].isEmpty()) {
        int startidx = idx;
        for (Long hhcode: coverage[i]) {
          // INFO(hbs): we do not AND the lowest bits because they have already been cleared
          //            when building the Coverage.
          geocells[idx] = ((long) (i + 1)) << 60;
          geocells[idx] |= (hhcode >> 4) & 0x0fffffffffffffffL;
          idx++;
        }
        //
        // Sort the cells at the current resolution
        //
        Arrays.sort(geocells, startidx, idx);
      }      
    }
    
    for (int i = 0; i < 7; i++) {
      if (i >= finestresolution >> 1) {
        break;
      }
      if (null != coverage[i] && !coverage[i].isEmpty()) {
        int startidx = idx;
        for (Long hhcode: coverage[i]) {
          // INFO(hbs): we do not AND the lowest bits because they have already been cleared
          //            when building the Coverage.
          geocells[idx] = ((long) (i + 1)) << 60;
          geocells[idx] |= (hhcode >> 4) & 0x0fffffffffffffffL;
          idx++;
        }
        //
        // Sort the cells at the current resolution
        //
        Arrays.sort(geocells, startidx, idx);
      }
    }

    return geocells;
  }
    
  /**
   * Checks whether a given hhcode is present in a GeoCell coverage
   * at a given resolution
   * 
   * @param geocells Array of geocells to check against
   * @param resolution Resolution at which to check (2-30)
   * @param hhcode HHCode to check
   * @return true or false depending on containment
   */
  public static boolean contains(long[] geocells, int resolution, long hhcode) {
    // Compute first/last geocell values at the given resolution
    
    long firstval = ((long) (resolution >> 1)) << 60;
    long lastval = (long) 0xffffffffffffffffL << (60 - (resolution << 1));
    lastval &= 0x0fffffffffffffffL;
    lastval |= firstval;
    
    //
    // Perform a binary search to check if the resolution exists in the geocells
    //
    
    /*
    int firstidx = Arrays.binarySearch(geocells, firstval);
    int lastidx = Arrays.binarySearch(geocells, lastval);

    if (firstidx == lastidx) {
      return false;
    }
    
    if (firstidx < 0) {
      firstidx = -1 - firstidx;
    }
    
    if (lastidx < 0) {
      lastidx = -1 - lastidx;
    }
    */
    
    long key = (((hhcode >> 4) & 0x0fffffffffffffffL) & lastval) | firstval;
    
    //int idx = Arrays.binarySearch(geocells, firstidx, lastidx, key);
    int idx = Arrays.binarySearch(geocells, key);
    
    return idx >= 0;
  }
  
  public static boolean contains(long[] geocells, long hhcode) {
    for (int i = 2; i < 32; i += 2) {
      if (contains(geocells, i, hhcode)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Check whether an array of geocells contains a given HHCode at a resolution
   * contained between bounds.
   * 
   * @param geocells
   * @param hhcode
   * @param lowerResolution
   * @param upperResolution
   * @return
   */
  public static boolean contains(long[] geocells, long hhcode, int lowerResolution, int upperResolution) {
    if (lowerResolution < 2) {
      lowerResolution = 2;
    }
    if (upperResolution > 30) {
      upperResolution = 30;
    }    
    for (int i = lowerResolution; i <= upperResolution; i += 2) {
      if (contains(geocells, i, hhcode)) {
        return true;
      }
    }
    return false;    
  }
  
  /**
   * Optimize a coverage.
   * Cells at resolution R are merged into the enclosing cell at R-1 if the number of those cells reaches
   * the given threshold.
   * 
   * @param thresholds A long containing the thresholds for each resolution. Each threshold is on 4 bits, with 0 meaning 16.
   *                   Threshold for R=2 is on bits 63-60, R=4 on 59-56 ... R=32 on 3-0
   * @param minresolution Resolution at or below which no optimization will be done.
   * @param count Stop optimizing when the cell count reaches count
   */
  public Coverage optimize(long thresholds, int minresolution, int cellcount) {
    
    minresolution = (minresolution >> 1) - 1;
    
    int totalcells = 0;
    
    if (0 != cellcount) {
      totalcells = getCellCount();
      if (totalcells <= cellcount) {
        return this;
      }
    }
    
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
          // We need to have children set to 1 so we can handle the case when there is only 1 cell left
          children = 0;
        }
        
        first = false;
        
        // The parent cell just changed or we reached the end of the cells, decide if we should include the current
        // parent or not.
        
        if (lastParent != parentCell || 0 == count) {
          if (0 == count) {
            children++;
          }
          if ((threshold > 0 && children >= threshold) || children == 16) {
            // Add parent cell at r - 1
            internalGetCells(r - 1).add(lastParent);
            totalcells++;

            //
            // We remove all found children.
            // Intuitively you could think we could remove each child one after the other and stop when the
            // desired cell count is reached without adding the parent cell, but that's
            // not the case, because doing so would lead to a coverage which does not include the
            // original one. So counter intuitively we remove all children.
            // If this is not the last cell, we need to prevent an off by one error.
            //
            totalcells -= (0 == threshold ? 16 : (0 == count ? children : children - 1));
            
            Set<Long> s = internalGetCells(r);
            int cardinality = s.size();
            
            // Remove child cells at r
            for (long offset = 0L; offset < 16L; offset++) {              
              s.remove(lastParent | (offset << (4 * (15 -r))));
            }
            // Exit if we reached the desired number of cells
            if (cellcount > 0 && totalcells <= cellcount) {
              break;
            }
          }
          lastParent = parentCell;
          children = 1;        
        } else {
          children++;
        }
      }

      if (cellcount > 0 && totalcells <= cellcount) {
        break;
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
    optimize(thresholds, HHCodeHelper.MIN_RESOLUTION, 0);
  }

  public void optimize(long thresholds, int minresolution) {
    optimize(thresholds, minresolution, 0);
  }

  /**
   * Prune a coverage
   * Cells at resolution R are kept only if the enclosing cell at R-1 has more (strictly) subcells than a thrshold
   * 
   * @param thresholds A long containing the thresholds for each resolution. Each threshold is on 4 bits,
   *                   Threshold for R=2 is on bits 63-60, R=4 on 59-56 ... R=32 on 3-0
   * @param minresolution Resolution at or below which no optimization will be done.
   * @param count Stop optimizing when the cell count reaches count
   */
  public Coverage prune(long thresholds, int minresolution, int cellcount) {
    
    minresolution = (minresolution >> 1) - 1;
    
    int totalcells = 0;
    
    if (0 != cellcount) {
      totalcells = getCellCount();
      if (totalcells <= cellcount) {
        return this;
      }
    }
    
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
          // We need to have children set to 1 so we can handle the case when there is only 1 cell left
          children = 0;
        }
        
        first = false;
        
        // The parent cell just changed or we reached the end of the cells, decide if we should keep the
        // current children or not.
        
        if (lastParent != parentCell || 0 == count) {
          if (0 == count) {
            children++;
          }
          if (children <= threshold) {
            //
            // We remove all found children since there are not enough of them
            //
            totalcells -= children;
            
            Set<Long> s = internalGetCells(r);
            int cardinality = s.size();
            
            // Remove child cells at r
            for (long offset = 0L; offset < 16L; offset++) {              
              s.remove(lastParent | (offset << (4 * (15 -r))));
            }
            // Exit if we reached the desired number of cells
            if (cellcount > 0 && totalcells <= cellcount) {
              break;
            }
          }
          lastParent = parentCell;
          children = 1;        
        } else {
          children++;
        }
      }

      if (cellcount > 0 && totalcells <= cellcount) {
        break;
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
   * Return the finest resolution at which there are cells.
   * @return The finest resolution or 0 if the coverage is empty.
   */
  public int getFinestResolution() {
    for (int r = 15; r >= 0; r--) {
      if (null != coverage[r] && !coverage[r].isEmpty()) {
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
    // Loop over all resolutions above the target one
    //
    
    for (int r = 0; r < resolution; r++) {
      
      Set<Long> cells = internalGetCells(r);
      
      if (cells.isEmpty()) {
        continue;
      }
      
      //
      // Compute the last mask we will need to add
      //
      // If expanding cell 0xf000000000000000L (at R=2) to R=6,
      // we need to add 0x00000000000000000L
      //                0x00100000000000000L
      //                0x00200000000000000L
      //                ...
      //                0x0ff00000000000000L
      // to the cell HHCode in order to generate all the children cells at the
      // target R (R=6)
      //
      
      long lastmask = 1 << (4 * (resolution - r));
      
      for (long l = 0; l < lastmask; l++) {
        long mask = l << (4 * (15 - resolution));

        // Generate (grand)^(resolution-r) children cells
        for (long hhcode: cells) {
          internalGetCells(resolution).add((hhcode|mask) & PREFIX_MASK[resolution]);          
        }
      }
      
      //
      // Clear cells at the current resolution
      //
      
      cells.clear();
      resolutions.remove((r + 1) << 1);
    }

    // Record the target resolution.
    resolutions.add((resolution + 1) << 1);
    
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
  
  /**
   * Return the approximate mean resolution of the coverage.
   * 
   * @return The computed mean, or 0 if the coverage has no cells.
   */
  public int getMeanResolution() {
    
    long count = getCellCount();
    
    if (0 == count) {
      return 0;
    }
    
    long avgArea = ((long) (area() / count)) << 1;
    
    if (0 == avgArea) {
      return 32;
    }
    
    return ((int) (64 - Math.log(avgArea) / Math.log(2)) >> 1) & 0x3e;    
  }
  
  /**
   * Will compute A\B (A-B, A minus B)
   * 
   * @param a
   * @param b
   * @return The difference A-B, A and B left untouched.
   */
  public static Coverage minus_normalize(Coverage a, Coverage b) {

    //
    // Clone coverages.
    //
    
    a = a.deepCopy();
    b = b.deepCopy();
    
    int meanA = a.getMeanResolution();
    int meanB = b.getMeanResolution();

    //
    // Try not to lower the resolution of b. We won't if meanB > meanA and meanB - meanA <= MAX_RES_DIFF.
    // Otherwise we'll normalize both coverages to meanA + MAX_RES_DIFF.
    // A resolution delta of 4 means a ratio of 1/256 between resolutions, it also means that
    // many more cells (256x) in the normalized coverage.
    // FIXME(hbs): Note that since we computed a mean, we might very well hit edge cases
    //             with catastrophic cell multiplication. Let's call those cancerous cells...
    //
    
    int normRes = meanB;
    
    if (meanB < meanA || (meanB - meanA) > MAX_RES_DIFF) {
      normRes = meanA + MAX_RES_DIFF;
    }
    
    a.normalize(normRes);
    b.normalize(normRes);
    
    for (long hhcode: b.getCells(normRes)) {
      a.removeCell(normRes, hhcode);
    }
    
    return a;
  }

  public static Coverage minus(Coverage a, Coverage b) {

    //
    // Clone coverages.
    //
    
    a = a.deepCopy();
    b = b.deepCopy();
    
    //
    // Optimize clones
    //
    
    a.optimize(0L);
    b.optimize(0L);
    
    //
    // Normalize them
    //
    
    Coverage.normalize(a, b);

    for (int r = 2; r <= 32; r+=2) {
      for (long hhcode: b.getCells(r)) {
        a.removeCell(r, hhcode);
      }
    }
    
    return a;
  }

  /**
   * Compute the intersection of two coverages.
   * 
   * @param a
   * @param b
   * @return A new coverage that is the intersection of A and B. A and B left untouched.
   */
  public static Coverage intersection_normalize(Coverage a, Coverage b) {
    
    //
    // If one of the coverages is empty, return an empty coverage
    //
    
    if (0 == a.getCellCount() || 0 == b.getCellCount()) {
      return new Coverage();
    }
    
    //
    // Clone coverages.
    //
    
    a = a.deepCopy();
    b = b.deepCopy();
    
    int meanA = a.getMeanResolution();
    int meanB = b.getMeanResolution();

    //
    // If the difference in mean resolution is more than MAX_RES_DIFF,
    // normalize to the highest resolution + MAX_RES_DIFF.
    //
    
    int normRes = 0;
    
    if (Math.abs(meanA - meanB) > MAX_RES_DIFF) {
      normRes = Math.min(meanA, meanB) + MAX_RES_DIFF;
    } else {
      normRes = Math.max(meanA, meanB);
    }
    
    a.normalize(normRes);
    b.normalize(normRes);
    
    //
    // Now loop over cells in the coverage with the least
    // and only keep the ones that are in both.
    //
    
    Set<Long> cellsA = a.getCells(normRes);
    Set<Long> cellsB = b.getCells(normRes);
    
    Coverage c = new Coverage();
    
    if (cellsA.size() < cellsB.size()) {
      for (long hhcode: cellsA) {
        if (cellsB.contains(hhcode)) {
          c.addCell(normRes, hhcode);
        }
      }
    } else {
      for (long hhcode: cellsB) {
        if (cellsA.contains(hhcode)) {
          c.addCell(normRes, hhcode);
        }
      }      
    }
    
    return c;
  }

  /**
   * Compute the intersection of two coverages.
   * 
   * @param a
   * @param b
   * @return A new coverage that is the intersection of A and B. A and B left untouched.
   */
  public static Coverage intersection(Coverage a, Coverage b) {
    
    //
    // If one of the coverages is empty, return an empty coverage
    //
    
    if (0 == a.getCellCount() || 0 == b.getCellCount()) {
      return new Coverage();
    }
    
    //
    // Clone coverages.
    //
    
    a = a.deepCopy();
    b = b.deepCopy();
    
    //
    // Optimize
    //
    
    a.optimize(0L);
    b.optimize(0L);
    
    //
    // Normalize
    //
    
    Coverage.normalize(a, b);
    
     //
    // Now loop over cells in the coverage with the least
    // and only keep the ones that are in both.
    //
 
    Coverage c = new Coverage();
    
    for (int r = 2; r <= 32; r += 2) {
      Set<Long> cellsA = a.getCells(r);
      Set<Long> cellsB = b.getCells(r);
    
      if (cellsA.size() < cellsB.size()) {
        for (long hhcode: cellsA) {
          if (cellsB.contains(hhcode)) {
            c.addCell(r, hhcode);
          }
        }
      } else {
        for (long hhcode: cellsB) {
          if (cellsA.contains(hhcode)) {
            c.addCell(r, hhcode);
          }
        }      
      }
    }
    
    return c;
  }

  /**
   * Optimize a coverage until the number of cells it contains
   * is less or equal to 'count'. This is useful when performing a
   * search as the less cells the better.
   * 
   * @param count Maximum number of cells the coverage can contain.
   */
  public void reduce(int count) {

    long thresholds = 0L;
      
    //
    // Optimize the coverage as a starter.
    //
    optimize(0L, HHCodeHelper.MIN_RESOLUTION, count);
    
    //
    // Do nothing if the number of cells is already ok
    //
    
    if (getCellCount() <= count) {
      return;
    }
        
    //
    // Loop while there are too many cells and while there are cells
    // at finer resolutions than 2 (because we can't reduce a coverage that
    // only has cells at resolution 2).
    //
    
    //
    // Start by optimizing at the finest resolution
    //
    
    int resolution = getFinestResolution();
    
    //
    // Start with a threshold of 15 (we already optimized with a threshold of 16)
    // We will optimize vy decreasing the threshold at each resolution and walking
    // our way to the coarsest resolution.
    //
    
    long resthreshold = 15;
    
    while (getCellCount() > count || getCellCount(2) == getCellCount()) {
      thresholds = resthreshold << (64 - 2 * resolution);
      optimize(thresholds, resolution - 2, count);
      
      // Decrease threshold for the current resolution
      resthreshold--;
      
      // If we reached 0, decrease resolution.
      if (0 == resthreshold) {
        resthreshold = 15;
        do {
          resolution -= 2;
        } while (0 == getCellCount(resolution) && resolution > 0);
      }
    }      
  }
  
  /**
   * Check if a hhcode is included by this cover.
   * 
   * @param hhcode HHCode to check.
   * @return true or false depending on result.
   */
  public boolean includes(long hhcode) {
    //
    // Check at each resolution, starting with the coarsest
    //
    
    for (int r = 0; r < 15; r++) {
      if (null == coverage[r] || coverage[r].isEmpty()) {
        continue;
      }
      // Check if there is a cell containing the point at the given resolution
      if (coverage[r].contains(hhcode & PREFIX_MASK[r])) {
        return true;
      }
    }
    
    return false;
  }
  
  public void clear() {
    this.resolutions.clear();

    for (int i = 0; i < this.coverage.length; i++) {
      this.coverage[i] = null;
    }
  }
  
  /**
   * Split some cells of the coverage until it contains a given cell which was
   * covered by cells in the coverage but at a possibly coarser resolution.
   * 
   * For example, if coverage contains HHCode 'A' and splitTo is called with
   * arguments ('6', 'AAA'), then 'A' will be split into 'A0', 'A1', 'A2', ... 'A9', 'AB', ... 'AF'
   * and 'AA0', 'AA1', ... 'AAF' thus leading to 15 + 16 cells.
   * 
   * The number of created cells is N = (DeltaRes/2) * 15 + 16, this is a far better optimization than
   * normalizing the coverages to the new resolution which would split each cell into N = 4^DeltaRes subcells
   * 
   * 
   * If 'hhcode' does not exist in the coverage at 'resolution', nothing happens.
   * If 'newresolution' is coarser or equal to 'resolution', nothing happens.
   * 
   * @param resolution Resolution (2->32) at which hhcode should be included
   * @param hhcode Cell to ultimately include
   */
  
  public void splitTo(int resolution, long hhcode) {
    
    //
    // Check if hhcode is fully covered by the coverage
    //
    
    int coveredAtResolution = -1;
    
    for (int r = 0; r < 16; r++) {
      // Break if the cell is contained in a larger cell
      if (null != coverage[r] && coverage[r].contains(hhcode & PREFIX_MASK[r])) {
        coveredAtResolution = r;
        break;
      }
      //
      // Break if we've reached the target resolution
      //
      if (((r+1)<<1) == resolution) {
        break;
      }
    }
    
    //
    // If cell is not covered by this coverage or
    // already in the coverage, return
    //
    
    if (-1 == coveredAtResolution || resolution == ((coveredAtResolution + 1) << 1)) {
      return;
    }
    
    //
    // Split cell at the next resolution and repeat the process
    // for each subcell
    //
    
    // Remove original cell
    
    coverage[coveredAtResolution].remove(hhcode & PREFIX_MASK[coveredAtResolution]);
    
    // Add 16 subcells
    
    if (null == coverage[coveredAtResolution + 1]) {
      coverage[coveredAtResolution + 1] = new HashSet<Long>();
    }
    
    for (int i = 0; i < 16; i++) {
      long subcell = ((hhcode & PREFIX_MASK[coveredAtResolution]) | (((long) i) << (60  - 4 * (coveredAtResolution + 1)))) & PREFIX_MASK[coveredAtResolution + 1];
      //System.out.printf("%x %x\n", hhcode & PREFIX_MASK[coveredAtResolution], subcell);
      coverage[coveredAtResolution + 1].add(subcell);  
    }
    
    //
    // Repeat process
    //
    
    splitTo(resolution, hhcode);
  }

  /**
   * Modify two coverages so cells that are covered by both coverages
   * appear in both at the same resolution.
   * This implies splitting cells until the right cell appear.
   * 
   * Ideally both coverages should have been previously optimized to
   * reduce the number of operations needed.
   * 
   * @param a
   * @param b
   */
  public static void normalize(Coverage a, Coverage b) {
    
    //
    // Proceed from coarsest resolution to the finest
    // and for each resolution first reveal cells from a
    // then reveal cells from b
    //
    
    for (int r = 0; r < 16; r++) {      
      if (null != a.coverage[r]) {
        for (long hhcode: a.coverage[r]) {
          b.splitTo((r + 1) << 1, hhcode);
        }
      }
      if (null != b.coverage[r]) {
        for (long hhcode: b.coverage[r]) {
          a.splitTo((r + 1) << 1, hhcode);
        }        
      }
    }
  }
}
