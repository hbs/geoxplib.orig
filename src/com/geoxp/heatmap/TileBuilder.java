package com.geoxp.heatmap;

import java.awt.image.BufferedImage;

public abstract class TileBuilder {
  
  private static final int TILE_WIDTH = 256;
  private static final int TILE_HEIGHT = TILE_WIDTH;
  
  public static interface Scaler {
    double scale(double value);
  };
  
  /**
   * Return the tile at the given coords and zoom level
   * 
   * @return null if there is no data to display on the tile.
   */
  public BufferedImage getTile(long timestamp, long bucketspan, int bucketcount, double timedecay, Scaler scaler, long tileX, long tileY, int zoom, Radiator radiator, int[] palette, double opacity) {
    
    double decay = 1.0 / (2 + (Math.pow(zoom,5)));
    
    if (tileX < 0) {
      tileX += 1 << zoom;
    }
    
    if (tileY < 0) {
      tileY += 1 << zoom;
    }
    
    //
    // Compute top right bottom left coords of tile
    //
    
    long left = tileX * TILE_WIDTH;
    long top = tileY * TILE_HEIGHT;
  
    long right = left + TILE_WIDTH - 1;
    long bottom = top + TILE_HEIGHT - 1;
    
    //
    // Extend area by half radiator size
    //
    
    int radiatorHalfWidth = 0; // radiator.getWidth() / 2;
    int radiatorHalfHeight = 0; // radiator.getHeight() / 2;
    
    long outerLeft = left - radiatorHalfWidth;
    long outerTop = top - radiatorHalfHeight;
    long outerRight = right + radiatorHalfWidth;
    long outerBottom = bottom + radiatorHalfHeight;
    
    //
    // Compute lat/lng for top/bottom/right/left
    //
    
    double[] nw = GoogleMaps.XY2LatLon(outerLeft, outerTop, zoom);
    double[] se = GoogleMaps.XY2LatLon(outerRight, outerBottom, zoom);
    
    //
    // Fetch data
    //
    
    double[] data = fetchData(nw[0],nw[1],se[0],se[1], zoom, timestamp, bucketspan, bucketcount, timedecay);

    if (0 == data.length) {
      return null;
    }
    
    //
    // Build HeatMap
    //
        
    HeatMap map = new HeatMap((int) (outerRight - outerLeft + 1), (int) (outerBottom - outerTop + 1));
    
    //
    // Radiate points
    //

    if (null != data && data.length > 0 && data.length % 3 == 0) {
      int i = 0;

      do {
        // Extract lat/lon and intensity
        double lat = data[i++];
        double lon = data[i++];
        double intensity = data[i++];
        
        // Compute x,y
        
        long[] coords = GoogleMaps.LatLon2XY(lat, lon, zoom);

        //
        // Scale intensity
        //
        
        intensity = scaler.scale(intensity);

        while (intensity > 1.0) {
          map.radiate((int) (coords[0] - outerLeft), (int) (coords[1] - outerTop), radiator, 1.0);
          intensity -= 1.0;
        }
     
        map.radiate((int) (coords[0] - outerLeft), (int) (coords[1] - outerTop), radiator, intensity);
        
      } while (i < data.length);      
    }
    
    // Return the tile's content (trimming the size)
    
    BufferedImage bi = map.getImage(radiatorHalfWidth, radiatorHalfHeight, TILE_WIDTH, TILE_HEIGHT, palette, opacity);

    return bi; 
  }
  
  /**
   * Fetch the data that covers an extended tile (tile + a border of width half that of the radiator).
   * 
   * @param nwLat Extended Tile's NorthWest corner latitude
   * @param nwLon Extended Tile's NorthWest corner longitude
   * @param seLat Extended Tile's SouthEast corner latitude
   * @param seLon Extended Tile's SouthEast corner longitude
   * @param zoom Zoom level
   * @param vpSwLat SW latitude of viewport
   * @param vpSwLon SW longitude of viewport
   * @param vpNeLat NE latitude of viewport
   * @param vpNeLon NE longitude of viewport
   * @return The fetched data as an array of double, each group of 3 doubles being lat, lon, intensity
   */
  public abstract double[] fetchData(double nwLat, double nwLon, double seLat, double seLon, int zoom, long timestamp, long bucketspan, int bucketcount, double timedecay);
}
