package com.geoxp.heatmap;

import com.google.inject.servlet.ServletModule;

public class GuiceServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    serve("/heatmap").with(TileServlet.class);
    serve("/update/*").with(UpdateServlet.class);
  }
  
  
}
