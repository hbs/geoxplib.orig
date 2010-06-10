package com.geocoord.server.servlet.api;

import java.util.HashMap;
import java.util.Map;

import net.iroise.commons.test.CassandraHelper;
import net.iroise.commons.test.JettyHelper;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.OAuthServiceProvider;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.geocoord.server.ServiceFactory;
import com.geocoord.server.servlet.GuiceBootstrap;
import com.geocoord.server.servlet.OAuthFilter;
import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Cookie;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerCreateRequest;
import com.geocoord.thrift.data.LayerCreateResponse;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserCreateRequest;
import com.geocoord.thrift.data.UserCreateResponse;
import com.geocoord.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class AtomServletTestCase {
  
  private static Server server;
  
  private static class TestGuiceServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
      filterRegex("/api/v0/(.*)").through(OAuthFilter.class);
      serve("/api/v0/atom/*").with(AtomServlet.class);
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
    
    server = JettyHelper.startServer(new TestGuiceServletConfig(), 0);
    server.start();

    //
    // Setup a Cassandra instance
    //
    
    CassandraHelper.start();
    
    //
    // Initialize injector
    //
    
    GuiceBootstrap gbs = new GuiceBootstrap();
    gbs.init();    
  }
  
  @AfterClass
  public static void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void testCreate_Point() throws Exception {
    
    //
    // Create user
    //
    
    String layerName = "com.geoxp.test.atomservlettestcase.testcreate.point";

    UserCreateRequest request = new UserCreateRequest();
    User user = new User();
    request.setUser(user);
    UserCreateResponse response = ServiceFactory.getInstance().getUserService().create(request);
    user = response.getUser();
        
    //
    // Create a layer
    //

    LayerCreateRequest lreq = new LayerCreateRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(user.getUserId());
    
    Layer layer = new Layer();
    layer.setLayerId(layerName);
    layer.setIndexed(false);
    
    lreq.setLayer(layer);
    lreq.setCookie(cookie);

    LayerCreateResponse lresp = ServiceFactory.getInstance().getLayerService().create(lreq);
    layer = lresp.getLayer();
    
    //
    // Post Atom Create request
    //

    Connector connector = server.getConnectors()[0];
    
    OAuthServiceProvider sp = new OAuthServiceProvider("","","");
    OAuthConsumer consumer = new OAuthConsumer(null, user.getUserId(), user.getSecret(), sp);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    
    Map<String,String> params = new HashMap<String, String>();
    
    String atomName = "test.point";
    double atomLat = 48.0;
    double atomLon = -4.5;
    double atomAlt = 42.0;
    double atomTs = System.currentTimeMillis();
    
    params.put("layer", layer.getLayerId());
    params.put("type", "point");
    params.put("atom", "{'name':'" + atomName + "','lat':" + atomLat + ", 'lon':" + atomLon + ", 'alt':" + atomAlt + ", 'ts':" + atomTs + ",'tags':'tag0 tag1','attr':[{'name':'attr0','value':'value0'},{'name':'~attr1','value':'value1'}]}");
        
    OAuthMessage message = new OAuthMessage(OAuthMessage.POST, "http://" + connector.getHost() + ":" + connector.getLocalPort() + "/api/v0/atom/create", params.entrySet());
    message.addRequiredParameters(accessor);

    // FIXME(hbs): use a pool in real life
    OAuthClient client = new OAuthClient(new HttpClient4());    
    
    Throwable t = null;
    
    try {
      // If parameters are passed as QUERY_STRING, a NPE will
      // be thrown if the invoke leads to an exception being thrown
      // as the body is retrieved but is null...
      // TODO(hbs): need to file a bug with oauth, done #153 http://code.google.com/p/oauth/issues/detail?id=153&colspec=ID%20Type%20Status%20Priority%20Lib%20Owner%20Summary
      //
      OAuthMessage resp = client.invoke(message, ParameterStyle.BODY);
      
      // Read response
      String jstring = OAuthMessage.readAll(resp.getBodyAsStream(), resp.getBodyEncoding());
      
      JsonElement json = new JsonParser().parse(jstring);

      Assert.assertTrue(json instanceof JsonArray);
      Assert.assertEquals(1, json.getAsJsonArray().size());
      Assert.assertTrue(json.getAsJsonArray().get(0).isJsonObject());
      JsonObject jo = json.getAsJsonArray().get(0).getAsJsonObject();

      Point p = JsonUtil.pointFromJson(jo.toString());
      
      /*
      // Convert it to Json
      Assert.assertEquals("false", jo.get("indexed").getAsString());
      Assert.assertEquals("false", jo.get("public").getAsString());
      Assert.assertEquals(name, jo.get("name").getAsString());
      */
      
      //
      // Retrieve point
      //
      
      AtomRetrieveRequest areq = new AtomRetrieveRequest();
      areq.setAtom(atomName);
      areq.setLayer(layerName);
      
      AtomRetrieveResponse aresp = ServiceFactory.getInstance().getAtomService().retrieve(areq);
      
      Assert.assertEquals(AtomType.POINT, aresp.getAtom().getType());
      Point pp = aresp.getAtom().getPoint();
      Assert.assertEquals(user.getUserId(), pp.getUserId());
      Assert.assertEquals(layer.getLayerId(), pp.getLayerId());
      Assert.assertEquals(p.getAltitude(), pp.getAltitude(), 0.00000001);
      Assert.assertEquals(p.getTimestamp(), pp.getTimestamp(), 0.00000001);
      Assert.assertEquals(p.getTags(), pp.getTags());
      Assert.assertEquals(p.getHhcode(), pp.getHhcode());
      Assert.assertEquals(atomName, pp.getPointId());
      Assert.assertEquals(p.getAttributes(), pp.getAttributes());
    } catch (OAuthProblemException e) {
      t = e;
      t.printStackTrace();
    } finally {
      Assert.assertNull(t);
    }
  }
}
