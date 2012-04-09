package com.geocoord.server.servlet.api;

import java.util.ArrayList;
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
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserCreateRequest;
import com.geocoord.thrift.data.UserCreateResponse;
import com.geocoord.util.JsonUtil;
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
    user.addToLayerNamespaces("com.geoxp.test");
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
    
    Layer layer = new Layer();
    layer.setLayerId(name);
    layer.setPublicLayer(false);
    layer.setIndexed(false);
    layer.setSecret("");
    layer.putToAttributes("foo", new ArrayList<String>() {{ add("bar"); }});
    
    params.put("layer", JsonUtil.toJson(layer).toString());
    
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
      
      // Parse Layer
      Layer respLayer = JsonUtil.layerFromJson(jstring);
      
      Assert.assertEquals(layer.isIndexed(), respLayer.isIndexed());
      Assert.assertEquals(layer.isPublicLayer(), respLayer.isPublicLayer());
      Assert.assertEquals(layer.getLayerId(), respLayer.getLayerId());      
      Assert.assertEquals(layer.getAttributes(), respLayer.getAttributes());

      //
      // Retrieve layer
      //
      
      LayerRetrieveRequest lreq = new LayerRetrieveRequest();
      lreq.setLayerId(name);
      
      LayerRetrieveResponse lresp = ServiceFactory.getInstance().getLayerService().retrieve(lreq);

      Assert.assertEquals(1, lresp.getLayersSize());
      Layer retrLayer = lresp.getLayers().get(0);

      Assert.assertEquals(layer.isIndexed(), retrLayer.isIndexed());
      Assert.assertEquals(layer.isPublicLayer(), retrLayer.isPublicLayer());
      Assert.assertEquals(layer.getLayerId(), retrLayer.getLayerId());      
      Assert.assertEquals(layer.getAttributes(), retrLayer.getAttributes());
      Assert.assertEquals(respLayer.getSecret(), retrLayer.getSecret());

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
    user.addToLayerNamespaces("com.geoxp.test");
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
    
    Layer layer = new Layer();
    layer.setLayerId(name);
    layer.setSecret("");
    layer.setPublicLayer(false);
    layer.setIndexed(false);
    
    params.put("layer", JsonUtil.toJson(layer).toString());
    
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
    
    Assert.assertEquals("[" + s1 + "]", s2);
  }
  
  @Test
  public void testUpdate() throws Exception {
    String name = "com.geoxp.test.layerservlettestcase.testupdate";

    //
    // Create user
    //
    
    UserCreateRequest request = new UserCreateRequest();
    User user = new User();
    user.addToLayerNamespaces("com.geoxp.test");
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
    
    Layer layer = new Layer();
    layer.setLayerId(name);
    layer.setSecret("");
    layer.setPublicLayer(false);
    layer.setIndexed(false);
    
    params.put("layer", JsonUtil.toJson(layer).toString());
    
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

    OAuthMessage.readAll(resp.getBodyAsStream(), resp.getBodyEncoding());

    // Retrieve via Service the created layer
    LayerRetrieveRequest retrieveReq = new LayerRetrieveRequest();
    retrieveReq.setLayerId(layer.getLayerId());
    Layer createdLayer = ServiceFactory.getInstance().getLayerService().retrieve(retrieveReq).getLayers().get(0);
    
    //
    // Update layer
    //
    
    layer.putToAttributes("foo", new ArrayList<String>() {{ add("bar"); }});
    layer.setIndexed(true);
    layer.setPublicLayer(true);
    layer.setSecret("0123456789ABCDEF");
    
    params.clear();
    params.put("layer", JsonUtil.toJson(layer).toString());
    message = new OAuthMessage(OAuthMessage.POST, "http://" + connector.getHost() + ":" + connector.getLocalPort() + "/api/v0/layer/update", params.entrySet());
    message.addRequiredParameters(accessor);
    client = new OAuthClient(new HttpClient4());    
    resp = client.invoke(message, ParameterStyle.BODY);
    String s2 = OAuthMessage.readAll(resp.getBodyAsStream(), resp.getBodyEncoding());

    // Retrieve updated layer
    retrieveReq = new LayerRetrieveRequest();
    retrieveReq.setLayerId(layer.getLayerId());
    Layer updatedLayer = ServiceFactory.getInstance().getLayerService().retrieve(retrieveReq).getLayers().get(0);

    System.out.println(createdLayer);
    System.out.println(updatedLayer);
    
    //
    // Make sure some elements where NOT updated
    //
    
    Assert.assertEquals(createdLayer.getUserId(), updatedLayer.getUserId());
    Assert.assertEquals(createdLayer.getLayerId(), updatedLayer.getLayerId());
    Assert.assertEquals(createdLayer.getGeneration(), updatedLayer.getGeneration());
    
    //
    // Check that the rest was updated
    //
    
    Assert.assertEquals(layer.isIndexed(), updatedLayer.isIndexed());
    Assert.assertEquals(layer.isPublicLayer(), updatedLayer.isPublicLayer());
    Assert.assertEquals(layer.getAttributes(), updatedLayer.getAttributes());
    Assert.assertEquals(layer.getSecret(), updatedLayer.getSecret());
    Assert.assertEquals(layer.isDeleted(), updatedLayer.isDeleted());
  }
}
