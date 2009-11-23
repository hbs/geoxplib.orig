package com.geocoord.server.servlet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.thrift.data.gwt.CoverageRequest;
import com.geocoord.thrift.data.gwt.CoverageResponse;
import com.geocoord.thrift.services.gwt.CoverageService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class CoverageServiceServlet extends RemoteServiceServlet implements CoverageService.Iface {
  public CoverageResponse getCoverage(CoverageRequest request) {
    //
    // Build list of HHCode from lat/lon
    //
    
    List<Long> hhcodes = new ArrayList<Long>();
    
    for (int i = 0; i < request.getPathSize(); i += 2) {
      hhcodes.add(HHCodeHelper.getHHCodeValue(request.getPath().get(i), request.getPath().get(i+1)));
    }
    
    // Get coverage
    Map<Integer,List<Long>> coverage = HHCodeHelper.coverPolygon(hhcodes, request.getResolution());
    
    // Optimize coverage
    long thresholds = Long.valueOf(request.getThreshold(), 16);
    HHCodeHelper.optimize(coverage, thresholds);
    
    //
    // Record coverage in CoverageResponse
    //
    
    CoverageResponse response = new CoverageResponse();
    
    for (int resolution: coverage.keySet()) {
      if (!coverage.get(resolution).isEmpty()) {
        for (long hhcode: coverage.get(resolution)) {
          String key = HHCodeHelper.toString(hhcode, resolution);
          response.putToCells(key, new ArrayList<Double>());
          double[] sw = HHCodeHelper.getLatLon(hhcode, resolution);
          response.getCells().get(key).add(sw[0]);
          response.getCells().get(key).add(sw[1]);
          double[] ne = HHCodeHelper.getLatLon(hhcode | ((1L << (2 * (32 - resolution))) - 1), 32);
          response.getCells().get(key).add(ne[0]);
          response.getCells().get(key).add(ne[1]);
        }
      }
    }
    
    System.out.println(response.getCells());
    return response;
  }
}
