package com.geocoord.server.servlet;

import com.geocoord.server.servlet.api.LayerServlet;
import com.google.inject.servlet.ServletModule;

public class GuiceServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    filterRegex("/api/v0/.*").through(OAuthFilter.class);
    serve("/api/v0/layer/.*").with(LayerServlet.class);
  }
}
