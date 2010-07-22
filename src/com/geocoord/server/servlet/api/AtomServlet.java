package com.geocoord.server.servlet.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.geocoord.thrift.data.AtomRemoveRequest;
import com.geocoord.thrift.data.AtomRemoveResponse;
import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.data.AtomStoreRequest;
import com.geocoord.thrift.data.AtomStoreResponse;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.Cookie;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Geofence;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.data.User;
import com.geocoord.util.JsonUtil;
import com.geocoord.util.NamingUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gwt.json.client.JSONString;
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
    
    if ("/store".equals(verb)) {
      if (consumer instanceof Layer) {
        doStore(req, resp, (Layer) consumer);
      } else if (consumer instanceof User) {
        doStore(req, resp, (User) consumer);
      }
    } else if ("/retrieve".equals(verb)) {
      doRetrieve(req, resp, consumer);
    } else if ("/remove".equals(verb)) {
      doRemove(req, resp, consumer);
    } else {
      // Invalid verb, bail out
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return; 
    }
  }
  
  private void doStore(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
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


      if (0 == response.getLayersSize()) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;        
      }

      if (!user.getUserId().equals(response.getLayers().get(0).getUserId())) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return;
      }
      
      doStore(req, resp, response.getLayers().get(0));
    } catch (TException te) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;      
    } catch (GeoCoordException gce) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;      
    }
  }

  private void doStore(HttpServletRequest req, HttpServletResponse resp, Layer layer) throws IOException {
        
    //
    // Extract atom type
    //
    
    String type = req.getParameter(HTTP_PARAM_TYPE);
    
    AtomType atomType = null;
    
    if ("point".equalsIgnoreCase(type)) {
      atomType = AtomType.POINT;
    } else if ("geofence".equalsIgnoreCase(type)) {
      atomType = AtomType.GEOFENCE;
    }
    
    if (null == atomType) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.ATOM_UNSUPPORTED_TYPE.toString());
      return;
    }
   
    //
    // Extract each atom, check that it is valid
    //
    
    String[] atoms = req.getParameterValues(HTTP_PARAM_ATOM);
   
    if (null == atoms) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.ATOM_MISSING_PARAMETER.toString());
      return;      
    }
    
    List<Atom> allAtoms = new ArrayList<Atom>();
    
    for (String atom: atoms) {

      switch (atomType) {
        case POINT:
          // Attempt to parse atom as JSON      
          Point point = JsonUtil.pointFromJson(atom);

          // Invalid point
          if (null == point) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.ATOM_INVALID_FORMAT.toString());
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
          break;
        case GEOFENCE:
          Geofence geofence = JsonUtil.geofenceFromJson(atom);
          
          // Invalid geofence
          if (null == geofence) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.ATOM_INVALID_FORMAT.toString());
            return;                          
          }
          
          geofence.setLayerId(layer.getLayerId());
          geofence.setUserId(layer.getUserId());
          
          atm = new Atom();
          atm.setGeofence(geofence);
          atm.setType(AtomType.GEOFENCE);
          atm.setTimestamp(System.currentTimeMillis());
          
          atm.setIndexed(layer.isIndexed());
          allAtoms.add(atm);
          break;
      }
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
        switch (atomType) {
          case POINT:
            jsonAtoms.add(JsonUtil.toJson(response.getAtom().getPoint()));
            break;
          case GEOFENCE:
            jsonAtoms.add(JsonUtil.toJson(response.getAtom().getGeofence()));
            break;
        }
      } catch (TException te) {       
      } catch (GeoCoordException gce) {        
      }
    }      
            
    //
    // Output the result as JSON
    //
          
    try {
      resp.setContentType("application/json");
      resp.setCharacterEncoding("utf-8");
      resp.getWriter().append(jsonAtoms.toString());      
    } catch (IOException ioe) {
      logger.error("doCreate", ioe);
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
  
  /**
   * Retrieve atoms. 
   * @param req
   * @param resp
   * @param consumer
   */
  private void doRetrieve(HttpServletRequest req, HttpServletResponse resp, Object consumer) {
    //
    // Extract layer
    //
    
    String layerId = req.getParameter(HTTP_PARAM_LAYER);
    
    if (null == layerId || null == req.getParameterValues(HTTP_PARAM_ATOM)) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;      
    }
    
    // If the consumer is a User, retrieve the layer
    
    Layer layer = null;
    
    try {
      
      if (consumer instanceof User) {
        LayerRetrieveRequest lrreq = new LayerRetrieveRequest();
        lrreq.setLayerId(layerId);
        
        LayerRetrieveResponse lrresp = ServiceFactory.getInstance().getLayerService().retrieve(lrreq);
        
        if (0 == lrresp.getLayersSize()) {
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;      
        }
        
        layer = lrresp.getLayers().get(0);

        // If the layer is public, it's OK to proceed, otherwise, the owner of the layer
        // MUST be the authenticated User.
        if (!layer.isPublicLayer() && !((User) consumer).getUserId().equals(layer.getUserId())) {
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;              
        }
      } else {
        layer = (Layer) consumer;
      }
      
      //
      // Retrieve all atoms
      //
      
      AtomRetrieveRequest arreq = new AtomRetrieveRequest();
      
      for (String atom: req.getParameterValues(HTTP_PARAM_ATOM)) {
        arreq.addToUuids(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(layer.getLayerId(), atom)));
      }
      
      AtomRetrieveResponse arresp = ServiceFactory.getInstance().getAtomService().retrieve(arreq);
      
      JsonArray atoms = new JsonArray();
      
      if (arresp.getAtomsSize() > 0) {
        for (Atom atom: arresp.getAtoms()) {
          switch (atom.getType()) {
            case POINT:
              atoms.add(JsonUtil.toJson(atom.getPoint()));
              break;
            case GEOFENCE:
              atoms.add(JsonUtil.toJson(atom.getGeofence()));
              break;
          }
        }
      }
      
      try {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("utf-8");
        resp.getWriter().append(atoms.toString());      
      } catch (IOException ioe) {
        logger.error("doRetrieve", ioe);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } catch (TException te) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (GeoCoordException gce) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }    

  /**
   * Remove atoms. 
   * @param req
   * @param resp
   * @param consumer
   */
  private void doRemove(HttpServletRequest req, HttpServletResponse resp, Object consumer) {
    //
    // Extract layer
    //
    
    String layerId = req.getParameter(HTTP_PARAM_LAYER);
    
    if (null == layerId || null == req.getParameterValues(HTTP_PARAM_ATOM)) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;      
    }
    
    // If the consumer is a User, retrieve the layer
    
    Layer layer = null;
    
    try {
      
      if (consumer instanceof User) {
        LayerRetrieveRequest lrreq = new LayerRetrieveRequest();
        lrreq.setLayerId(layerId);
        
        LayerRetrieveResponse lrresp = ServiceFactory.getInstance().getLayerService().retrieve(lrreq);
        
        if (0 == lrresp.getLayersSize()) {
          resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;      
        }
        
        layer = lrresp.getLayers().get(0);

        if (!((User) consumer).getUserId().equals(layer.getUserId())) {
          resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
          return;              
        }
      } else {
        layer = (Layer) consumer;
      }
      
      //
      // Remove all atoms
      //
      
      AtomRemoveRequest arreq = new AtomRemoveRequest();
      
      Map<byte[],String> atomIds = new HashMap<byte[], String>();
      
      for (String atom: req.getParameterValues(HTTP_PARAM_ATOM)) {
        byte[] uuid = NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(layer.getLayerId(), atom));
        arreq.addToUuids(uuid);
        atomIds.put(uuid, atom);
      }
      
      AtomRemoveResponse arresp = ServiceFactory.getInstance().getAtomService().remove(arreq);
      
      JsonArray atoms = new JsonArray();
      
      if (arresp.getUuidsSize() > 0) {
        for (byte[] uuid: arresp.getUuids()) {
          String id = atomIds.get(uuid);
          if (null != id) {
            atoms.add(new JsonPrimitive(id));
          }
        }
      }
      
      try {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("utf-8");
        resp.getWriter().append(atoms.toString());      
      } catch (IOException ioe) {
        logger.error("doRetrieve", ioe);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } catch (TException te) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (GeoCoordException gce) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }    

}
