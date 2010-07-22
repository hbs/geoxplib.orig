package com.geocoord.server.servlet;

import java.util.ArrayList;

import net.iroise.commons.test.CassandraHelper;
import net.iroise.commons.test.JettyHelper;

import org.eclipse.jetty.server.Server;
import org.junit.BeforeClass;
import org.junit.Test;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.server.ServiceFactory;
import com.geocoord.server.servlet.api.AtomServlet;
import com.geocoord.server.servlet.api.LayerServlet;
import com.geocoord.server.servlet.api.SearchServlet;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomStoreRequest;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.Cookie;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerCreateRequest;
import com.geocoord.thrift.data.LayerCreateResponse;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserCreateRequest;
import com.geocoord.thrift.data.UserCreateResponse;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class LayarGetPointsOfInterestServletTestCase {

  private static Server server;

  private static class TestGuiceServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
      serve("/api/v0/layar").with(LayarGetPointsOfInterestServlet.class);
      filterRegex("/api/v0/(atom|layer|search).*").through(OAuthFilter.class);
      serve("/api/v0/atom/search").with(SearchServlet.class);
      serve("/api/v0/atom/*").with(AtomServlet.class);
      serve("/api/v0/layer/*").with(LayerServlet.class);
      serve("/api/v0/search/*").with(SearchServlet.class);
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
    //
    // Setup a Cassandra instance
    //
    
    CassandraHelper.start();
    
    //
    // Initialize injector
    //
    
    GuiceBootstrap gbs = new GuiceBootstrap();
    gbs.init();
    
    //
    // Create User / Layer / Atoms
    //
    
    //
    // Create user
    //
    
    String layerName = "com.geoxp.layar.test.0000";

    UserCreateRequest request = new UserCreateRequest();
    User user = new User();
    //user.addToLayerNamespaces("net.bakadesho");
    request.setUser(user);
    UserCreateResponse response = ServiceFactory.getInstance().getUserService().create(request);
    user = response.getUser();
        
    System.out.println(user);
    
    //
    // Create a layer
    //

    LayerCreateRequest lreq = new LayerCreateRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(user.getUserId());
    
    Layer layer = new Layer();
    layer.setLayerId(layerName);
    layer.setIndexed(true);    
    layer.putToAttributes(Constants.LAYER_ATTR_LAYAR_OAUTH_SECRET, new ArrayList<String>() {{ add("myoauthsecret"); add("myothersecret"); }});
    lreq.setLayer(layer);
    lreq.setCookie(cookie);

    try {
      LayerCreateResponse lresp = ServiceFactory.getInstance().getLayerService().create(lreq);
      layer = lresp.getLayer();
    } catch (GeoCoordException gce) {
      // Attempt to read layer
      LayerRetrieveRequest lrr = new LayerRetrieveRequest();
      lrr.setLayerId(layer.getLayerId());
      LayerRetrieveResponse lrresp = ServiceFactory.getInstance().getLayerService().retrieve(lrr);
      layer = lrresp.getLayers().get(0);
    }

    System.out.println(layer);
    
    //
    // Store/index points
    //
    
    long nano = System.nanoTime();
    
    for (int i = 0; i < 100; i++) {
      
      final Point point = new Point();
      point.setHhcode(HHCodeHelper.getHHCodeValue(48.0 + i * 0.00001 * (i % 2 == 0 ? 1 : -1), -4.5 + i *0.00001 * (i % 2 == 0 ? 1 : -1)));
      point.setPointId("point-" + i);
      point.setLayerId(layer.getLayerId());
      point.setAltitude(0);
      point.putToAttributes("layar.title", new ArrayList<String>() {{ add("Title - " + point.getPointId()); }});
      point.putToAttributes("layar.line2", new ArrayList<String>() {{ add("Line 2"); }});
      point.putToAttributes("layar.line3", new ArrayList<String>() {{ add("Line 3"); }});
      point.putToAttributes("layar.line4", new ArrayList<String>() {{ add("Line 4"); }});
      point.putToAttributes("layar.attribution", new ArrayList<String>() {{ add("[GeoXP] Attribution"); }});
      point.putToAttributes("layar.actions", new ArrayList<String>() {{ add("[]"); }});
      point.putToAttributes("layar.imageURL", new ArrayList<String>() {{ add("http://farm1.static.flickr.com/1/buddyicons/60822044@N00.jpg?1263041345#60822044@N00"); }});
      point.putToAttributes("layar.type", new ArrayList<String>() {{ add("0"); }});
      point.putToAttributes("layar.dimension", new ArrayList<String>() {{ add("1"); }});
      point.putToAttributes("~layar", new ArrayList<String>() {{ add("true"); }});
      
      Atom atom = new Atom();
      atom.setType(AtomType.POINT);
      atom.setPoint(point);
      
      AtomStoreRequest sreq = new AtomStoreRequest();
      sreq.setCookie(cookie);
      sreq.setAtom(atom);
      
      ServiceFactory.getInstance().getAtomService().store(sreq);      
    }
    
    System.out.println("Stored points in " + (System.nanoTime() - nano) / 1000000.0 + " ms.");
  }

  @Test
  public void testLoop() {
    while (true) {
      try {      
        Thread.sleep(3600000L);
      } catch (InterruptedException ie) {
        
      }      
    }
  }
}
