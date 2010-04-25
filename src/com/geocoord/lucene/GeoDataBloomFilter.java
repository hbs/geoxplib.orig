package com.geocoord.lucene;

import org.apache.lucene.util.OpenBitSet;

/**
 * This class implements Bloom Filters which can be
 * used to determine if a given atom is stored in a given segment.
 * 
 * This is intended to speed up deletion of atoms by UUID which
 * are mandatory when indexing a given Atom.
 * 
 * TODO(hbs): adapt BF to segment size
 */
public class GeoDataBloomFilter {
  
  private static int BLOOM_FILTER_BITS = 65536;
  /**
   * Bit masks to extract various values. Element 0 MUST be the least significant
   * bit extractor as it is also used to constraint the result to X bits.
   */
  private static final long[] BLOOM_FILTER_MASKS = { 0x000000000000ffffL, 0xffff000000000000L, 0x0000ffff00000000L, 0x00000000ffff0000L };
  private static final int[] BLOOM_FILTER_OFFSETS = { 48, 32, 16, 0 };
  
  private OpenBitSet[] bitsets = new OpenBitSet[BLOOM_FILTER_MASKS.length];
  
  public GeoDataBloomFilter() {
    // Allocate bit sets
    for (int i = 0; i < BLOOM_FILTER_MASKS.length; i++) {
      bitsets[i] = new OpenBitSet(BLOOM_FILTER_BITS);
    }
  }
  
  /**
   * Store a UUID in the bloom filter
   * @param msb Most significant bits of the UUID
   * @param lsb Leas significant bits of the UUID
   */
  public void store(long msb, long lsb) {
    for (int i = 0; i < BLOOM_FILTER_MASKS.length; i++) {
      int bit = (int) (((msb & BLOOM_FILTER_MASKS[i]) >> BLOOM_FILTER_OFFSETS[i]) & BLOOM_FILTER_MASKS[0]);
      bitsets[i].fastSet(bit);
    }
  }
  
  /**
   * Check if the bloom filter is set for the
   * given UUID
   */
  public boolean isSet(long msb, long lsb) {
    for (int i = 0; i < BLOOM_FILTER_MASKS.length; i++) {
      int bit = (int) (((msb & BLOOM_FILTER_MASKS[i]) >> BLOOM_FILTER_OFFSETS[i]) & BLOOM_FILTER_MASKS[0]);
      if (!bitsets[i].fastGet(bit)) {
        return false;
      }
    }
    
    return true;
  }
}
