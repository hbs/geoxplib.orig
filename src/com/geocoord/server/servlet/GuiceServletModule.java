package com.geocoord.server.servlet;

import com.geocoord.server.servlet.api.AtomServlet;
import com.geocoord.server.servlet.api.LayerServlet;
import com.geocoord.server.servlet.api.SearchServlet;
import com.google.inject.servlet.ServletModule;

public class GuiceServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    filterRegex("/api/v0/.*").through(OAuthFilter.class);
    serve("/api/v0/layer/.*").with(LayerServlet.class);
    serve("/api/v0/atom/.*").with(AtomServlet.class);
    serve("/api/v0/search").with(SearchServlet.class);
    serve("/layarPOI").with(LayarGetPointsOfInterestServlet.class);
  }
}
