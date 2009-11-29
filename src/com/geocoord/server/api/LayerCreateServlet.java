package com.geocoord.server.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;

import com.geocoord.server.ServiceFactory;
import com.geocoord.server.dao.DB;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.LayerAdminRequest;
import com.geocoord.thrift.data.LayerAdminRequestType;
import com.geocoord.thrift.data.LayerAdminResponse;
import com.geocoord.thrift.data.User;
import com.geocoord.util.CryptoUtil;

public class LayerCreateServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doPost(req, resp);
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    try {
      //
      // Check for mandatory parameters
      //
      
      if (null == req.getParameter(Constants.API_PARAM_TS)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_TIMESTAMP");      
      }
      
      if (null == req.getParameter(Constants.API_PARAM_ID)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_ID");
      }

      if (null == req.getParameter(Constants.API_PARAM_SIG)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_SIGNATURE");
      }

      if (null == req.getParameter(Constants.API_PARAM_LAYER_CREATE_NAME)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_NAME");
      }

      if (null == req.getParameter(Constants.API_PARAM_LAYER_CREATE_PRIVACY)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_PRIVACY");
      }

      //
      // Extract timestamp, check that it's still valid
      //
      
      try {     
        long delay = System.currentTimeMillis() - Long.valueOf(req.getParameter(Constants.API_PARAM_TS));
        
        if (delay > Constants.API_SIGNATURE_TTL) {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_EXPIRED_SIGNATURE");            
        }
      } catch (NumberFormatException nfe) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_TIMESTAMP");
      }
      
      //
      // Retrieve User and check signature
      //
      
      User user = null;
      
      try {
        user = ServiceFactory.getInstance().getUserService().load("gcuid:" + req.getParameter(Constants.API_PARAM_ID));
      } catch (TException te) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      } catch (GeoCoordException gce) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_ID");
      }
      
      //
      // Check request signature
      //

      try { 
        String sig = CryptoUtil.signRequest(req, user.getHmacKey());
        
        if (!sig.equals(req.getParameter(Constants.API_PARAM_SIG))) {
          throw new GeoCoordException(GeoCoordExceptionCode.API_EXPIRED_SIGNATURE);
        }
      } catch (GeoCoordException gce) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_SIGNATURE");
      }

      //
      // Create LayerAdminRequest
      //
      
      LayerAdminRequest request = new LayerAdminRequest();
      request.setType(LayerAdminRequestType.CREATE);
      
      request.setName(req.getParameter(Constants.API_PARAM_LAYER_CREATE_NAME));
      
      String privacy = req.getParameter(Constants.API_PARAM_LAYER_CREATE_PRIVACY); 

      if ("private".equals(privacy)) {
        request.setPublicLayer(false);
      } else {
        request.setPublicLayer(true);
      }

      //
      // Issue the create request
      //
      
      LayerAdminResponse response = null;
      
      try {
        response = ServiceFactory.getInstance().getLayerService().admin(request);
      } catch (TException te) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);        
      } catch (GeoCoordException gce) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      
      //
      // Report gclid
      //

      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/plain");
      resp.getWriter().print("id:");
      resp.getWriter().print(response.getGclid());
      resp.getWriter().print("\r\n");
    } finally {
      // DO NOT, I REPEAT, DO NOT FORGET TO CALL RECYCLE!!!!!!
      try { DB.recycle(); } catch (GeoCoordException gce) {}
    }
  }
}
