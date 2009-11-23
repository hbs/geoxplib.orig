package com.geocoord.client.widgets;

import java.util.ArrayList;
import java.util.List;

import com.geocoord.client.GeoCoord;
import com.geocoord.client.GeoCoordServices;
import com.geocoord.thrift.data.gwt.CoverageRequest;
import com.geocoord.thrift.data.gwt.CoverageResponse;
import com.geocoord.thrift.data.gwt.CoverageType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Polygon;
import com.google.gwt.maps.client.overlay.PolygonOptions;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CoverageTesterWidget extends Composite implements ClickHandler, MapClickHandler {
  
  private MapWidget map = null;

  private Button clearOverlay = null;
  private Button closePolygon = null;
  private Button coverPath = null;
  private Button coverPolygon = null;
  
  private TextBox resolution = null;
  private TextBox threshold = null;
  
  private List<LatLng> path = null;
  
  public CoverageTesterWidget(MapWidget map) {
    this.map = map;
    this.path = new ArrayList<LatLng>();
    
    VerticalPanel vp = new VerticalPanel();
    initWidget(vp);
    this.setStylePrimaryName(getClass().getName().replaceAll(".*\\.", ""));

    initUI();
  }
  
  private void initUI() {
    
    map.addMapClickHandler(this);
    
    VerticalPanel vp = (VerticalPanel) getWidget();
    
    // Add a clear overlay button
    clearOverlay = new Button(GeoCoord.getMessages().coverageTesterWidgetClearOverlay());
    vp.add(clearOverlay);
    clearOverlay.addClickHandler(this);

    // Add a close polygon button
    closePolygon = new Button(GeoCoord.getMessages().coverageTesterWidgetClosePolygon());
    vp.add(closePolygon);
    closePolygon.addClickHandler(this);

    // Add a cover polygon button
    coverPolygon = new Button(GeoCoord.getMessages().coverageTesterWidgetCoverPolygon());
    vp.add(coverPolygon);
    coverPolygon.addClickHandler(this);

    // Add a cover path button
    coverPath = new Button(GeoCoord.getMessages().coverageTesterWidgetCoverPath());
    vp.add(coverPath);
    coverPath.addClickHandler(this);
    
    resolution = new TextBox();
    resolution.setText("0");
    vp.add(resolution);
    
    threshold = new TextBox();
    threshold.setText("0");
    vp.add(threshold);
  }
  
  public void onClick(ClickEvent event) {
    
    CoverageRequest request = null;
    
    if (clearOverlay == event.getSource()) {
      this.map.clearOverlays();
      this.path.clear();
    } else if (closePolygon == event.getSource()) {
      
    } else if (coverPath == event.getSource()) {
      
    } else if (coverPolygon == event.getSource()) {
      request = new CoverageRequest();
      request.setType(CoverageType.POLYGON);
    }
    
    //
    // There is a pending CoverageRequest, issue it.
    //
    
    if (null != request) {
      request.setResolution(Integer.valueOf(resolution.getText()));
      request.setThreshold(threshold.getText());      
      
      //
      // Add path
      //
      
      for (LatLng node: path) {
        request.addToPath(node.getLatitude());
        request.addToPath(node.getLongitude());
      }
      
      AsyncCallback<CoverageResponse> callback = new AsyncCallback<CoverageResponse>() {
        public void onFailure(Throwable caught) {
          Window.alert("beuh!");
        }
        public void onSuccess(CoverageResponse result) {
          processCoverageResponse(result);
        }
      };
      
      GeoCoordServices.getCoverageService().getCoverage(request, callback);
    }
  }
  
  private void processCoverageResponse(CoverageResponse response) {
    //
    // Clear overlays
    //
    
    this.map.clearOverlays();
    
    //
    // Draw the coverage
    //
    
    for (String hhcode: response.getCells().keySet()) {
      LatLng[] points = new LatLng[4];
      points[0] = LatLng.newInstance(response.getCells().get(hhcode).get(0), response.getCells().get(hhcode).get(1)); // SW
      points[1] = LatLng.newInstance(response.getCells().get(hhcode).get(0), response.getCells().get(hhcode).get(3)); // SE
      points[2] = LatLng.newInstance(response.getCells().get(hhcode).get(2), response.getCells().get(hhcode).get(3)); // NE
      points[3] = LatLng.newInstance(response.getCells().get(hhcode).get(2), response.getCells().get(hhcode).get(1)); // NW
      Polygon cell = new Polygon(points, "#404040", 2, 0.7, "#ffaa19", 0.5);
      this.map.addOverlay(cell);
    }

    //
    // Draw the original polyline
    //
    
    
    Polygon line = new Polygon(path.toArray(new LatLng[0]), "#2400c7", 2, 0.7, "#ffffff", 0);
    this.map.addOverlay(line);
  }
  
  public void onClick(MapClickEvent event) {
    this.path.add(event.getLatLng());
    
    //
    // If the path has more than 1 node, connect the last two
    //
    
    if (this.path.size() > 1) {
      LatLng[] points = new LatLng[2];
      points[0] = this.path.get(this.path.size() - 2);
      points[1] = this.path.get(this.path.size() - 1);
      this.map.addOverlay(new Polygon(points, "#808080", 2, 0.7, "", 0));
    }
  }
}
