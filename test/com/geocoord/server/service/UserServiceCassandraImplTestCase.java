package com.geocoord.server.service;

import org.junit.BeforeClass;
import org.junit.Test;

import com.geocoord.server.servlet.GuiceBootstrap;


public class UserServiceCassandraImplTestCase {

  private static GuiceBootstrap gbs = null;
  
  @BeforeClass
  public static void init() throws Exception {
    gbs = new GuiceBootstrap();
    gbs.init();    
  }
  
  @Test
  public void testUserCreate() {
    
  }
  
  @Test
  public void testUserRetrieve_ByUUID() {
    
  }
  
  @Test
  public void testUserRetrieve_ByAlias() {
    
  }

}
