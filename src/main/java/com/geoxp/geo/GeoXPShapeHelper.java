//
//   GeoXP Lib, library for efficient geo data manipulation
//
//   Copyright 2020-      SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
package com.geoxp.geo;

import java.util.Arrays;
import java.util.BitSet;

import com.geoxp.GeoXPLib;
import com.geoxp.GeoXPLib.GeoXPShape;

public class GeoXPShapeHelper {
  
  private static long[] RESOLUTION_MASKS;
  
  static {
    RESOLUTION_MASKS = new long[16];
    RESOLUTION_MASKS[1]  = 0x0f00000000000000L;
    RESOLUTION_MASKS[2]  = 0x0ff0000000000000L;
    RESOLUTION_MASKS[3]  = 0x0fff000000000000L;
    RESOLUTION_MASKS[4]  = 0x0ffff00000000000L;
    RESOLUTION_MASKS[5]  = 0x0fffff0000000000L;
    RESOLUTION_MASKS[6]  = 0x0ffffff000000000L;
    RESOLUTION_MASKS[7]  = 0x0fffffff00000000L;
    RESOLUTION_MASKS[8]  = 0x0ffffffff0000000L;
    RESOLUTION_MASKS[9]  = 0x0fffffffff000000L;
    RESOLUTION_MASKS[10] = 0x0ffffffffff00000L;
    RESOLUTION_MASKS[11] = 0x0fffffffffff0000L;
    RESOLUTION_MASKS[12] = 0x0ffffffffffff000L;
    RESOLUTION_MASKS[13] = 0x0fffffffffffff00L;
    RESOLUTION_MASKS[14] = 0x0ffffffffffffff0L;
    RESOLUTION_MASKS[15] = 0x0fffffffffffffffL;
  }

  public static boolean intersects(GeoXPShape a, GeoXPShape b) {
    return null != intersection(a, b, true);
  }
  
  public static GeoXPShape intersection(GeoXPShape a, GeoXPShape b) {
    return intersection(a, b, false);
  }
  
  private static GeoXPShape intersection(GeoXPShape a, GeoXPShape b, boolean check) {
       
   long[] acells = GeoXPLib.getCells(a);
   long[] bcells = GeoXPLib.getCells(b);
   
   //
   // Sort cells
   //
   //Arrays.sort(acells);
   //Arrays.sort(bcells);
   
   // Determine the start/end index of each resolution
   int[] astart = new int[16];
   int[] aend = new int[16];
   int[] bstart = new int[16];
   int[] bend = new int[16];
   
   for (int i = 1; i < 16; i++) {
     long cell = ((long) i) << 60;
     int idx = Arrays.binarySearch(acells, cell); 

     if (idx >= 0) {
       astart[i] = idx;
     } else {
       astart[i] = -idx - 1;
       if (astart[i] >= acells.length || (acells[astart[i]] & 0xF000000000000000L) != cell) {
         astart[i] = -1;
       }
     }
            
     idx = Arrays.binarySearch(bcells, cell);       
     
     if (idx >= 0) {
       bstart[i] = idx;
     } else {
       bstart[i] = -idx - 1;
       if (bstart[i] >= bcells.length || (bcells[bstart[i]] & 0xF000000000000000L) != cell) {
         bstart[i] = -1;
       }
     }
   }
   
   for (int i = 1; i < 16; i++) {
     aend[i] = -1;
     if (-1 != astart[i]) {
       // Find the start of the next index
       for (int j = i + 1; j < 16; j++) {
         if (-1 != astart[j]) {
           aend[i] = astart[j] - 1;
           break;
         }
       }
       if (-1 == aend[i]) {
         if (i >= 8) {
           for (int j = 1; j < 8; j++) {
             if (-1 != astart[j]) {
               aend[i] = astart[j] - 1;
               break;
             }
           }
         }
         if (-1 == aend[i]) {
           aend[i] = acells.length - 1;
         }
       }
     }
   }

   for (int i = 1; i < 16; i++) {
     bend[i] = -1;
     if (-1 != bstart[i]) {
       // Find the start of the next index
       for (int j = i + 1; j < 16; j++) {
         if (-1 != bstart[j]) {
           bend[i] = bstart[j] - 1;
           break;
         }
       }
       if (-1 == bend[i]) {
         if (i >= 8) {
           for (int j = 1; j < 8; j++) {
             if (-1 != bstart[j]) {
               bend[i] = bstart[j] - 1;
               break;
             }
           }
         }
         if (-1 == bend[i]) {
           bend[i] = bcells.length - 1;
         }
       }
     }
   }

   //
   // Maintain a bitset of included cells
   //
   
   BitSet froma = new BitSet(acells.length);
   BitSet fromb = new BitSet(bcells.length);
   
   //
   // Now iterate over the cells of both shapes by increasing resolution
   //
   
   for (int res = 1; res < 16; res++) {
     if (-1 != astart[res]) {
       for (int i = astart[res]; i <= aend[res]; i++) {
         // Ignore cells which are already included
         if (froma.get(i)) {
           continue;
         }
         long cell = acells[i] & RESOLUTION_MASKS[res];
         // Iterate over the resolutions finer or equal to res in the other shape
         for (int subres = res; subres < 16; subres++) {
           if (-1 == bstart[subres]) {
             continue;
           }
           // attempt to find the insertion point for the subcell 0 at subres
           long subcell = cell | (((long) subres) << 60);
           int idx = Arrays.binarySearch(bcells, bstart[subres], bend[subres] + 1, subcell);
           if (idx < 0) {
             idx = -idx - 1;
           }
           while(idx < bend[subres]) {
             if (cell != (bcells[idx] & RESOLUTION_MASKS[res])) {
               break;
             }
             if (check) {
               return new GeoXPShape();
             }
             fromb.set(idx);
             idx++;
           }
         }
       }
     }
     
     if (-1 != bstart[res]) {
       for (int i = bstart[res]; i <= bend[res]; i++) {
         // Ignore cells which are already included
         if (fromb.get(i)) {
           continue;
         }
         long cell = bcells[i] & RESOLUTION_MASKS[res];
         // Iterate over the resolution finer or equal to res in the other shape
         for (int subres = res; subres < 16; subres++) {
           if (-1 == astart[subres]) {
             continue;
           }
           // attempt to find the insertion point for the subcell 0 at subres
           long subcell = cell | (((long) subres) << 60);
           int idx = Arrays.binarySearch(acells, astart[subres], aend[subres] + 1, subcell);
           if (idx < 0) {
             idx = -idx - 1;
           }
           while(idx < aend[subres]) {
             if (cell != (acells[idx] & RESOLUTION_MASKS[res])) {
               break;
             }
             if (check) {
               return new GeoXPShape();
             }
             froma.set(idx);
             idx++;
           }
         }
       }
     }
   }

   // If only checking, return null as the intersection is empty
   if (check) {
     return null;
   }
   
   long[] intersection = new long[froma.cardinality() + fromb.cardinality()];
   
   int i = 0;
   int idx = 0;
   
   while(true) {
     idx = froma.nextSetBit(idx);
     if (-1 == idx) {
       break;
     }
     intersection[i++] = acells[idx];
     idx++;
   }
   
   idx = 0;
   
   while(true) {
     idx = fromb.nextSetBit(idx);
     if (-1 == idx) {
       break;
     }
     intersection[i++] = bcells[idx];
     idx++;
   }
   
   Arrays.sort(intersection);
   return GeoXPLib.fromCells(intersection, false);
  }
}
