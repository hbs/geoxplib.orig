package com.geoxp.server.servlet;

import com.geoxp.heatmap.HeatMapManagerTileBuilder;
import com.geoxp.heatmap.TileServlet;
import com.geoxp.server.servlet.api.AtomServlet;
import com.geoxp.server.servlet.api.LayerServlet;
import com.geoxp.server.servlet.api.SearchServlet;
import com.geoxp.twitter.TwitterCallbackServlet;
import com.geoxp.twitter.TwitterLoginServlet;
import com.google.inject.servlet.ServletModule;

public class GuiceServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    serve("/heatmap").with(TileServlet.class);
    /*
    filterRegex("/api/v0/(layer|atom|search)/(.)*").through(OAuthFilter.class);
    serveRegex("/api/v0/layer/(.)*").with(LayerServlet.class);
    serve("/api/v0/atom/search").with(SearchServlet.class);
    serveRegex("/api/v0/atom/(.)*").with(AtomServlet.class);
    serve("/api/v0/search").with(SearchServlet.class);
    serve("/api/v0/layar").with(LayarGetPointsOfInterestServlet.class);
    serveRegex("/api/v0/layar/(.)*").with(LayarGetPointsOfInterestServlet.class);
    serve("/twitter/login").with(TwitterLoginServlet.class);
    serve("/twitter/callback").with(TwitterCallbackServlet.class);
    */
  }
}
