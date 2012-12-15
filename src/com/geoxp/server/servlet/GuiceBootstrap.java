package com.geoxp.server.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.geoxp.server.GuiceModule;
import com.geoxp.server.ServiceFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class GuiceBootstrap extends HttpServlet {
  @Override
  public void init() throws ServletException {
    //
    // Initialize an injector and inject into a DAOFactory
    //

    try {
    Injector injector = Guice.createInjector(new GuiceModule());

    ServiceFactory factory = ServiceFactory.getInstance();
    injector.injectMembers(factory);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
