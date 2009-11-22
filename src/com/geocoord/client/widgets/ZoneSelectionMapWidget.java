package com.geocoord.client.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.MapOptions;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.control.LargeMapControl;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.event.MapZoomEndHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.LatLngBounds;
import com.google.gwt.maps.client.overlay.Polygon;
import com.google.gwt.user.client.ui.Composite;

public class ZoneSelectionMapWidget extends Composite implements MapZoomEndHandler, MapClickHandler {
  
  
  private double LAT_DEG_PER_UNIT = 180.0 / (1L << 32);
  private double LON_DEG_PER_UNIT = 360.0 / (1L << 32);
  
  /**
   * Current zone log
   */
  private int log = 32;
  
  public ZoneSelectionMapWidget() {
    
    MapOptions options = MapOptions.newInstance();

    MapWidget map = new MapWidget();
    map.addControl(new LargeMapControl());
    initWidget(map);
    this.setStylePrimaryName(ZoneSelectionMapWidget.class.getName().replaceAll(".*\\.", ""));
    this.init();
  }
  
  public void init() {
    MapWidget map = (MapWidget) this.getWidget();
    
    map.setWidth("640px");
    map.setHeight("400px");
    
    map.checkResize();
    map.clearOverlays();
    
    map.addMapZoomEndHandler(this);
    map.addMapClickHandler(this);
  }
  
  public void onZoomEnd(MapZoomEndEvent event) {
    //
    // Recompute the size of covering zones
    //
    
    MapWidget map = event.getSender();

    LatLngBounds bounds = map.getBounds();
    
    LatLng ne = bounds.getNorthEast();
    LatLng sw = bounds.getSouthWest();
    
    //
    // Compute lat/lon delta
    //
    
    long latDelta = Math.round((ne.getLatitude() - sw.getLatitude()) / LAT_DEG_PER_UNIT);
    long lonDelta = Math.round((ne.getLongitude() - sw.getLongitude()) / LON_DEG_PER_UNIT);
    
    log = (int) Math.floor(Math.min(Math.log(latDelta), Math.log(lonDelta))/Math.log(2.0));
    // Make log an even number.
    log = log & 0xfe;

    log -= 2;
    
    if (log >= 32) {
      log = 30;
    }
  }
  
  public void onClick(MapClickEvent event) {
    
    MapWidget map = ((MapWidget) this.getWidget());
    
    if (null != event.getOverlay()) {
      map.removeOverlay(event.getOverlay());
      return;
    }
    
    LatLng latlon = event.getLatLng();
        
    //
    // Add an overlay for the enclosing zone
    //
        
    // HHCode for latlon
    
    GWT.log(latlon.getLatitude() + ":" + latlon.getLongitude(), null);
    
    long lat = Math.round((latlon.getLatitude() + 90.0) / LAT_DEG_PER_UNIT);
    long lon = Math.round((latlon.getLongitude() + 180.0) / LON_DEG_PER_UNIT);
        
    // Trim according to the current zone log
    lat = lat & (0xffffffff ^ ((1L << log) - 1));
    lon = lon & (0xffffffff ^ ((1L << log) - 1));
    
    // Determine ne corner of zone
    long nelat = lat | ((1L << log) - 1);
    long nelon = lon | ((1L << log) - 1);
    
    LatLng[] bounds = new LatLng[5];
    bounds[0] = LatLng.newInstance(-90.0 + lat * LAT_DEG_PER_UNIT, -180.0 + lon * LON_DEG_PER_UNIT);
    bounds[1] = LatLng.newInstance(-90.0 + nelat * LAT_DEG_PER_UNIT, -180.0 + lon * LON_DEG_PER_UNIT);
    bounds[2] = LatLng.newInstance(-90.0 + nelat * LAT_DEG_PER_UNIT, -180.0 + nelon * LON_DEG_PER_UNIT);
    bounds[3] = LatLng.newInstance(-90.0 + lat * LAT_DEG_PER_UNIT, -180.0 + nelon * LON_DEG_PER_UNIT);
    bounds[4] = bounds[0];
    
    GWT.log("" + bounds, null);
    
    Polygon poly = new Polygon(bounds, "#808080", 1, 0.7, "#ffaa19", 0.5);
    map.addOverlay(poly);
  }
}
