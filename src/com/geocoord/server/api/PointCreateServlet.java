//package com.geocoord.server.api;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.apache.thrift.TException;
//
//import com.geocoord.geo.HHCodeHelper;
//import com.geocoord.server.ServiceFactory;
//import com.geocoord.thrift.data.Constants;
//import com.geocoord.thrift.data.GeoCoordException;
//import com.geocoord.thrift.data.Layer;
//import com.geocoord.thrift.data.Point;
//import com.geocoord.thrift.data.PointStoreRequest;
//
//public class PointCreateServlet extends GeoCoordAPIServlet {
//  
//  @Override
//  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//    doPost(req, resp);
//  }
//  
//  @Override
//  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//    
//    try {
//      if (!checkParams(req, resp)) {
//        return;
//      }
//  
//      //
//      // Retrieve Layer and check signature
//      //
//      
//      Layer layer = null;
//      
//      String id = req.getParameter(Constants.API_PARAM_ID);
//      
//      try {
//        layer = ServiceFactory.getInstance().getLayerService().load("gclid:" + id);
//      } catch (TException te) {
//        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//        return;
//      } catch (GeoCoordException gce) {
//        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_ID");
//        return;
//      }
//      
//      if (!checkSignature(req, resp, layer.getHmacKey())) {
//        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_SIGNATURE");
//        return;
//      }
//
//      //
//      // If there is a 'count' parameter, we are creating that many points.
//      //
//      
//      int count = 0;
//
//      if (null != req.getParameter(Constants.API_PARAM_POINT_CREATE_COUNT)) {
//        try {
//          count = Integer.valueOf(req.getParameter(Constants.API_PARAM_POINT_CREATE_COUNT));
//        } catch (NumberFormatException nfe) {
//          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_POINT_COUNT");
//          return;
//        }
//      }
//      
//      //
//      // Loop over params
//      //
//      
//      int idx = 0;
//
//      PointStoreRequest request = new PointStoreRequest();
//      
//      do {
//        String gcname = req.getParameter((count > 0 ? ("" + idx + "") : "") + Constants.API_PARAM_POINT_NAME);
//        
//        String gclat = req.getParameter((count > 0 ? ("" + idx + "") : "") + Constants.API_PARAM_POINT_LAT);
//        String gclon = req.getParameter((count > 0 ? ("" + idx + "") : "") + Constants.API_PARAM_POINT_LON);
//
//        long hhcode = 0L;
//
//        try {
//          hhcode = HHCodeHelper.getHHCodeValue(Double.valueOf(gclat), Double.valueOf(gclon));
//        } catch (NumberFormatException nfe) {
//          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_PARAMETER");
//          return;
//        }
//
//        Point point = new Point();
//      
//        // Set layer
//        point.setGclid(layer.getGclid());
//        
//        if (null != gcname) {
//          point.setGcname(gcname);
//        }
//        point.setHhcode(hhcode);
//        
//        // Add point to request
//        request.addToPoints(point);
//        
//        idx++;
//      } while (idx < count);
//      
//      //
//      // Issue the storage request
//      //
//      
//      
//    } finally {
//      
//    }
//  }
//}
