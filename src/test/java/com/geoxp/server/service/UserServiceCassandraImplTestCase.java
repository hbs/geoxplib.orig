package com.geoxp.server.service;

import junit.framework.Assert;

import org.apache.cassandra.service.EmbeddedCassandraService;
import org.junit.BeforeClass;
import org.junit.Test;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.UserAliasRequest;
import com.geocoord.thrift.data.UserAliasResponse;
import com.geocoord.thrift.data.UserCreateRequest;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserCreateResponse;
import com.geocoord.thrift.data.UserRetrieveRequest;
import com.geocoord.thrift.data.UserRetrieveResponse;
import com.geoxp.server.ServiceFactory;
import com.geoxp.server.servlet.GuiceBootstrap;


public class UserServiceCassandraImplTestCase {

  private static GuiceBootstrap gbs = null;
  
  @BeforeClass
  public static void init() throws Exception {
    gbs = new GuiceBootstrap();
    gbs.init();    
    
    //
    // Create a Cassandra embedded service
    //
    
    try {
      System.setProperty("storage-config", "test/conf");
      EmbeddedCassandraService cassandra = new EmbeddedCassandraService();
      cassandra.init();
      Thread t = new Thread(cassandra);
      t.setDaemon(true);
      t.start();    
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  @Test
  public void testCreate() throws Exception {
    UserCreateRequest req = new UserCreateRequest();
    User user = new User();
    req.setUser(user);
    
    UserCreateResponse resp = ServiceFactory.getInstance().getUserService().create(req);
    
    Assert.assertTrue(resp.isSetUser());
    Assert.assertTrue(resp.getUser().isSetSecret());
    Assert.assertTrue(resp.getUser().isSetUserId());
    Assert.assertTrue(0 != resp.getUser().getMaxLayers());
  }
  
  @Test
  public void testAlias() throws Exception {
    //
    // Create a new user
    //
    
    UserCreateRequest req = new UserCreateRequest();
    User user = new User();
    req.setUser(user);
    
    UserCreateResponse resp = ServiceFactory.getInstance().getUserService().create(req);
    
    //
    // Request that an alias be added for this user
    //
    
    UserAliasRequest areq = new UserAliasRequest();
    areq.setUserId(resp.getUser().getUserId());
    areq.setAlias("twitter:geoxpcom");
    
    UserAliasResponse aresp = ServiceFactory.getInstance().getUserService().alias(areq);
  }
  
  @Test
  public void testUserRetrieve_ById_Unknown() {
    //
    // Attempt to retrieve user
    //
    
    Throwable t = null;
    
    try {
      UserRetrieveRequest ureq = new UserRetrieveRequest();
      ureq.setUserId("foo");
    
      UserRetrieveResponse uresp = ServiceFactory.getInstance().getUserService().retrieve(ureq);
    } catch (Exception e) {
      t = e;
    } finally {
      Assert.assertTrue(null != t);
      Assert.assertTrue(t instanceof GeoCoordException);
      Assert.assertEquals(GeoCoordExceptionCode.USER_NOT_FOUND, ((GeoCoordException) t).getCode());
    }
  }
  
  @Test
  public void testUserRetrieve_ById_Ok() throws Exception {
    
    //
    // Create a new user
    //
    
    UserCreateRequest req = new UserCreateRequest();
    User user = new User();
    req.setUser(user);
    
    UserCreateResponse resp = ServiceFactory.getInstance().getUserService().create(req);

    //
    // Attempt to retrieve user
    //
    
    UserRetrieveRequest ureq = new UserRetrieveRequest();
    ureq.setUserId(user.getUserId());
    
    UserRetrieveResponse uresp = ServiceFactory.getInstance().getUserService().retrieve(ureq);
    
    Assert.assertEquals(resp.getUser(), uresp.getUser());
  }
  
  @Test
  public void testUserRetrieve_ByAlias() throws Exception {
    //
    // Create a new user
    //
    
    UserCreateRequest req = new UserCreateRequest();
    User user = new User();
    req.setUser(user);
    
    UserCreateResponse resp = ServiceFactory.getInstance().getUserService().create(req);
    
    //
    // Request that an alias be added for this user
    //
    
    UserAliasRequest areq = new UserAliasRequest();
    areq.setUserId(resp.getUser().getUserId());
    areq.setAlias("twitter:geoxpcom");
    
    UserAliasResponse aresp = ServiceFactory.getInstance().getUserService().alias(areq);

    //
    // Now retrieve by alias
    //
    
    UserRetrieveRequest ureq = new UserRetrieveRequest();
    ureq.setAlias("twitter:geoxpcom");
    
    UserRetrieveResponse uresp = ServiceFactory.getInstance().getUserService().retrieve(ureq);
    

    // Check that we retrieved the correct user
    Assert.assertEquals(resp.getUser(), uresp.getUser());
  }

}
