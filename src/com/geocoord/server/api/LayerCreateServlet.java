package com.geocoord.server.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;

import com.geocoord.server.ServiceFactory;
import com.geocoord.server.dao.DB;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.LayerAdminRequest;
import com.geocoord.thrift.data.LayerAdminRequestType;
import com.geocoord.thrift.data.LayerAdminResponse;
import com.geocoord.thrift.data.User;

/**
 * API servlet that creates a layer. The call must be signed by a HMAC-SHA256 using
 * a user's key.
 */
public class LayerCreateServlet extends GeoCoordAPIServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doPost(req, resp);
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    try {
      if (!checkParams(req, resp)) {
        return;
      }
      
      if (null == req.getParameter(Constants.API_PARAM_LAYER_CREATE_NAME)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_NAME");
        return;
      }

      if (null == req.getParameter(Constants.API_PARAM_LAYER_CREATE_PRIVACY)) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_PRIVACY");
        return;
      }
      
      //
      // Retrieve User and check signature
      //
      
      User user = null;
      
      String id = req.getParameter(Constants.API_PARAM_ID);
      
      try {
        user = ServiceFactory.getInstance().getUserService().load("gcuid:" + id);
      } catch (TException te) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      } catch (GeoCoordException gce) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_ID");
        return;
      }
      
      if (!checkSignature(req, resp, user.getHmacKey())) {
        return;
      }
      
      //
      // Check the number of layers this user has already created
      //
      
      LayerAdminRequest request = new LayerAdminRequest();
      request.setGcuid(id);
      request.setType(LayerAdminRequestType.COUNT);
      
      LayerAdminResponse response = null;

      try {
        response = ServiceFactory.getInstance().getLayerService().admin(request);
      } catch (TException te) {
        te.printStackTrace();
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      } catch (GeoCoordException gce) {
        gce.printStackTrace();
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      }

      
      //
      // Check that the max is not yet reached
      //
      
      if (response.getCount() >= user.getMaxLayers()) {
        resp.sendError(HttpServletResponse.SC_FORBIDDEN, "API_TOO_MANY_LAYERS");
        return;
      }
      
      //
      // Create LayerAdminRequest
      //
      
      request = new LayerAdminRequest();      
      request.setType(LayerAdminRequestType.CREATE);
      
      request.setGcuid(id);
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
      
      response = null;
      
      try {
        response = ServiceFactory.getInstance().getLayerService().admin(request);
      } catch (TException te) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      } catch (GeoCoordException gce) {
        gce.printStackTrace();
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
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
