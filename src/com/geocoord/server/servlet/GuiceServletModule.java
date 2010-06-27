package com.geocoord.server.servlet;

import com.geocoord.server.servlet.api.AtomServlet;
import com.geocoord.server.servlet.api.LayerServlet;
import com.geocoord.server.servlet.api.SearchServlet;
import com.geocoord.twitter.TwitterCallbackServlet;
import com.geocoord.twitter.TwitterLoginServlet;
import com.google.inject.servlet.ServletModule;

public class GuiceServletModule extends ServletModule {
  @Override
  protected void configureServlets() {
    filterRegex("/api/v0/(layer|atom|search).*").through(OAuthFilter.class);
    serve("/api/v0/layer/.*").with(LayerServlet.class);
    serve("/api/v0/atom/.*").with(AtomServlet.class);
    serve("/api/v0/search").with(SearchServlet.class);
    serve("/api/v0/layar").with(LayarGetPointsOfInterestServlet.class);
    serve("/api/v0/layar/.*").with(LayarGetPointsOfInterestServlet.class);
    serve("/twitter/login").with(TwitterLoginServlet.class);
    serve("/twitter/callback").with(TwitterCallbackServlet.class);
  }
}
