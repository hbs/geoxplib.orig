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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import com.geoxp.GeoXPLib;

/**
 * A Bloom Filter specifically designed for locations
 * 
 */
public class GeoBloomFilter {
  
  //
  // We compute the sizes of the successive generations of Bloom Filter
  //
  // Gen 0: 1000 elements at 1% error
  // Gen 1: 10000 elements at 2% error
  // Gen 2: 100000 elements at 3% error
  // Gen 3: 1000000 elements at 4% error
  // Gen 4: 10000000 elements at 5% error
  // Gen 5: 100000000 elements at 10% error 
  //

  /**
   * Number of hash functions
   */
  private static final int K = 100;
  
  private static final int MAX_GEN = 6;

  private static final int[] DEFAULT_OFFSETS = new int[MAX_GEN];
  private static final int[] DEFAULT_LIMITS = new int[MAX_GEN];
  private static final int[] DEFAULT_LENGTHS = new int[MAX_GEN];
  
  private long known;
  
  /**
   * Hash keys (2 per hash functions)
   */
  private static final long[] hashkeys;

  static {
    int[] n = new int[] { 1000, 10000, 100000, 1000000, 10000000, 100000000 };
    double[] p = new double[] { 0.01, 0.02, 0.03, 0.04, 0.05, 0.1 };
    
    int offset = 16 + 256;
    
    for (int i = 0; i < n.length; i++) {
      DEFAULT_OFFSETS[i] = offset;
      DEFAULT_LENGTHS[i] = (int) Math.ceil((-n[i] * Math.log(p[i]) / (Math.log(2) * Math.log(2))));
      DEFAULT_LIMITS[i] = n[i];
      offset += n[i];
    }

    try {
      Random r = new Random();
            
      //
      // Generate hash keys
      //
      
      hashkeys = new long[K * 2];
      
      for (int i = 0; i < hashkeys.length; i++) {
        hashkeys[i] = r.nextLong();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  private final int[] offsets;
  private final int[] limits;
  private final int[] lengths;
  
  private static final sun.misc.Unsafe UNSAFE;
  static {
    sun.misc.Unsafe unsafe = null;
    try {
      java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      unsafe = (sun.misc.Unsafe)field.get(null);
    } catch (Exception e) {
      e.printStackTrace();
    }
    UNSAFE = unsafe;
  }
  private static final long base = UNSAFE.arrayBaseOffset(new byte[0].getClass());

  /**
   * Current generation (determines the offset)
   */
  
  private int generation = 0;

  private int k = 6;
  
  /**
   * Do we slice the output of a single computation or compute multiple hash functions
   */
  private final boolean slice;
  
  /**
   * Initial bitset for level 1 cells (16),
   * level 2 cells (256) and an initial bloom filter for 1k
   * entries with 1% error (m = - n ln p / (ln 2)^2)
   * 9587 is prime
   */
  private final BitSet bits;
  
  /**
   * Number of 'new' elements added to the filter
   */
  private long[] counts = new long[] { 0 };
    
  private final byte[] data = new byte[8];
  
  private final int maxres;
  private final long resolutionMask;

  private void init(int[] n, double[] p) {
    if (n.length != p.length || n.length > MAX_GEN || p.length > MAX_GEN) {
      throw new RuntimeException("Invalid n/p arrays, max size is " + MAX_GEN);
    }
    
    int offset = 16 + 256;
    
    for (int i = 0; i < n.length; i++) {
      offsets[i] = offset;
      lengths[i] = (int) Math.ceil((-n[i] * Math.log(p[i]) / (Math.log(2) * Math.log(2))));
      limits[i] = n[i];
      offset += n[i];
    }
  }
  
  public GeoBloomFilter(int maxresolution, int[] n, double[] fprate, int k, boolean slice) {
    if (maxresolution < 1 || maxresolution > 15) {
      throw new RuntimeException("Invalid resolution, MUST be between 1 and 15, both inclusive.");
    }
    
    if (k > hashkeys.length / 2) {
      throw new RuntimeException("k cannot be greater than " + (hashkeys.length / 2));
    }
        
    this.slice = slice;
    this.k = k;
    
    if (k > 32 && slice) {
      throw new RuntimeException("slicing can only be used when k <= 32.");
    }
    
    this.maxres = maxresolution;
    this.resolutionMask = 0xFFFFFFFFFFFFFFFL << (60 - (maxresolution * 4));
    
    if (null != n && null != fprate) {
      offsets = new int[n.length];
      limits = new int[n.length];
      lengths = new int[n.length];
      
      init(n, fprate);
    } else {
      offsets = DEFAULT_OFFSETS;
      limits = DEFAULT_LIMITS;
      lengths = DEFAULT_LENGTHS;
    }
        
    this.bits = new BitSet(16 + 256 + lengths[0]);
  }

  /**
   * Adapt a cell so its resolution is no more than maxres
   */
  public long fixCell(long cell) {
    //
    // Adapt the resolution if it's over maxres
    //
    
    int res = (int) ((cell >>> 60) & 0xFL);
    
    if (res > maxres) {
      cell = cell & 0x0FFFFFFFFFFFFFFFL;
      cell = cell | (((maxres & 0xFL) << 60) & 0xF000000000000000L);
      cell = cell & resolutionMask;
    }

    return cell;
  }
  
  /**
   * This method is synchronized so we can use the 'data' field without
   * problems
   */
  public synchronized void add(long hhcode) {
    //
    // Extract level 2
    //
    int l2bits = (int) (hhcode >>> 56);

    this.bits.set(16 + l2bits);
    
    //
    // Extract level 1
    //
    int l1bits = (l2bits & 0xF0) >>> 4;
    
    this.bits.set(l1bits);
    
    //
    // If res is 1 or 2, return now
    //
    
    if (0L == (hhcode & 0x00FFFFFFFFFFFFFFL)) {
      return;
    }
    
    //
    // Hash cell
    //
    
    for (int res = 0; res < maxres; res++) {
      
      // Encode resolution
      long cell = ((long) (res+1)) << 60;
      
      // Encode HHCode
      cell |= (hhcode >> 4) & 0x0fffffffffffffffL;
      
      // Trim HHCode to resolution
      cell &= (0xffffffffffffffffL ^ ((1L << (4 * (15 - (res + 1)))) - 1)); 

      for (int i = 0; i < 8; i++) {
        data[7 - i] = (byte) (cell & 0xFFL);
        cell = cell >>> 8;
      }
      
      boolean inset = true;

      if (this.slice) {
        long initialHash = hash24(hashkeys[0], hashkeys[1], data, 0, data.length);
        
        for (int i = 0; i < k; i++) {
          // Compute kth hash by shifting 'initialHash' k bits to the right.
          long hash = (initialHash >>> i) & 0xFFFFFFFFL;
          
          // If 'inset' is true, check if the bit was set, if so, skip modifying the bitset
          
          if (inset && this.bits.get(offsets[generation] + (int) (hash % lengths[generation]))) {
            //known++;
            continue;
          }
          
          // Set the bit to '1'
          this.bits.set(offsets[generation] + (int) (hash % lengths[generation]));
          
          inset = false;
        }
      } else {
        for (int i = 0; i < k * 2; i += 2) {
          long hash = hash24(hashkeys[i], hashkeys[i + 1], data, 0, data.length) & 0xFFFFFFFFL;
          
          // If 'inset' is true, check if the bit was set, if so, skip modifying the bitset
          
          if (inset && this.bits.get(offsets[generation] + (int) (hash % lengths[generation]))) {
            //known++;
            continue;
          }
          
          // Set the bit to '1'
          this.bits.set(offsets[generation] + (int) (hash % lengths[generation]));
          
          inset = false;
        }        
      }

      if (!inset) {
        counts[generation]++;
        
        //
        // Check if we should allocate a new Bloom Filter
        //
              
        if (counts[generation] >= limits[generation]) {        
          if (generation < MAX_GEN - 1) {          
            generation++;
            counts = Arrays.copyOf(counts, counts.length + 1);
          }
        }
      }
    }
  }
  
  public boolean contains(long cell) {
    
    //
    // Extract level 1 & 2
    //
    int l2bits = (int) ((cell & 0x0FF0000000000000L) >>> 52);
    int l1bits = (l2bits & 0xF0) >>> 4;

    int res = (int) ((cell >>> 60) & 0xFL);

    if (0 == res) {
      return false;
    }
    
    //
    // Check levels 1 and 2
    //
    
    if (!this.bits.get(l1bits)) {
      return false;
    }

    if (1 == res) {
      return true;
    }
    
    if (!this.bits.get(16 + l2bits)) {
      return false;
    }
        
    if (2 == res) {
      return true;
    }
        
    //
    // Hash cell
    //
    
    byte[] data = new byte[8];
    for (int i = 0; i < 8; i++) {
      data[7 - i] = (byte) (cell & 0xFFL);
      cell = cell >>> 8;
    }
    
    long gens = (1L << (generation + 1)) - 1L;
    
    if (this.slice) {
      long initialHash = hash24(hashkeys[0], hashkeys[1], data, 0, data.length);
      
      for (int i = 0; i < k; i++) {
        long hash = (initialHash >>> i) & 0xFFFFFFFFL;
        
        for (int g = 0; g <= generation; g++) {
          if (!this.bits.get(offsets[g] + (int) ((hash & 0xFFFFFFFFL) % lengths[g]))) {
            // Set the 'g' bit to 0 to indicate at least one bit was not set in the associated bit field
            gens = gens & (0xFFFFFFFFFFFFFFFFL ^ (1L << g));
            // Return early if gens is already 0
            if (0 == gens) {
              return false;
            }
          }        
        }
      }
    } else {
      for (int i = 0; i < k * 2; i += 2) {
        long hash = hash24(hashkeys[i], hashkeys[i + 1], data, 0, data.length);
        
        for (int g = 0; g <= generation; g++) {
          if (!this.bits.get(offsets[g] + (int) ((hash & 0xFFFFFFFFL) % lengths[g]))) {
            // Set the 'g' bit to 0 to indicate at least one bit was not set in the associated bit field
            gens = gens & (0xFFFFFFFFFFFFFFFFL ^ (1L << g));
            // Return early if gens is already 0
            if (0 == gens) {
              return false;
            }
          }        
        }
      }      
    }

    //
    // If at least one bit of gens is set then this means the location was found
    // in at least one bitset
    //
    
    return 0 != gens;
  }
  
  /**
   * Check wether a cell and all its parents are contained in the bloom filter.
   * This mitigates the false positive probability by ensuring that the whole
   * family of cells are indeed in the set.
   * 
   * @param cell
   * @return
   */
  public boolean containsHierarchy(long cell) {
    while(0L != cell && contains(cell)) {
      cell = HHCodeHelper.parentGeoCell(cell);
    }
    
    return 0L == cell;
  }
  
  public long[] getKeys() {
    return this.hashkeys;
  }
  
  /**
   * Return an estimated memory footprint for this index
   */
  public long size() {
    return bits.size() / 8;
  }
  
  private static long hash24(long k0, long k1, byte[] data, int offset, int len) {
    long v0 = 0x736f6d6570736575L ^ k0;
    long v1 = 0x646f72616e646f6dL ^ k1;
    long v2 = 0x6c7967656e657261L ^ k0;
    long v3 = 0x7465646279746573L ^ k1;
    long m;
    int last = len / 8 * 8;
    
    int i = 0;

    // processing 8 bytes blocks in data
    while (i < last) {
      
      m = UNSAFE.getLong(data, base + offset + i);
      
      i += 8;
      
      // MSGROUND {
      v3 ^= m;

      /*
       * SIPROUND wih hand reordering
       * 
       * SIPROUND in siphash24.c:
       * A: v0 += v1;
       * B: v1=ROTL(v1,13);
       * C: v1 ^= v0;
       * D: v0=ROTL(v0,32);
       * E: v2 += v3;
       * F: v3=ROTL(v3,16);
       * G: v3 ^= v2;
       * H: v0 += v3;
       * I: v3=ROTL(v3,21);
       * J: v3 ^= v0;
       * K: v2 += v1;
       * L: v1=ROTL(v1,17);
       * M: v1 ^= v2;
       * N: v2=ROTL(v2,32);
       * 
       * Each dependency:
       * B -> A
       * C -> A, B
       * D -> C
       * F -> E
       * G -> E, F
       * H -> D, G
       * I -> H
       * J -> H, I
       * K -> C, G
       * L -> K
       * M -> K, L
       * N -> M
       * 
       * Dependency graph:
       * D -> C -> B -> A
       * G -> F -> E
       * J -> I -> H -> D, G
       * N -> M -> L -> K -> C, G
       * 
       * Resulting parallel friendly execution order:
       * -> ABCDHIJ
       * -> EFGKLMN
       */

      // SIPROUND {
      v0 += v1;
      v2 += v3;
      v1 = (v1 << 13) | v1 >>> 51;
      v3 = (v3 << 16) | v3 >>> 48;
      v1 ^= v0;
      v3 ^= v2;
      v0 = (v0 << 32) | v0 >>> 32;
      v2 += v1;
      v0 += v3;
      v1 = (v1 << 17) | v1 >>> 47;
      v3 = (v3 << 21) | v3 >>> 43;
      v1 ^= v2;
      v3 ^= v0;
      v2 = (v2 << 32) | v2 >>> 32;
      // }
      // SIPROUND {
      v0 += v1;
      v2 += v3;
      v1 = (v1 << 13) | v1 >>> 51;
      v3 = (v3 << 16) | v3 >>> 48;
      v1 ^= v0;
      v3 ^= v2;
      v0 = (v0 << 32) | v0 >>> 32;
      v2 += v1;
      v0 += v3;
      v1 = (v1 << 17) | v1 >>> 47;
      v3 = (v3 << 21) | v3 >>> 43;
      v1 ^= v2;
      v3 ^= v0;
      v2 = (v2 << 32) | v2 >>> 32;
      // }
      v0 ^= m;
      // }
    }

    // packing the last block to long, as LE 0-7 bytes + the length in the top
    // byte
    m = 0;
    for (i = len - 1; i >= last; --i) {
      m <<= 8;
      m |= data[offset + i] & 0xffL;
    }
    m |= (long) len << 56;
    
    // MSGROUND {
    v3 ^= m;
    // SIPROUND {
    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;
    // }
    // SIPROUND {
    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;
    // }
    v0 ^= m;
    // }

    // finishing...
    v2 ^= 0xff;
    // SIPROUND {
    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;
    // }
    // SIPROUND {
    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;
    // }
    // SIPROUND {
    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;
    // }
    // SIPROUND {
    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;
    // }
    return v0 ^ v1 ^ v2 ^ v3;
  }

}
