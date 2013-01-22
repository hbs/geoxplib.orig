package com.geoxp.server.service;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.services.ActivityService;

public class ActivityServiceMock implements ActivityService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(ActivityServiceMock.class);
  
  @Override
  public void record(ActivityEvent event) throws GeoCoordException, TException {
    logger.debug(event.toString());
  }
}
