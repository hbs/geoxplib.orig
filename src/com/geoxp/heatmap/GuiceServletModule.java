package com.geoxp.heatmap;

import com.geoxp.geo.KMLServlet;
import com.google.inject.servlet.ServletModule;

public class GuiceServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    serve("/heatmap").with(TileServlet.class);
    serve("/update/*").with(UpdateServlet.class);
    serve("/kml").with(KMLServlet.class);
  }
  
  
}
