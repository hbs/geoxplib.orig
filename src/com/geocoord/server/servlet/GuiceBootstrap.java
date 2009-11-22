package com.geocoord.server.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.geocoord.server.GuiceModule;
import com.geocoord.server.ServiceFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class GuiceBootstrap extends HttpServlet {
  @Override
  public void init() throws ServletException {
    //
    // Initialize an injector and inject into a DAOFactory
    //

    Injector injector = Guice.createInjector(new GuiceModule());

    ServiceFactory factory = ServiceFactory.getInstance();
    injector.injectMembers(factory);
  }
}
