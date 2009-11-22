package com.geocoord.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.event.MapZoomEndHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;

import com.geocoord.client.GeoCoordRegistry;
import com.geocoord.client.i18n.images.GeoCoordImages;
import com.geocoord.client.i18n.messages.GeoCoordMessages;
import com.geocoord.client.widgets.ZoneSelectionMapWidget;

public class GeoCoord implements EntryPoint {
  private static GeoCoordImages images = null; //((GeoCoordImagesFactory) GWT.create(GeoCoordImagesFactory.class)).createImages();
  private static GeoCoordMessages messages = (GeoCoordMessages) GWT.create(GeoCoordMessages.class);
  
  public static GeoCoordImages getImages() {
    return images;
  }
  public static GeoCoordMessages getMessages() {
    return messages;
  }

  public void onModuleLoad() {
    GeoCoordRegistry.register(GeoCoordRegistry.MAIN_MODULE, this);

    // vvvvvv INSERT YOUR CODE BELOW THIS LINE vvvvvv

    HorizontalPanel hp = new HorizontalPanel();
    
    ZoneSelectionMapWidget map = new ZoneSelectionMapWidget();
    
    hp.add(map);

    RootPanel.get().add(hp);
        
    // ^^^^^^ INSERT YOUR CODE ABOVE THIS LINE ^^^^^^

    History.addValueChangeHandler(new GeoCoordHistoryHandler());
    // insert current URL fragment into history handler
    History.fireCurrentHistoryState();
  }
}
