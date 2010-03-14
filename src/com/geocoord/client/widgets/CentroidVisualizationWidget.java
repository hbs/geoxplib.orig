package com.geocoord.client.widgets;

import java.util.HashMap;
import java.util.Map;

import com.geocoord.client.GeoCoordServices;
import com.geocoord.thrift.data.gwt.Centroid;
import com.geocoord.thrift.data.gwt.CentroidPoint;
import com.geocoord.thrift.data.gwt.CentroidRequest;
import com.geocoord.thrift.data.gwt.CentroidResponse;
import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.control.LargeMapControl3D;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.event.MapDoubleClickHandler;
import com.google.gwt.maps.client.event.MapDragEndHandler;
import com.google.gwt.maps.client.event.MapZoomEndHandler;
import com.google.gwt.maps.client.event.MarkerClickHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.LatLngBounds;
import com.google.gwt.maps.client.overlay.Icon;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.maps.client.overlay.MarkerOptions;
import com.google.gwt.maps.client.overlay.Overlay;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

public class CentroidVisualizationWidget extends Composite implements MapClickHandler, MapDoubleClickHandler, MapZoomEndHandler, MapDragEndHandler {

    private Map<Integer,Icon> icons = new HashMap<Integer, Icon>() {{
      put(1, Icon.newInstance(GWT.getHostPageBaseURL() + "images/1.png"));
      put(10, Icon.newInstance(GWT.getHostPageBaseURL() + "images/10.png"));
      put(100, Icon.newInstance(GWT.getHostPageBaseURL() + "images/100.png"));
      put(1000, Icon.newInstance(GWT.getHostPageBaseURL() + "images/1000.png"));
      put(10000, Icon.newInstance(GWT.getHostPageBaseURL() + "images/10000.png"));
      put(100000, Icon.newInstance(GWT.getHostPageBaseURL() + "images/100000.png"));            
    }};
    public CentroidVisualizationWidget() {
      
      FlowPanel fp = new FlowPanel();
      initWidget(fp);
      
      redraw();      
    }
    
    private void redraw() {
      
      FlowPanel fp = (FlowPanel) this.getWidget();
      
      fp.clear();
      
      MapWidget map = new MapWidget();
      map.addControl(new LargeMapControl3D());

      map.setWidth("640px");
      map.setHeight("400px");
      
      map.checkResize();
      map.clearOverlays();
      
      //map.addMapClickHandler(this);
      map.addMapDoubleClickHandler(this);
      map.addMapZoomEndHandler(this);
      map.addMapDragEndHandler(this);
      
      fp.add(map);
      
      map.checkResize();
    }
    
    //@Override
    public void onClick(MapClickEvent event) {
      Overlay over = event.getOverlay();
      
      if (null != over) {
        
      } else {
        // Center the map
        event.getSender().setCenter(event.getLatLng());
      }
    }
    
    //@Override
    public void onDoubleClick(MapDoubleClickEvent event) {
      refreshCentroids(event.getSender());
    }
    
    //@Override
    public void onDragEnd(MapDragEndEvent event) {
      refreshCentroids(event.getSender());
    }
    
    //@Override
    public void onZoomEnd(MapZoomEndEvent event) {
      refreshCentroids(event.getSender());
    }

    private void refreshCentroids(final MapWidget map) {
      CentroidRequest request = new CentroidRequest();
      // Cells with less than4 point will display the points directly
      request.setPointThreshold(4);
      
      LatLngBounds bbox = map.getBounds();
      request.setBottomLat(bbox.getSouthWest().getLatitude());
      request.setLeftLon(bbox.getSouthWest().getLongitude());
      request.setTopLat(bbox.getNorthEast().getLatitude());
      request.setRightLon(bbox.getNorthEast().getLongitude());
      
      AsyncCallback<CentroidResponse> callback = new AsyncCallback<CentroidResponse>() {
        //@Override
        public void onFailure(Throwable caught) {
          // TODO Auto-generated method stub
          
        }
        //@Override
        public void onSuccess(CentroidResponse result) {
          map.clearOverlays();
          // Create one marker per centroid unless there are markers, in which
          // case we create markers for them
          for (Centroid centroid: result.getCentroids()) {
            if (0 == centroid.getPointsSize()) {              
              LatLng point = LatLng.newInstance(centroid.getLat(), centroid.getLon());
              MarkerOptions options = MarkerOptions.newInstance();
              options.setBouncy(false);
              options.setClickable(true);
              options.setDraggable(false);
              options.setIcon(getIconForThreshold(centroid.getCount()));
              Marker marker = new Marker(point, options);
              final Centroid cent = centroid;
              marker.addMarkerClickHandler(new MarkerClickHandler() {
                public void onClick(MarkerClickEvent event) {
                  map.setCenter(LatLng.newInstance(cent.getLat(), cent.getLon()));
                  //LatLngBounds bounds = LatLngBounds.newInstance(LatLng.newInstance(cent.getBottomLat(), cent.getLeftLon()),
                  //                                               LatLng.newInstance(cent.getTopLat(), cent.getRightLon()));
                  map.setZoomLevel(map.getZoomLevel() + 1);
                  //refreshCentroids(map);
                }
              });
              map.addOverlay(marker);
            } else {
              for (CentroidPoint point: centroid.getPoints()) {
                LatLng pt = LatLng.newInstance(point.getLat(), point.getLon());
                MarkerOptions options = MarkerOptions.newInstance();
                options.setBouncy(false);
                options.setClickable(true);
                options.setDraggable(false);
                options.setIcon(getIconForThreshold(1));
                Marker marker = new Marker(pt, options);
                map.addOverlay(marker);                
              }              
            }
          }
        }
      };
            
      //
      // Call the centroid service.
      //
      
      GWT.log("CALLING centroid", null);
      GeoCoordServices.getCentroidService().search(request, callback);
      
    }
    
    private Icon getIconForThreshold(int count) {
      if (count < 2) {
        return icons.get(1);
      } else if (count < 10) {
        return icons.get(10);          
      } else if (count < 100) {
        return icons.get(100);        
      } else if (count < 1000) {
        return icons.get(1000);        
      } else if (count < 10000) {
        return icons.get(10000);        
      } else {
        return icons.get(100000);        
      }
    }

}
