package net.iroise.commons.test;

import java.util.EventListener;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.inject.servlet.GuiceFilter;

public class JettyHelper {
  
  /**
   * Start a Jetty Server
   * 
   * @param listener An EventListener to add to the servlet context.
   * @param port The port the server should be started on. Use 0 to allocate it dynamically.
   * @return The started Server instance. The port it listens on can be retrieve via a call to getLocalPort.
   */
  public static final Server startServer(EventListener listener, int port) throws Exception {
    //
    // Setup a Jetty Server
    //
    
    Server server = new Server();
    Connector connector = new SelectChannelConnector();
    connector.setHost("0.0.0.0");
    connector.setPort(0);
    server.setConnectors(new Connector[]{connector});
    ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
    context.addFilter(GuiceFilter.class, "/*", 0);
    context.addEventListener(listener);
    //
    // DefaultServlet needs to be added for '/' (not '/*'!!!)
    //
    context.addServlet(DefaultServlet.class, "/");
    server.start();
    
    return server;
  }
}
