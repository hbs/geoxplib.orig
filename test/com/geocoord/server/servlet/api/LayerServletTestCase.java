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
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserCreateRequest;
import com.geocoord.thrift.data.UserCreateResponse;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class LayerServletTestCase {
  
  private static Server server;
  
  private static class TestGuiceServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
      filterRegex("/api/v0/(.*)").through(OAuthFilter.class);
      serve("/api/v0/layer/*").with(LayerServlet.class);
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
  public void testCreate() throws Exception {
    String name = "com.geoxp.test.layerservlettestcase.testcreate";

    //
    // Create user
    //
    
    UserCreateRequest request = new UserCreateRequest();
    User user = new User();
    request.setUser(user);
    UserCreateResponse response = ServiceFactory.getInstance().getUserService().create(request);
    user = response.getUser();
    
    //
    // Create a layer via API
    //
    
    Connector connector = server.getConnectors()[0];
    
    OAuthServiceProvider sp = new OAuthServiceProvider("","","");
    OAuthConsumer consumer = new OAuthConsumer(null, user.getUserId(), user.getSecret(), sp);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    
    Map<String,String> params = new HashMap<String, String>();
    
    params.put("name", name);
    params.put("indexed", "false");
    params.put("public", "false");
    
    OAuthMessage message = new OAuthMessage(OAuthMessage.POST, "http://" + connector.getHost() + ":" + connector.getLocalPort() + "/api/v0/layer/create", params.entrySet());
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
      
      // Convert it to Json
      JsonElement json = new JsonParser().parse(jstring);
      Assert.assertTrue(json instanceof JsonObject);
      JsonObject jo = json.getAsJsonObject();
      Assert.assertEquals("false", jo.get("indexed").getAsString());
      Assert.assertEquals("false", jo.get("public").getAsString());
      Assert.assertEquals(name, jo.get("name").getAsString());
      
      //
      // Retrieve layer
      //
      
      LayerRetrieveRequest lreq = new LayerRetrieveRequest();
      lreq.setLayerId(name);
      
      LayerRetrieveResponse lresp = ServiceFactory.getInstance().getLayerService().retrieve(lreq);
      
      Assert.assertEquals(lresp.getLayer().getLayerId(), jo.get("name").getAsString());
      Assert.assertEquals(lresp.getLayer().getSecret(), jo.get("secret").getAsString());
      Assert.assertEquals(lresp.getLayer().isIndexed(), !"false".equals(jo.get("indexed").getAsString()));
      Assert.assertEquals(lresp.getLayer().isPublicLayer(), !"false".equals(jo.get("public").getAsString()));
    } catch (OAuthProblemException e) {
      t = e;
      t.printStackTrace();
    } finally {
      Assert.assertNull(t);
    }
  }
  
  @Test
  public void testRetrieve() throws Exception {
    String name = "com.geoxp.test.layerservlettestcase.testretrieve";

    //
    // Create user
    //
    
    UserCreateRequest request = new UserCreateRequest();
    User user = new User();
    request.setUser(user);
    UserCreateResponse response = ServiceFactory.getInstance().getUserService().create(request);
    user = response.getUser();
    
    //
    // Create a layer via API
    //
    
    Connector connector = server.getConnectors()[0];
    
    OAuthServiceProvider sp = new OAuthServiceProvider("","","");
    OAuthConsumer consumer = new OAuthConsumer(null, user.getUserId(), user.getSecret(), sp);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    
    Map<String,String> params = new HashMap<String, String>();
    
    params.put("name", name);
    params.put("indexed", "false");
    params.put("public", "false");
    
    OAuthMessage message = new OAuthMessage(OAuthMessage.POST, "http://" + connector.getHost() + ":" + connector.getLocalPort() + "/api/v0/layer/create", params.entrySet());
    message.addRequiredParameters(accessor);

    // FIXME(hbs): use a pool in real life
    OAuthClient client = new OAuthClient(new HttpClient4());    
    
    Throwable t = null;
    
    // If parameters are passed as QUERY_STRING, a NPE will
    // be thrown if the invoke leads to an exception being thrown
    // as the body is retrieved but is null...
    // TODO(hbs): need to file a bug with oauth, done #153 http://code.google.com/p/oauth/issues/detail?id=153&colspec=ID%20Type%20Status%20Priority%20Lib%20Owner%20Summary
    //
    OAuthMessage resp = client.invoke(message, ParameterStyle.BODY);

    String s1 = OAuthMessage.readAll(resp.getBodyAsStream(), resp.getBodyEncoding());
    
    //
    // Retrieve layer
    //

    params.clear();
    params.put("name", name);
    
    message = new OAuthMessage(OAuthMessage.POST, "http://" + connector.getHost() + ":" + connector.getLocalPort() + "/api/v0/layer/retrieve", params.entrySet());
    message.addRequiredParameters(accessor);
      
    resp = client.invoke(message, ParameterStyle.BODY);

    String s2 = OAuthMessage.readAll(resp.getBodyAsStream(), resp.getBodyEncoding());
    
    Assert.assertEquals(s1, s2);
  }
}
