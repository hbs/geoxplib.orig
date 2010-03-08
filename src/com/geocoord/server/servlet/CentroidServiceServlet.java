package com.geocoord.server.servlet;

import org.apache.thrift.GWTHelper;
import org.apache.thrift.TException;

import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.gwt.CentroidRequest;
import com.geocoord.thrift.data.gwt.CentroidResponse;
import com.geocoord.thrift.data.gwt.GeoCoordException;
import com.geocoord.thrift.data.gwt.GeoCoordExceptionCode;
import com.geocoord.thrift.services.gwt.CentroidService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class CentroidServiceServlet extends RemoteServiceServlet implements CentroidService.Iface {
  public CentroidResponse search(CentroidRequest request) throws GeoCoordException {    
    try {
      return (CentroidResponse) GWTHelper.convert(ServiceFactory.getInstance().getCentroidService().search((com.geocoord.thrift.data.CentroidRequest)GWTHelper.convert(request)));
    } catch (com.geocoord.thrift.data.GeoCoordException gce) {
      GeoCoordException ggce = new GeoCoordException();
      ggce.setCode(GeoCoordExceptionCode.findByValue(gce.getCode().getValue()));
      throw ggce;
    } catch (TException te) {
      GeoCoordException ggce = new GeoCoordException();
      ggce.setCode(GeoCoordExceptionCode.THRIFT_ERROR);
      throw ggce;
    }
  }
}
