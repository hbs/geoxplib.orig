package com.geocoord.server;

import com.google.inject.Inject;

public class ServiceFactory {
  private static ServiceFactory singleton = null;
  
  private ServiceFactory() {}
  
  /*
   * For each service, define:
   *
   * private XxxService.Iface xxxService = null;
   *
   * @Inject
   * public void setXxxService(XxxService.Iface xxxService) { this.xxxService = xxxService; }
   * public XxxService.Iface getXxxService() { return this.xxxService; }
   */ 

  public static ServiceFactory getInstance() {    
    if (null == singleton) {
      singleton = new ServiceFactory();
    }
    return singleton;
  }
}
