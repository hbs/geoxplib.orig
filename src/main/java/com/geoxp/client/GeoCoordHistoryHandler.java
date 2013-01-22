package com.geoxp.client;

import com.google.gwt.event.logical.shared.ValueChangeHandler;

public class GeoCoordHistoryHandler implements ValueChangeHandler<String> {

  public void onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent<String> event) {
    String historyToken = event.getValue();
  }
}
