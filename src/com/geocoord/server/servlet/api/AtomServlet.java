package com.geocoord.server.servlet.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.ActivityEventType;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomStoreRequest;
import com.geocoord.thrift.data.AtomStoreResponse;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.Cookie;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.data.User;
import com.geocoord.util.JsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.inject.Singleton;

@Singleton
public class AtomServlet extends HttpServlet {
  
  private static final Logger logger = LoggerFactory.getLogger(AtomServlet.class);
  
  private static final String HTTP_PARAM_TYPE = "type";
  private static final String HTTP_PARAM_ATOM = "atom";
  private static final String HTTP_PARAM_LAYER = "layer";
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    //
    // Check OAuth token
    //
    
    Object consumer = req.getAttribute(Constants.SERVLET_REQUEST_ATTRIBUTE_CONSUMER);
    
    if (null == consumer) {
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
      if (consumer instanceof Layer) {
        doCreate(req, resp, (Layer) consumer);
      } else if (consumer instanceof User) {
        doCreate(req, resp, (User) consumer);
      }
//    } else if ("/retrieve".equals(verb)) {
//      doRetrieve(req, resp, (User) consumer);
//    } else if ("/update".equals(verb)) {
//      doUpdate(req, resp, (User) consumer);
//    } else if ("/remove".equals(verb)) {
//      doRemove(req, resp, (User) consumer);
    } else {
      // Invalid verb, bail out
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return; 
    }
  }
  
  private void doCreate(HttpServletRequest req, HttpServletResponse resp, User user) {
    //
    // Check layer
    //
    
    String layerid = req.getParameter(HTTP_PARAM_LAYER);

    if (null == layerid) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
 
    //
    // Retrieve layer
    //
    
    LayerRetrieveRequest request = new LayerRetrieveRequest();
    request.setLayerId(layerid);
    
    try {
      LayerRetrieveResponse response = ServiceFactory.getInstance().getLayerService().retrieve(request);


      if (null == response.getLayer()) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;        
      }

      if (!user.getUserId().equals(response.getLayer().getUserId())) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      
      doCreate(req, resp, response.getLayer());
    } catch (TException te) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;      
    } catch (GeoCoordException gce) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;      
    }
  }
  
  private void doCreate(HttpServletRequest req, HttpServletResponse resp, Layer layer) {
        
    //
    // Extract atom type
    //
    
    String type = req.getParameter(HTTP_PARAM_TYPE);
    
    // For now only accept 'point' atoms
    if (null == type || !"point".equalsIgnoreCase(type)) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
   
    //
    // Extract each atom, check that it is valid
    //
    
    String[] atoms = req.getParameterValues(HTTP_PARAM_ATOM);
   
    if (null == atoms) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;      
    }
    
    List<Atom> allAtoms = new ArrayList<Atom>();
    
    for (String atom: atoms) {
      // Attempt to parse atom as JSON      
      Point point = JsonUtil.pointFromJson(atom);

      // Invalid point
      if (null == point) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;              
      }
      
      // Force layer/user
      point.setLayerId(layer.getLayerId());
      point.setUserId(layer.getUserId());
      
      Atom atm = new Atom();
      atm.setPoint(point);
      atm.setType(AtomType.POINT);
      atm.setTimestamp(System.currentTimeMillis());
      
      atm.setIndexed(layer.isIndexed());
      allAtoms.add(atm);
    }
    
    //
    // Now attempt to store all atoms
    //
    
    Cookie cookie = new Cookie();
    cookie.setUserId(layer.getUserId());
    
    JsonArray jsonAtoms = new JsonArray();

    for (Atom atom: allAtoms) {
      AtomStoreRequest request = new AtomStoreRequest();
      request.setCookie(cookie);
      request.setAtom(atom);
        
      try {
        AtomStoreResponse response = ServiceFactory.getInstance().getAtomService().store(request);
        jsonAtoms.add(JsonUtil.toJson(response.getAtom().getPoint()));
      } catch (TException te) {       
      } catch (GeoCoordException gce) {        
      }
    }      
            
    //
    // Output the result as JSON
    //
          
    try {
      resp.setContentType("application/json");
      resp.getWriter().append(jsonAtoms.toString());      
    } catch (IOException ioe) {
      logger.error("doCreate", ioe);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
