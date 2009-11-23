package com.geocoord.client;

import com.geocoord.thrift.services.gwt.CoverageService;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

//import com.geocoord.thrift.services.gwt.XxxService;

public class GeoCoordServices {

  private static CoverageService.IfaceAsync coverageService = null;

  public static CoverageService.IfaceAsync getCoverageService() {
    if (null == coverageService) {
      CoverageService.IfaceAsync service = (CoverageService.IfaceAsync) GWT.create(CoverageService.Iface.class);
      ServiceDefTarget endpoint = (ServiceDefTarget) service;
      // Endpoint for the service, i.e. /module/xxx
      endpoint.setServiceEntryPoint(GWT.getModuleBaseURL() + "coverage");
      coverageService = service;
    }
    
    return coverageService;
  }

  //private static XxxService.IfaceAsync xxxService = null;
  
  /*
  public static XxxService.IfaceAsync getXxxService() {
    if (null == xxxService) {
      XxxService.IfaceAsync service = (XxxService.IfaceAsync) GWT.create(XxxService.Iface.class);
      ServiceDefTarget endpoint = (ServiceDefTarget) service;
      // Endpoint for the service, i.e. /module/xxx
      endpoint.setServiceEntryPoint(GWT.getModuleBaseURL() + "xxx");
      xxxService = service;
    }
    
    return xxxService;
  }
  */
}
