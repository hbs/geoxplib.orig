package com.geocoord.server;

import com.geocoord.thrift.services.UserService;
import com.google.inject.Inject;

public class ServiceFactory {
  private static final ServiceFactory singleton = new ServiceFactory();
  
  private ServiceFactory() {}
  
  public static ServiceFactory getInstance() {    
    return singleton;
  }
  
  private UserService.Iface userDAO = null;
  
  @Inject
  public void injectSlotDAO(UserService.Iface userDAO) {
    this.userDAO = userDAO;
  }
  
  public UserService.Iface getUserDAO() {
    return singleton.userDAO;
  }
}
