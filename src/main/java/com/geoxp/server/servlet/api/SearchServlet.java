package com.geoxp.server.servlet.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;

import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Centroid;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Geofence;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.data.SearchRequest;
import com.geocoord.thrift.data.SearchResponse;
import com.geocoord.thrift.data.SearchType;
import com.geocoord.thrift.data.User;
import com.geoxp.geo.Coverage;
import com.geoxp.geo.GeoParser;
import com.geoxp.geo.HHCodeHelper;
import com.geoxp.server.ServiceFactory;
import com.geoxp.util.JsonUtil;
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
    // Dispatch according to verb
    //
    
    if (consumer instanceof Layer) {
      doSearchAtoms(req, resp, (Layer) consumer);
    } else if (consumer instanceof User) {
      doSearchAtoms(req, resp, (User) consumer);
    }
  }
  
  private static void doSearchAtoms(HttpServletRequest req, HttpServletResponse resp, Layer layer) throws IOException {

    //
    // Retrieve proxied layer if layer is a proxy
    //
    
    if (null != layer.getProxyFor()) {
      LayerRetrieveRequest request = new LayerRetrieveRequest();
      request.setRetrieveProxied(true);
      request.setLayerId(layer.getLayerId());
      try {
        LayerRetrieveResponse response = ServiceFactory.getInstance().getLayerService().retrieve(request);
        layer = response.getLayers().get(0);        
      } catch (TException te) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        return;
      } catch (GeoCoordException gce) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, gce.getCode().toString());
        return;
      }
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

    List<Layer> layers = new ArrayList<Layer>();
    layers.add(layer);
    
    doSearchAtoms(req, resp, layers, json);
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
    
    if (!json.has("layers") || !json.has("type")) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_MISSING_PARAMETER.toString());
      return;      
    }
    
    Iterator<JsonElement> iter = json.getAsJsonArray("layers").iterator();
    
    LayerRetrieveRequest lrreq = new LayerRetrieveRequest();
    // Retrieve proxied layers
    lrreq.setRetrieveProxied(true);
    
    StringBuilder sb = new StringBuilder();
    
    while (iter.hasNext()) {
      JsonElement elt = iter.next();

      String id = elt.getAsString();
      sb.setLength(0);
      sb.append(id);
      
      boolean searchRoot = id.endsWith(".");
      		
      // LayerId ended with a dot, it is a search root. We first need
      // to strip the trailing dot so we can retrieve the layer.
      
      if (searchRoot) {
        sb.setLength(sb.length() - 1);
      }

      lrreq.setLayerId(sb.toString());

      Layer layer = null;
      
      try {
        LayerRetrieveResponse lrresp = ServiceFactory.getInstance().getLayerService().retrieve(lrreq);
        if (0 != lrresp.getLayersSize()) {
          layer = lrresp.getLayers().get(0);
        }
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
     
      // Set the searchRoot flag
      layer.setSearchRoot(searchRoot);
      
      // Add the layer if it is indexed
      if (layer.isIndexed()) {
        layers.add(layer);
      }
    }

    // Proceed with the search
    doSearchAtoms(req,resp,layers, json);
  }
  
  private static void doSearchAtoms(HttpServletRequest req, HttpServletResponse resp, List<Layer> layers, JsonObject json) throws IOException {

    //
    // Dispatch according to search type
    //
    
    if ("point".equals(json.get("type").getAsString())) {
      doSearchAtomsPoint(req, resp, layers, json);
      return;
    } else if ("geofence".equals(json.get("type").getAsString())) {
      doSearchAtomsGeofence(req, resp, layers, json);
      return;
    } else if ("cluster".equals(json.get("type").getAsString())) {
      doSearchAtomsCluster(req, resp, layers, json);
    }
  }
  
  private static void doSearchAtomsPoint(HttpServletRequest req, HttpServletResponse resp, List<Layer> layers, JsonObject json) throws IOException {
    SearchRequest request = new SearchRequest();

    for (Layer layer: layers) {
      // Layer is search root, add its name suffixed with '.'
      if (layer.isSearchRoot()) {
        request.addToLayers(layer.getLayerId() + ".");
      } else {
        request.addToLayers(layer.getLayerId());        
      }
    }
    
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

    //
    // Extract query
    //
    
    if (json.has("q")) {
      request.setQuery(json.get("q").getAsString());
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
      arreq.setUuids(sresp.getPointUuids());

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

  private static void doSearchAtomsGeofence(HttpServletRequest req, HttpServletResponse resp, List<Layer> layers, JsonObject json) throws IOException {
    SearchRequest request = new SearchRequest();

    for (Layer layer: layers) {
      request.addToLayers(layer.getLayerId() + (layer.isSearchRoot() ? "." : ""));
    }
    
    //
    // Extract paging
    //
    
    if (json.has("page")) {
      request.setPage(json.get("page").getAsInt());      
    }

    if (json.has("perpage")) {
      request.setPerpage(json.get("perpage").getAsInt());      
    }

    if (!json.has("points")) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_INVALID_GEOFENCED_POINTS.toString());
      return;      
    }

    String[] points = json.get("points").getAsString().split(",");
    
    for (String point: points) {
      long hhcode = GeoParser.parseLatLon(point);
      request.addToGeofenced(hhcode);
    }
    
    if (json.has("exhaustive")) {
      request.setGeofenceAll(json.get("exhaustive").getAsBoolean());
    }
    
    //
    // Extract query
    //
    
    if (json.has("q")) {
      request.setQuery(json.get("q").getAsString());
    }
    
    request.setType(SearchType.GEOFENCE);
    
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
      arreq.setUuids(sresp.getPointUuids());

      AtomRetrieveResponse arresp = ServiceFactory.getInstance().getAtomService().retrieve(arreq);
      
      JsonArray atoms = new JsonArray();
      
      if (arresp.getAtomsSize() > 0) {
        for (Atom atom: arresp.getAtoms()) {
          // FIXME(hbs): handle other types of atoms
          if (!AtomType.GEOFENCE.equals(atom.getType())) {
            continue;
          }
          
          Geofence geofence = atom.getGeofence();
          
          atoms.add(JsonUtil.toJson(geofence));          
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
  
  private static void doSearchAtomsCluster(HttpServletRequest req, HttpServletResponse resp, List<Layer> layers, JsonObject json) throws IOException {
    SearchRequest request = new SearchRequest();

    for (Layer layer: layers) {
      // Layer is search root, add its name suffixed with '.'
      if (layer.isSearchRoot()) {
        request.addToLayers(layer.getLayerId() + ".");
      } else {
        request.addToLayers(layer.getLayerId());        
      }
    }
    
    //
    // Check clustering params
    //

    if (!json.has("clusters")) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_MISSING_CLUSTER_COUNT.toString());
      return;            
    }
    
    request.setClusterCount(json.get("clusters").getAsInt());
    
    if (json.has("threshold")) {
      request.setClusterThreshold(json.get("threshold").getAsInt());
    } else {
      request.setClusterThreshold(0);
    }
    
    if (json.has("accuracy")) {
      request.setClusterMax(json.get("accuracy").getAsInt());
    } else {
      request.setClusterMax(0);
    }
    
    //
    // Extract area
    //
    
    if (json.has("areas")) {
      JsonArray areas = json.get("areas").getAsJsonArray();
      
      try {
        Coverage coverage = JsonUtil.coverageFromJson(areas, -2); 
        // Don't optimize the coverage, we want all cells at the same
        // resolution when we compute centroids.
        request.setArea(coverage.getAllCells());
      } catch (GeoCoordException gce) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, gce.getCode().toString());
        return;
      }
    }
    
    //
    // If no area was specified, bail out
    //
    
    if (null == request.getArea()) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, GeoCoordExceptionCode.SEARCH_INVALID_AREA.toString());
      return;
    } else {
      request.setType(SearchType.CLUSTER);
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

    //
    // Extract query
    //
    
    if (json.has("q")) {
      request.setQuery(json.get("q").getAsString());
    }
    
    JsonObject jresp = new JsonObject();

    try {
      //
      // Issue the search
      //

      SearchResponse sresp = ServiceFactory.getInstance().getSearchService().search(request);

      //
      // Retrieve individual atoms
      //

      if (sresp.getPointUuidsSize() > 0) {
        AtomRetrieveRequest arreq = new AtomRetrieveRequest();
        arreq.setUuids(sresp.getPointUuids());

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
      } else {
        jresp.add("atoms", new JsonArray());
      }
      
      //
      // Add centroids
      //
      
      if (sresp.getCentroidsSize() > 0) {
        JsonArray clusters = new JsonArray();
        
        for (Centroid centroid: sresp.getCentroids()) {
          JsonObject jcentroid = new JsonObject();
          jcentroid.addProperty("lat", centroid.getLat());
          jcentroid.addProperty("lon", centroid.getLon());
          jcentroid.addProperty("count", centroid.getCount());
          clusters.add(jcentroid);
        }
        
        jresp.add("clusters", clusters);
      } else {
        jresp.add("clusters", new JsonArray());        
      }
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
