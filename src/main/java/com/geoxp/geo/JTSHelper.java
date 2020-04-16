//
//   GeoXP Lib, library for efficient geo data manipulation
//
//   Copyright 2020-      SenX S.A.S.
//   Copyright 2019-2020  iroise.net S.A.S.
//   Copyright 1999-2019  Mathias Herberts
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.predicate.RectangleIntersects;

public class JTSHelper {
  
  private static double[] resLatOffset = new double[33];
  private static double[] resLonOffset = new double[33];
  
  private static long[] level2GeoCells = { 0x1000000000000000L, 0x1100000000000000L, 0x1200000000000000L, 0x1300000000000000L,
                                           0x1400000000000000L, 0x1500000000000000L, 0x1600000000000000L, 0x1700000000000000L,
                                           0x1800000000000000L, 0x1900000000000000L, 0x1a00000000000000L, 0x1b00000000000000L,
                                           0x1c00000000000000L, 0x1d00000000000000L, 0x1e00000000000000L, 0x1f00000000000000L };
  
  static {
    for (int i = 0; i < 33; i++) {
      resLatOffset[i] = HHCodeHelper.DEGREES_PER_LAT_UNIT * (1 << (32 - i));
      resLonOffset[i] = HHCodeHelper.DEGREES_PER_LON_UNIT * (1 << (32 - i));
    }
  }
  
  private static ThreadLocal<Coordinate[]> coordinateCache = new ThreadLocal<Coordinate[]>() {
    protected Coordinate[] initialValue() {
      return new Coordinate[5];
    };
  };
  
  private static ThreadLocal<GeometryFactory> factoryCache = new ThreadLocal<GeometryFactory>() {
    @Override
    protected GeometryFactory initialValue() {
      return new GeometryFactory();
    }
  };
  
  public static LinearRing hhcodeToLinearRing(long hhcode, int resolution) {
    //Coordinate[] coords = coordinateCache.get();
    Coordinate[] coords = new Coordinate[5];
    
    double[] latlon = HHCodeHelper.getLatLon(hhcode, resolution);
    
    coords[0] = new Coordinate(latlon[1], latlon[0]);
    coords[1] = new Coordinate(latlon[1], latlon[0] + resLatOffset[resolution]);
    coords[2] = new Coordinate(latlon[1] + resLonOffset[resolution], latlon[0] + resLatOffset[resolution]);
    coords[3] = new Coordinate(latlon[1] + resLonOffset[resolution], latlon[0]);
    coords[4] = coords[0];
      
    //return factoryCache.get().createLinearRing(coords);
    return new GeometryFactory().createLinearRing(coords);
  }
  
  public static LinearRing geoCellToLinearRing(long geocell) {
    int resolution = (int) (((geocell & 0xf000000000000000L) >> 60) & 0xf);
    return hhcodeToLinearRing(geocell << 4, resolution << 1);
  }
  
  /**
   * 
   * @param geometry Geometry to cover
   * @param minresolution Coarsest resolution to use for coverage
   * @param maxresolution Finest resolution to use for coverage, if negative, will be the coarsest resolution encountered + maxresolution
   * @param containedOnly Only consider finest resolution cells which are fully contained, useful when subtracting a coverage.
   * @param maxcells Maximum number of cells in the coverage. If the coverage is not complete when we reach this number of cells, return
   *                 null.
   * @return The computed coverage or null if maxcells was reached before the coverage was complete.
   */
  public static Coverage coverGeometry(Geometry geometry, int minresolution, int maxresolution, boolean containedOnly, int maxcells) {
    //
    // Start with the 16 cells at resolution 2
    //
    
    long[] geocells = new long[256];
    
    //TLongArrayList geocells = new TLongArrayList(1000000);
    //geocells.add(level2GeoCells);
    
    int idx = 0;
    
    for (long geocell: level2GeoCells) {
      geocells[idx++] = geocell;
    }
    
    Coverage c = new Coverage();
    
    LinearRing[] empty = new LinearRing[0];
    
    GeometryFactory factory = new GeometryFactory();
    
    int cellcount = 0;
    int ngeocells = idx;
    int curidx = 0;
    
    //while (0 != geocells.size() && cellcount < maxcells) {
    while (0 != ngeocells && cellcount < maxcells) {
      //
      // Create the rectangle of the first geocell
      //

      //long geocell = geocells.removeAt(0);
      long geocell = geocells[curidx++];
      ngeocells--;

      int cellres = ((int) (((geocell & 0xf000000000000000L) >> 60) & 0xf)) << 1;

      //ystem.out.println(maxresolution + " >>> " + cellres + " >>> count=" + cellcount + " >>> " + ngeocells);
      //Polygon cellgeo = new Polygon(JTSHelper.hhcodeToLinearRing(geocell << 4, cellres), empty, factoryCache.get());
      Polygon cellgeo = new Polygon(JTSHelper.hhcodeToLinearRing(geocell << 4, cellres), empty, factory);

      //
      // If the current cell does not intersect 'geometry', ignore the cell and continue
      //
      
      if (!RectangleIntersects.intersects(cellgeo, geometry)) {
        continue;
      }
      
      //
      // If 'cellres' is the maximum resolution, intersecting the geometry is
      // sufficient to include the cell if 'containedOnly' is false
      //
      
      if (maxresolution == cellres && !containedOnly) {
        c.addCell(cellres, geocell << 4);
        cellcount++;
        continue;
      }
      
      //
      // If the cell is fully contained in 'geometry', add it to the coverage
      //
      
      if (geometry.covers(cellgeo) && cellres >= minresolution) {
        if (maxresolution < 0) {
          maxresolution = cellres - maxresolution;
        }
        c.addCell(cellres, geocell << 4);
        cellcount++;
        continue;
      }
      
      //
      // Do not further subdivide cells if we've reached the finest resolution
      //
      
      if (maxresolution == cellres) {
        continue;
      }
      
      //
      // Cell is not fully contained, add its 16 children to 'geocells'
      // If cellres is 30, check the 16 children manually as they can't be represented as
      // geocells
      //
      
      if (30 == cellres) {
        long[] subcells = HHCodeHelper.getSubGeoCells(geocell);
        
        for (long hhcode: subcells) {
          LinearRing lr = JTSHelper.hhcodeToLinearRing(hhcode, HHCodeHelper.MAX_RESOLUTION);
          if (geometry.intersects(lr) && !containedOnly || geometry.covers(lr)) {
            c.addCell(HHCodeHelper.MAX_RESOLUTION, hhcode);
            cellcount++;
          }
        }
      } else {
        //geocells.add(HHCodeHelper.getSubGeoCells(geocell));
        if (geocells.length < idx + 16) {
          if (curidx > 16) {
            for (int i = 0; i < ngeocells; i++) {
              geocells[i] = geocells[curidx + i];
            }
            idx = ngeocells;
            curidx = 0;
          } else {
            long[] tmp = new long[Math.min(geocells.length * 2, geocells.length + 65536)];
            System.arraycopy(geocells, curidx, tmp, 0, ngeocells);
            curidx = 0;
            idx = ngeocells;
            geocells = tmp;
          }
        }
        for (long cell: HHCodeHelper.getSubGeoCells(geocell)) {
          geocells[idx++] = cell;
        }
        ngeocells += 16;
      }
    }
    
    //if (!geocells.isEmpty()) {
    if (0 != ngeocells) {
      return null;
    }
    
    return c;
  }
  
  public static Coverage coverGeometry(Geometry geometry, int minresolution, int maxresolution, boolean containedOnly) {
    return coverGeometry(geometry, minresolution, maxresolution, containedOnly, Integer.MAX_VALUE);
  }
}
