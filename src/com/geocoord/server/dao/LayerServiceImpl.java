package com.geocoord.server.dao;

import org.apache.thrift.TException;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.LayerAdminRequest;
import com.geocoord.thrift.data.LayerAdminResponse;
import com.geocoord.thrift.services.LayerService;

public class LayerServiceImpl implements LayerService.Iface {
  public LayerAdminResponse admin(LayerAdminRequest request) throws GeoCoordException, TException {
    return null;
  }
}
