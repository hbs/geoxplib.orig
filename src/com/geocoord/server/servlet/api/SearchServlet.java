package com.geocoord.server.servlet.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;

import com.geocoord.geo.Coverage;
import com.geocoord.geo.GeoParser;
import com.geocoord.geo.HHCodeHelper;
import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.data.SearchRequest;
import com.geocoord.thrift.data.SearchResponse;
import com.geocoord.thrift.data.SearchType;
import com.geocoord.thrift.data.User;
import com.geocoord.util.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Singleton;

@Singleton
public class SearchServlet extends HttpServlet {
  
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
    
    if ("/atoms".equals(verb)) {
      if (consumer instanceof Layer) {
        List<Layer> layers = new ArrayList<Layer>();
        layers.add((Layer) consumer);
        doSearchAtoms(req, resp, layers);
      } else if (consumer instanceof User) {
        doSearchAtoms(req, resp, (User) consumer);
      }
    //} else if ("/clusters".equals(verb)) {
      //doSearchClusters(req, resp, consumer);
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
  
  private static void doSearchAtoms(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
    //
    // We were called with a User OAuth consumer, we need to check the layers
    //
    
    //
    // Attempt to read 'q'
    //
    
    String q = req.getParameter("q");
    
    if (null == q) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_MISSING_PARAMETER.toString());
      return;
    }
    
    //
    // Attempt to convert query to JSON
    //
    
    JsonParser parser = new JsonParser();
    
    JsonObject json = parser.parse(q).getAsJsonObject();
    
    //
    // Extract layer ids, retrieve those layers
    //
    
    List<Layer> layers = new ArrayList<Layer>();
    
    if (!json.has("layers")) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_MISSING_PARAMETER.toString());
      return;      
    }
    
    Iterator<JsonElement> iter = json.getAsJsonArray("layers").iterator();
    
    LayerRetrieveRequest lrreq = new LayerRetrieveRequest();
    
    while (iter.hasNext()) {
      JsonElement elt = iter.next();
      
      lrreq.setLayerId(elt.getAsString());

      Layer layer = null;
      
      try {
        LayerRetrieveResponse lrresp = ServiceFactory.getInstance().getLayerService().retrieve(lrreq);
        layer = lrresp.getLayer();
      } catch (TException te) {        
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      } catch (GeoCoordException gce) {
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, gce.getCode().toString());
        return;
      }
      
      //
      // If the user is owner of this layer OR if it is public, add it
      //
      
      if (null == layer) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown layer " + lrreq.getLayerId());
        return;
      }
      
      if (!user.getUserId().equals(layer.getUserId()) && !layer.isPublicLayer()) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Access denied to layer " + layer.getLayerId());
        return;
      }
     
      // Add the layer if it is indexed
      if (layer.isIndexed()) {
        layers.add(layer);
      }
    }

    // Proceed with the search
    doSearchAtoms(req,resp,layers);
  }
  
  private static void doSearchAtoms(HttpServletRequest req, HttpServletResponse resp, List<Layer> layers) throws IOException {
    SearchRequest request = new SearchRequest();

    for (Layer layer: layers) {
      request.addToLayers(layer.getLayerId());
    }
    
    //
    // Attempt to read 'q'
    //
    
    String q = req.getParameter("q");
    
    if (null == q) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_MISSING_PARAMETER.toString());
      return;
    }
    
    //
    // Attempt to convert query to JSON
    //
    
    JsonParser parser = new JsonParser();
    
    JsonObject json = parser.parse(q).getAsJsonObject();
    
    //
    // Extract paging
    //
    
    if (json.has("page")) {
      request.setPage(json.get("page").getAsInt());      
    }

    if (json.has("perpage")) {
      request.setPerpage(json.get("perpage").getAsInt());      
    }

    if (!json.has("center")) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_MISSING_CENTER.toString());
      return;      
    }

    request.setCenter(GeoParser.parseLatLon(json.get("center").getAsString()));
    
    //
    // Extract area
    //
    
    if (json.has("areas")) {
      JsonArray areas = json.get("areas").getAsJsonArray();
      
      try {
        Coverage coverage = JsonUtil.coverageFromJson(areas, -2);            
        request.setArea(coverage.getAllCells());
      } catch (GeoCoordException gce) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, gce.getCode().toString());
        return;
      }
    }
    
    //
    // If no area was specified, we are looking for zones including the center.
    //
    
    if (null == request.getArea()) {
      // FIXME(hbs): handle this kind of searches
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_INVALID_AREA.toString());
      return;
    } else {
      request.setType(SearchType.DIST);
    }
    
    //
    // Extract Viewport if set
    // swlat:swlon:nelat:nelon
    //
    
    if (json.has("viewport")) {
      String[] tokens = json.get("viewport").getAsString().split(":");
      
      if (4 != tokens.length) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_INVALID_VIEWPORT.toString());
        return;        
      }
      
      for (String token: tokens) {
        try {
          request.addToViewport(Double.valueOf(token));
        } catch (NumberFormatException nfe) {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_INVALID_VIEWPORT.toString());
          return;                  
        }
      }
    }

    
    JsonObject jresp = new JsonObject();

    try {
      //
      // Issue the search
      //

      SearchResponse sresp = ServiceFactory.getInstance().getSearchService().search(request);

      //
      // Retrieve atoms
      //
      
      AtomRetrieveRequest arreq = new AtomRetrieveRequest();
      arreq.setUuid(sresp.getPointUuids());

      AtomRetrieveResponse arresp = ServiceFactory.getInstance().getAtomService().retrieve(arreq);
      
      JsonArray atoms = new JsonArray();
      
      if (arresp.getAtomsSize() > 0) {
        for (Atom atom: arresp.getAtoms()) {
          // FIXME(hbs): handle other types of atoms
          if (!AtomType.POINT.equals(atom.getType())) {
            continue;
          }
          
          Point point = atom.getPoint();
          
          atoms.add(JsonUtil.toJson(point));          
        }
      }

      jresp.add("atoms", atoms);
      jresp.addProperty("results", sresp.getTotal());      
      
      resp.setCharacterEncoding("utf-8");
      resp.setContentType("application/json");
      resp.getWriter().write(jresp.toString());
    } catch (TException te) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;                              
    } catch (GeoCoordException gce) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, gce.getCode().toString());
      return;                        
    }
    
  }
}
