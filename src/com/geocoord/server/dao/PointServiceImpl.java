package com.geocoord.server.dao;

import org.apache.thrift.TException;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.services.PointService;

public class PointServiceImpl implements PointService.Iface {
  public Point store(Point point) throws GeoCoordException, TException {
    return null;
  }  
}
