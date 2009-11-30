package com.geocoord.server;

import com.geocoord.thrift.services.UserService;
import com.geocoord.thrift.services.LayerService;
import com.google.inject.Inject;

public class ServiceFactory {
  private static final ServiceFactory singleton = new ServiceFactory();
  
  private ServiceFactory() {}
  
  public static ServiceFactory getInstance() {    
    return singleton;
  }
  
  private UserService.Iface userService = null;

  private LayerService.Iface layerService = null;

  @Inject
  public void injectUserService(UserService.Iface userService) {
    this.userService = userService;
  }
  
  public UserService.Iface getUserService() {
    return this.userService;
  }

  @Inject
  public void injectLayerService(LayerService.Iface layerService) {
    this.layerService = layerService;
  }
  
  public LayerService.Iface getLayerService() {
    return this.layerService;
  }

}
