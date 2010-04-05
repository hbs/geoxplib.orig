package com.geocoord.server.service;

import org.apache.thrift.TException;

import com.geocoord.thrift.data.CentroidRequest;
import com.geocoord.thrift.data.CentroidResponse;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.services.CentroidService;

public class CentroidServiceMock implements CentroidService.Iface {
  @Override
  public CentroidResponse search(CentroidRequest request) throws GeoCoordException, TException {
    // TODO Auto-generated method stub
    return null;
  }
}
