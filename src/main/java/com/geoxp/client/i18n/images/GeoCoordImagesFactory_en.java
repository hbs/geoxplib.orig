package com.geoxp.client.i18n.images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Localizable;

public class GeoCoordImagesFactory_en implements Localizable {
  public GeoCoordImages createImages() {
    return (GeoCoordImages) GWT.create(GeoCoordImages_en.class);
  }
}
