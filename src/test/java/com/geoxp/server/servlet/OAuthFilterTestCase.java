package com.geoxp.server.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserCreateRequest;
import com.geocoord.thrift.data.UserCreateResponse;
import com.geoxp.server.ServiceFactory;
import com.geoxp.server.servlet.GuiceBootstrap;
import com.geoxp.server.servlet.OAuthFilter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class OAuthFilterTestCase {
  
  private static Server server;

  @Singleton
  private static class TestDummyServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      resp.setStatus(HttpServletResponse.SC_OK);
    }
  }
  
  private static class TestGuiceServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
      filterRegex("/api/v([0-9])+/.*").through(OAuthFilter.class);
      serve("/*").with(TestDummyServlet.class);
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
  public void testOAuthFilter_InvalidConsumerKey() throws Exception {
    //
    // Issue a call with the wrong key
    //

    Connector connector = server.getConnectors()[0];
    
    OAuthServiceProvider sp = new OAuthServiceProvider("","","");
    OAuthConsumer consumer = new OAuthConsumer(null, "USER", "BAD", sp);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    OAuthMessage message = new OAuthMessage(OAuthMessage.POST, "http://" + connector.getHost() + ":" + connector.getLocalPort() + "/api/v1/echo", new ArrayList<Entry<String,String>>());
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
      client.invoke(message, ParameterStyle.BODY);
    } catch (OAuthProblemException e) {
      t = e;
    } finally {
      Assert.assertNotNull(t);
      Assert.assertTrue(t instanceof OAuthProblemException);
      Assert.assertEquals("HTTP/1.1 500 Server Error", t.getMessage());
    }
  }
  
  @Test
  public void testOAuthFilter_InvalidConsumerSecret() throws Exception {
    //
    // Store a User
    //
    
    UserCreateRequest request = new UserCreateRequest();
    User user = new User();
    request.setUser(user);
    UserCreateResponse ucr = ServiceFactory.getInstance().getUserService().create(request);
    user = ucr.getUser();
    
    //
    // Issue a call with the wrong key
    //

    Connector connector = server.getConnectors()[0];
    
    OAuthServiceProvider sp = new OAuthServiceProvider("","","");
    OAuthConsumer consumer = new OAuthConsumer(null, user.getUserId(), user.getSecret() + "BAD", sp);
    OAuthAccessor accessor = new OAuthAccessor(consumer);
    OAuthMessage message = new OAuthMessage(OAuthMessage.POST, "http://" + connector.getHost() + ":" + connector.getLocalPort() + "/api/v1/echo", new ArrayList<Entry<String,String>>());
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
      client.invoke(message, ParameterStyle.BODY);
    } catch (OAuthProblemException e) {
      t = e;
    } finally {
      Assert.assertNotNull(t);
      Assert.assertTrue(t instanceof OAuthProblemException);
      Assert.assertEquals("HTTP/1.1 500 signature_invalid", t.getMessage());
    }
  }
}
