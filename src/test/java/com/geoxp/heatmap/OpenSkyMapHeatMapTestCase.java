package com.geoxp.heatmap;

import net.iroise.commons.test.JettyHelper;

import org.eclipse.jetty.server.Server;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class OpenSkyMapHeatMapTestCase {
  private static Server server;

  private static class TestGuiceServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
      serve("/tile").with(TileServlet.class);
    }
  }
  
  private static class TestGuiceServletConfig extends GuiceServletContextListener {
    @Override
    protected Injector getInjector() {
      return Guice.createInjector(new TestGuiceServletModule());
    }
  }
  
  @BeforeClass
  public static void setUp() throws Exception {    
    
    //
    // Setup a Jetty Server
    //
    
    server = JettyHelper.startServer(new TestGuiceServletConfig(), 8080);
    server.start();

    System.out.println("LISTENING ON PORT " + server.getConnectors()[0].getLocalPort());    
  }

  @Test
  public void test() throws Exception {
    while (true) {
      Thread.sleep(1000);
    }
  }
}
