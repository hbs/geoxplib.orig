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
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerClearRequest;
import com.geocoord.thrift.data.LayerClearResponse;
import com.geocoord.thrift.data.LayerCreateRequest;
import com.geocoord.thrift.data.LayerCreateResponse;
import com.geocoord.thrift.data.LayerRemoveRequest;
import com.geocoord.thrift.data.LayerRemoveResponse;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.LayerUpdateRequest;
import com.geocoord.thrift.data.LayerUpdateResponse;
import com.geocoord.thrift.data.User;
import com.geocoord.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Singleton;

/**
 * Handle layer creation/retrieval
 */
@Singleton
public class LayerServlet extends HttpServlet {
  
  private static final Logger logger = LoggerFactory.getLogger(LayerServlet.class);
  
  private static final String HTTP_PARAM_LAYER = "layer";
  private static final String HTTP_PARAM_NAME = "name";
  private static final String HTTP_PARAM_INDEXED = "indexed";
  private static final String HTTP_PARAM_PUBLIC = "public";
  
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
      if (consumer instanceof User) {
        doUpdate(req, resp, (User) consumer);
      } else if (consumer instanceof Layer) {
        doUpdate(req, resp, (Layer) consumer);
      }
    } else if ("/remove".equals(verb)) {
      doRemove(req, resp, (User) consumer);
    } else if ("/clear".equals(verb)) {
      doClear(req, resp, (User) consumer);
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
   * layer  JSON Object representing the layer to create
   *
   * @param req
   * @param resp
   * @param user
   */
  private void doCreate(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
    //
    // Name parameter is mandatory
    //
    
    if (null == req.getParameter(HTTP_PARAM_LAYER)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing " + HTTP_PARAM_LAYER + " parameter.");
      return;
    }
    
    //
    // Create layer
    //
    
    LayerCreateRequest request = new LayerCreateRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(user.getUserId());
    request.setCookie(cookie);
    
    Layer layer = JsonUtil.layerFromJson(req.getParameter(HTTP_PARAM_LAYER));
    
    if (null == layer) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.LAYER_INVALID_FORMAT.toString());
      return;
    }

    //
    // Check that the layer name is in an allowed namespace
    //
    
    boolean nsok = false;
    
    if (user.getLayerNamespacesSize() > 0) {
      for (String ns: user.getLayerNamespaces()) {
        if (layer.getLayerId().startsWith(ns)) {
          nsok = true;
          break;
        }
      }
    }
    
    if (!nsok) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.LAYER_INVALID_NAMESPACE.toString());
      return;      
    }
    
    request.setLayer(layer);
    
    // Attempt to create the layer
    
    try {
      LayerCreateResponse response = ServiceFactory.getInstance().getLayerService().create(request);
      
      //
      // Output the result as JSON
      //
            
      resp.setContentType("application/json");
      resp.setCharacterEncoding("utf-8");
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
    // Retrieve layer
    //
    
    LayerRetrieveRequest request = new LayerRetrieveRequest();
    
    // Don't retrieve proxied layer
    request.setRetrieveProxied(false);

    request.setLayerId(req.getParameter("name"));

    // If no layer name was specified, retrieve per user layers
    if (null == request.getLayerId()) {
      request.setUserId(user.getUserId());
    }
    
    try {
      LayerRetrieveResponse response = ServiceFactory.getInstance().getLayerService().retrieve(request);
      
      // If the layer's userid is not the requesting user, bail out
      
      if (0 == response.getLayersSize()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.LAYER_NOT_FOUND.toString());
        return;        
      }
            
      //
      // Output the result as JSON
      //
            
      JsonArray layers = new JsonArray();
      
      for (Layer layer: response.getLayers()) {
        if (!layer.getUserId().equals(user.getUserId())) {
          resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }
        
        layers.add(JsonUtil.toJson(layer));
      }
      
      resp.setContentType("application/json");
      resp.setCharacterEncoding("utf-8");
      resp.getWriter().append(layers.toString());
      
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
    
    Throwable throwable = null;
    
    try {      
      //
      // Extract layer from query string
      //
      
      Layer layer = JsonUtil.layerFromJson(req.getParameter(HTTP_PARAM_LAYER));
      if (null == layer) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.LAYER_INVALID_FORMAT.toString());
        return;
      }
      
      //
      // Retrieve layer
      //
      
      LayerRetrieveRequest lrreq = new LayerRetrieveRequest();
      
      // Don't retrieve proxied layer
      lrreq.setRetrieveProxied(false);
      
      lrreq.setLayerId(layer.getLayerId());
      
      LayerRetrieveResponse response = ServiceFactory.getInstance().getLayerService().retrieve(lrreq);
        
      // If the layer's userid is not the requesting user, bail out
        
      if (!response.getLayers().get(0).getUserId().equals(user.getUserId())) {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
  
      doUpdate(req, resp, response.getLayers().get(0));
    } catch (TException te) {
      logger.error("doUpdate", te);
      throwable = new GeoCoordException(GeoCoordExceptionCode.LAYER_ERROR);
    } catch (GeoCoordException gce) {
      throwable = gce;
    } catch (IOException ioe) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } finally {
      if (null != throwable) {
        try {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, throwable.toString());
        } catch (IOException ioe) {
          
        }
      }
    }    
  }
  
  private void doUpdate(HttpServletRequest req, HttpServletResponse resp, Layer layer) {
    Throwable t = null;
    
    try {
      //
      // Extract layer from the query string
      //
      
      Layer jlayer = JsonUtil.layerFromJson(req.getParameter(HTTP_PARAM_LAYER));
      
      if (null == jlayer) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.LAYER_INVALID_FORMAT.toString());
        return;
      }

      //
      // Check that the authenticating layer is the same one we are attempting to modify
      //
      
      if (!jlayer.getLayerId().equals(layer.getLayerId())) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.LAYER_ERROR.toString());
        return;        
      }
      
      //
      // Set 'updatable' fields
      //
      
      // Attributes
      layer.setAttributes(jlayer.getAttributes());
      
      // Public/Indexed 
      layer.setIndexed(jlayer.isIndexed());
      layer.setPublicLayer(jlayer.isPublicLayer());
      
      // Secret
      layer.setSecret(jlayer.getSecret());
      
      //
      // Do the update
      //
      
      LayerUpdateRequest lureq = new LayerUpdateRequest();
      lureq.setLayer(layer);
      LayerUpdateResponse luresp = ServiceFactory.getInstance().getLayerService().update(lureq);
      
      //
      // Output new value of layer.
      //
      
      resp.setContentType("application/json");
      resp.setCharacterEncoding("utf-8");
      resp.getWriter().append(JsonUtil.toJson(luresp.getLayer()).toString());
    } catch (TException te) {
      logger.error("doUpdate", te);
      t = new GeoCoordException(GeoCoordExceptionCode.LAYER_ERROR);
    } catch (GeoCoordException gce) {
      t = gce;
    } catch (IOException ioe) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } finally {
      if (null != t) {
        try {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, t.toString());
        } catch (IOException ioe) {          
        }
      }      
    }        
  }

  private void doRemove(HttpServletRequest req, HttpServletResponse resp, User user) {
    //
    // Retrieve layer
    //
    
    LayerRetrieveRequest request = new LayerRetrieveRequest();
    
    request.setRetrieveProxied(false);
    
    request.setLayerId(req.getParameter("name"));
    
    // If no layer name was specified, bail out
    if (null == request.getLayerId()) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    try {
      LayerRetrieveResponse response = ServiceFactory.getInstance().getLayerService().retrieve(request);
      
      // If the layer's userid is not the requesting user, bail out      
      if (1 != response.getLayersSize()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.LAYER_NOT_FOUND.toString());
        return;        
      }
      
      Layer layer = response.getLayers().get(0);
      
      if (!layer.getUserId().equals(user.getUserId())) {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }

      LayerRemoveRequest lrr = new LayerRemoveRequest();
      lrr.setLayer(layer);

      LayerRemoveResponse lrresp = ServiceFactory.getInstance().getLayerService().remove(lrr);
      
      //
      // Output old value of layer.
      //
      
      resp.setContentType("application/json");
      resp.setCharacterEncoding("utf-8");
      resp.getWriter().append(JsonUtil.toJson(lrresp.getLayer()).toString());
    } catch (IOException ioe) {
      logger.error("doRemove", ioe);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (GeoCoordException gce) {
      try {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, gce.getCode().toString());
      } catch (IOException ioe) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } catch (TException te) {
      logger.error("doRemove", te);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
  
  private void doClear(HttpServletRequest req, HttpServletResponse resp, User user) {
    //
    // Retrieve layer
    //
    
    LayerRetrieveRequest request = new LayerRetrieveRequest();
    
    request.setRetrieveProxied(false);
    
    request.setLayerId(req.getParameter("name"));
    
    // If no layer name was specified, bail out
    if (null == request.getLayerId()) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    try {
      LayerRetrieveResponse response = ServiceFactory.getInstance().getLayerService().retrieve(request);
      
      if (1 != response.getLayersSize()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.LAYER_NOT_FOUND.toString());
        return;        
      }
      
      Layer layer = response.getLayers().get(0);
      
      // If the layer's userid is not the requesting user, bail out      
      if (!layer.getUserId().equals(user.getUserId())) {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }

      // If the layer is a proxy, bail out too
      if (null != layer.getProxyFor()) {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      
      LayerClearRequest lcr = new LayerClearRequest();
      lcr.setLayer(layer);

      LayerClearResponse lrresp = ServiceFactory.getInstance().getLayerService().clear(lcr);
      
      //
      // Output old value of layer.
      //
      
      resp.setContentType("application/json");
      resp.setCharacterEncoding("utf-8");
      resp.getWriter().append(JsonUtil.toJson(lrresp.getLayer()).toString());
    } catch (IOException ioe) {
      logger.error("doClear", ioe);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (GeoCoordException gce) {
      try {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, gce.getCode().toString());
      } catch (IOException ioe) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } catch (TException te) {
      logger.error("doClear", te);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }    
  }
}
