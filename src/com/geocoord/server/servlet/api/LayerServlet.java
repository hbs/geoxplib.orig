package com.geocoord.server.servlet.api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.Cookie;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerCreateRequest;
import com.geocoord.thrift.data.LayerCreateResponse;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.User;
import com.geocoord.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.inject.Singleton;

/**
 * Handle layer creation/retrieval
 */
@Singleton
public class LayerServlet extends HttpServlet {
  
  private static final Logger logger = LoggerFactory.getLogger(LayerServlet.class);
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doPost(req, resp);
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    //
    // Check OAuth token
    //
    
    Object consumer = req.getAttribute(Constants.SERVLET_REQUEST_ATTRIBUTE_CONSUMER);
    
    if (!(consumer instanceof User)) {
      // Invalid consumer, bail out
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    
    //
    // Extract verb
    //
    
    String verb = req.getPathInfo();
    
    if (null == verb) {
      // Invalid verb, bail out
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    //
    // Dispatch according to verb
    //
    
    if ("/create".equals(verb)) {
      doCreate(req, resp, (User) consumer);
    } else if ("/retrieve".equals(verb)) {
      doRetrieve(req, resp, (User) consumer);
    } else if ("/update".equals(verb)) {
      doUpdate(req, resp, (User) consumer);
    } else if ("/remove".equals(verb)) {
      doRemove(req, resp, (User) consumer);
    } else {
      // Invalid verb, bail out
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return; 
    }
  }
  
  /**
   * Create a layer.
   * 
   * HTTP Params are
   * 
   * name     Name to give the layer
   * public   Whether the layer is public, 'true' or 'false', defaults to 'true'
   * indexed  Whether the layer is indexed, 'true' or 'false', defaults to 'true'
   * 
   * @param req
   * @param resp
   * @param user
   */
  private void doCreate(HttpServletRequest req, HttpServletResponse resp, User user) {
    //
    // Name parameter is mandatory
    //
    
    if (null == req.getParameter("name")) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    //
    // Create layer
    //
    
    LayerCreateRequest request = new LayerCreateRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(user.getUserId());
    request.setCookie(cookie);
    
    Layer layer = new Layer();
    layer.setLayerId(req.getParameter("name"));
    
    request.setLayer(layer);
    
    // Layers are by default indexed
    if (null != req.getParameter("indexed") && "false".equals(req.getParameter("indexed"))) {
      layer.setIndexed(false);
    } else {
      layer.setIndexed(true);
    }
    
    // Layers are by default public
    if (null != req.getParameter("public") && "false".equals(req.getParameter("public"))) {
      layer.setPublicLayer(false);
    } else {
      layer.setPublicLayer(true);
    }
    
    // Attempt to create the layer
    
    try {
      LayerCreateResponse response = ServiceFactory.getInstance().getLayerService().create(request);
      
      //
      // Output the result as JSON
      //
            
      resp.setContentType("application/json");
      resp.getWriter().append(JsonUtil.toJson(response.getLayer()).toString());
    } catch (IOException ioe) {
      logger.error("doCreate", ioe);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (GeoCoordException gce) {
      try {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, gce.getCode().toString());
      } catch (IOException ioe) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } catch (TException te) {
      logger.error("doCreate", te);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Retrieve a layer
   * 
   * HTTP Param
   * 
   * name Name of layer to retrieve infos for. If no authentified as the user who created the layer, will fail.
   * 
   * @param req
   * @param resp
   * @param user
   */
  private void doRetrieve(HttpServletRequest req, HttpServletResponse resp, User user) {
    //
    // Name parameter is mandatory
    //
    
    if (null == req.getParameter("name")) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    //
    // Retrieve layer
    //
    
    LayerRetrieveRequest request = new LayerRetrieveRequest();
    request.setLayerId(req.getParameter("name"));
    
    try {
      LayerRetrieveResponse response = ServiceFactory.getInstance().getLayerService().retrieve(request);
      
      // If the layer's userid is not the requesting user, bail out
      
      if (!response.getLayer().getUserId().equals(user.getUserId())) {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      
      //
      // Output the result as JSON
      //
            
      resp.setContentType("application/json");
      resp.getWriter().append(JsonUtil.toJson(response.getLayer()).toString());
      
    } catch (IOException ioe) {
      logger.error("doRetrieve", ioe);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (GeoCoordException gce) {
      try {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, gce.getCode().toString());
      } catch (IOException ioe) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } catch (TException te) {
      logger.error("doRetrieve", te);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  private void doUpdate(HttpServletRequest req, HttpServletResponse resp, User user) {
    resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);    
  }

  private void doRemove(HttpServletRequest req, HttpServletResponse resp, User user) {
    resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
  }
}
