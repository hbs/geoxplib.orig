package com.geoxp.heatmap;

import java.util.ArrayList;
import java.util.List;

import com.geoxp.geo.Coverage;
import com.geoxp.geo.GeoParser;
import com.geoxp.geo.HHCodeHelper;

public class HeatMapManagerTileBuilder extends TileBuilder {
  
  /**
   * HeatMapManager to use to retrieve data.
   */
  private final HeatMapManager manager;
  
  public HeatMapManagerTileBuilder(HeatMapManager manager) {
    this.manager = manager;
  }
  
  @Override
  public double[] fetchData(double nwLat, double nwLon, double seLat, double seLon, int zoom, long timestamp, long bucketspan, int bucketcount, double timedecay) {
    
    //
    // Expand search area so we have data overlap between tiles
    //
    
    double dlat = (nwLat - seLat) * 0.01;
    double dlon = (seLon - nwLon) * 0.01;
        
    seLat -= dlat;
    nwLat += dlat;
    seLon += dlon;
    nwLon -= dlon;
    
    //
    // Generate coverage for the tile
    //
    
    StringBuilder sb = new StringBuilder();
    sb.append(seLat > -90.0 ? seLat : -90.0);
    sb.append(":");
    sb.append(nwLon > -180.0 ? nwLon : -180.0);
    sb.append(",");
    sb.append(nwLat < 90.0 ? nwLat : 90.0);
    sb.append(":");
    sb.append(seLon < 180.0 ? seLon : 180.0);
    
    //
    // Compute coverage
    //
    
    int optr = (zoom + 2) - ((zoom + 2) % 2);
        
    if (optr > manager.getConfiguration().getMaxResolution() - this.manager.getConfiguration().getResolutionOffset()) {
      optr = manager.getConfiguration().getMaxResolution() - this.manager.getConfiguration().getResolutionOffset();
    }
    
    Coverage c = GeoParser.parseViewport(sb.toString(), optr);
    
    // retrieve resolution
    int r = c.getFinestResolution();
    
    // Determine if we will be able to return results or not
    
    if (r + this.manager.getConfiguration().getResolutionOffset() < this.manager.getConfiguration().getMinResolution() || r > this.manager.getConfiguration().getMaxResolution()) {
      return new double[0];
    }

    int realoffset = this.manager.getConfiguration().getResolutionOffset();
    
    if (r + realoffset > this.manager.getConfiguration().getMaxResolution()) {
      realoffset = this.manager.getConfiguration().getMaxResolution() - r;
    }
     
    long[] geocells = c.toGeoCells(HHCodeHelper.MAX_RESOLUTION);
    
    //
    // Retrieve data from the HeatMapManager
    //
    
    List<Double> data = new ArrayList<Double>();
    
    for (long geocell: geocells) {
      
      //
      // Offset geocell
      //
     
      long rprefix = (((long) (r + realoffset)) >> 1) << 60;
      long rmask = (0xffffffffffffffffL >> (60 - ((r + realoffset) << 1))) << (60 - ((r + realoffset) << 1));
            
      for (int i = 0; i < (1 << (realoffset << 1)); i++) {
        long subcell = rprefix;
        subcell |= geocell & 0x0fffffffffffffffL;
        subcell |= ((long) i) << (((32 - r - realoffset) << 1) - 4);
        subcell &= rmask;
      
        double[] result = manager.getData(subcell, timestamp, bucketspan, bucketcount, timedecay);

        if (0.0 == result[2]) {
          continue;
        }
        
        //
        // Compute the coordinates of the center of the geocell
        //
        
        //long hhcode = (subcell & 0x0fffffffffffffffL) << 4;
        
        //
        // Add centroid's coords
        //
        
        data.add(result[0]);
        data.add(result[1]);
        
        //
        // Add value
        //
        
        data.add(result[2]);
      }      
    }
    
    double[] result = new double[data.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = data.get(i);
    }
    
    return result;
  }
}
