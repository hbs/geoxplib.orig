package com.geoxp.geo;

import com.google.inject.servlet.ServletModule;

public class GuiceServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    serve("/kml").with(KMLServlet.class);
  }
  
  
}
