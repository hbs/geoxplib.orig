package com.geocoord.server;

import com.geocoord.thrift.services.CentroidService;
import com.geocoord.thrift.services.PointService;
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

  private PointService.Iface pointService = null;
  
  private CentroidService.Iface centroidService = null;
  
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

  @Inject
  public void injectPointService(PointService.Iface pointService) {
    this.pointService = pointService;
  }
  
  public PointService.Iface getPointService() {
    return this.pointService;
  }
  
  @Inject
  public void injectCentroidService(CentroidService.Iface centroidService) {
    this.centroidService = centroidService;
  }
  
  public CentroidService.Iface getCentroidService() {
    return this.centroidService;
  }
}
