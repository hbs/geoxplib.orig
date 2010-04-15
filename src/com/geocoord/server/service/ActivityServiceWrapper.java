package com.geocoord.server.service;

import org.apache.thrift.TException;

import com.geocoord.server.GuiceModule.ActivityIndexing;
import com.geocoord.server.GuiceModule.ActivityStorage;
import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.services.ActivityService;
import com.google.inject.Inject;

public class ActivityServiceWrapper implements ActivityService.Iface {
  
  private final ActivityService.Iface storageService;
  private final ActivityService.Iface indexingService;
  
  @Inject
  public ActivityServiceWrapper(@ActivityStorage ActivityService.Iface storageService, @ActivityIndexing ActivityService.Iface indexingService) {
    this.storageService = storageService;
    this.indexingService = indexingService;
  }
  
  @Override
  public void record(ActivityEvent event) throws GeoCoordException, TException {
    storageService.record(event);
    indexingService.record(event);
  }
}
