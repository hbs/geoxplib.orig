package com.geocoord.server.servlet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.thrift.TException;

import com.geocoord.geo.Coverage;
import com.geocoord.geo.GeoParser;
import com.geocoord.geo.HHCodeHelper;
import com.geocoord.geo.LatLonUtils;
import com.geocoord.lucene.AttributeTokenStream;
import com.geocoord.lucene.GeoCoordIndex;
import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.data.SearchRequest;
import com.geocoord.thrift.data.SearchResponse;
import com.geocoord.thrift.data.SearchType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Singleton;

/**
 * Servlet to respond to GetPointsOfInterest requests from Layar.
 *
 * @see http://layar.pbworks.com/GetPointsOfInterest
 */
@Singleton
public class LayarGetPointsOfInterestServlet extends HttpServlet {
  
  private static final int LAYAR_PERPAGE = 15;
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doPost(req, resp);
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    //
    // Retrieve layer id
    //
    
    String layerId = req.getParameter("oauth_consumer_key");
    
    if (null == layerId) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;      
    }
    
    //
    // Retrieve layer
    //
    
    LayerRetrieveRequest request = new LayerRetrieveRequest();
    request.setLayerId(layerId);

    Layer layer = null;
    
    try {
      LayerRetrieveResponse response = ServiceFactory.getInstance().getLayerService().retrieve(request);
      
      layer = response.getLayers().get(0);
    } catch (TException te) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    } catch (GeoCoordException gce) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;      
    }
    
    //
    // Check the OAuth authorization. If the layer is not Layar ready, return an error.
    //
    
    if (0 == layer.getAttributesSize() || !layer.getAttributes().containsKey(Constants.LAYER_ATTR_LAYAR_OAUTH_SECRET)) {
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    
    //OAuthMessage(GET, http://layar.geoxp.com/api/v0/layar, [lon=4.887339, developerId=11152, CHECKBOXLIST=, oauth_version=1.0, oauth_nonce=35928165, oauth_consumer_key=com.geoxp.layar.test.0000, layerName=geoxptest0000, RADIOLIST=rb%3Dfoo, SEARCHBOX=foobar, timestamp=1276458127740, developerHash=b832ac29bd0141a49dd4bc38b021e186ac6b2c92, oauth_signature=IzoC%2FUs6tWbWeagcgs9sY9f6bBo%3D, oauth_signature_method=HMAC-SHA1, userId=6f85d06929d160a7c8a3cc1ab4b54b87db99f74b, radius=1500, accuracy=100, lat=52.377544, oauth_timestamp=1276458129])

    Map<String,String[]> params = req.getParameterMap();
    Set<Entry<String, String>> parameters = new HashSet<Entry<String,String>>();
    
    for (String name: params.keySet()) {
      for (String value: params.get(name)) {
        SimpleImmutableEntry<String, String> entry = new SimpleImmutableEntry<String, String>(name, value);
        parameters.add(entry);
      }
    }
    
    //OAuthMessage message = new OAuthMessage(req.getMethod(), "http://layar.geoxp.com/api/v0/layar", parameters);
    OAuthMessage message = OAuthServlet.getMessage(req, "http://layar.geoxp.com/api/v0/layar");
    System.out.println(message);
    OAuthServiceProvider sp = new OAuthServiceProvider("","","");
    
    //
    // Check that at least one of the oauth secret was used for validation
    //
    
    boolean verified = false;
    
    for (String secret: layer.getAttributes().get(Constants.LAYER_ATTR_LAYAR_OAUTH_SECRET)) {
      OAuthConsumer consumer = new OAuthConsumer("", layer.getLayerId(), secret, sp);
      OAuthValidator validator = new SimpleOAuthValidator();
      OAuthAccessor accessor = new OAuthAccessor(consumer);
      
      try {
        validator.validateMessage(message, accessor);
        verified = true;
        break;
      } catch (URISyntaxException use) {
      } catch (OAuthException oae) {
      }      
    }

    if (!verified) {
      resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;            
    }
    
    //
    // Extract country and language
    //
    
    String country = req.getParameter("countryCode");
    if (null != country) {
      country = country.toLowerCase();
    } else {
      country = "";
    }
    String lang = req.getParameter("lang");
    if (null != lang) {
      lang = lang.toLowerCase();
    } else {
      lang = "";
    }
    
    //
    // Build the query
    //
    
    SearchRequest sreq = new SearchRequest();
    sreq.setType(SearchType.DIST);
    sreq.setPerpage(LAYAR_PERPAGE);
    sreq.addToLayers(layer.getLayerId());
    
    try {
      
      int page = 1;
      
      double lat = Double.valueOf(req.getParameter("lat"));
      double rlat = Math.toRadians(lat);
      double lon = Double.valueOf(req.getParameter("lon"));
      double rlon = Math.toRadians(lon);
      
      double radius = 1500.0;
      
      if (null != req.getParameter("radius")) {
        radius = Double.valueOf(req.getParameter("radius"));
      }
      
      sreq.setCenter(HHCodeHelper.getHHCodeValue(lat, lon));

      //
      // Extract page
      //
      
      if (null != req.getParameter("pageKey")) {
        page = Integer.valueOf(req.getParameter("pageKey"));
      }
      
      sreq.setPage(page);
      sreq.setPerpage(LAYAR_PERPAGE);
      
      //
      // Compute search area
      //
      
      Coverage coverage = GeoParser.parseCircle(lat + ":" + lon + ":" + radius, -2);
      
      sreq.setArea(coverage.getAllCells());
      sreq.setThreshold(radius);
      
      StringBuilder sb = new StringBuilder();
      
      // FIXME(hbs): we escape the query components using Lucene's QueryParse, this means we KNOW we are using Lucene in the background...
      
      if (null != req.getParameter("CHECKBOXLIST") && !"".equals(req.getParameter("CHECKBOXLIST"))) {
        for (String cb: req.getParameter("CHECKBOXLIST").split(",")) {
          if (sb.length() > 0) {
            sb.append(" OR ");
          }
          sb.append("(");
          sb.append(cb);
          //sb.append(QueryParser.escape(cb));
          sb.append(")");
        }
        sb.insert(0, "(");
        sb.append(")");
      }
      
      if (null != req.getParameter("RADIOLIST") && !"".equals(req.getParameter("RADIOLIST"))) {
        if (sb.length() > 0) {
          sb.append(" AND ");
        }
        sb.append("(");        
        sb.append(req.getParameter("RADIOLIST"));
        //sb.append(QueryParser.escape(req.getParameter("RADIOLIST")));
        sb.append(")");
      }
      
      if (sb.length() > 0) {
        sb.insert(0, ":(");
        sb.insert(0, GeoCoordIndex.ATTR_FIELD);
        sb.append(")");
      }
      
      if (null != req.getParameter("SEARCHBOX") && !"".equals(req.getParameter("SEARCHBOX"))) {
        if (sb.length() > 0) {
          sb.append (" AND ");
        }
        sb.append(GeoCoordIndex.TAGS_FIELD);
        sb.append(":(");
        sb.append(req.getParameter("SEARCHBOX"));
        //sb.append(QueryParser.escape(req.getParameter("SEARCHBOX")));
        sb.append(")");
      }

      if (sb.length() > 0) {
        sreq.setQuery(sb.toString());
      }
      
      //
      // Issue the search
      //
      
      SearchResponse sresp = ServiceFactory.getInstance().getSearchService().search(sreq);

      //
      // Create the JSON response
      // @see http://layar.pbworks.com/GetPointsOfInterest
      //
      
      JsonObject jresp = new JsonObject();
      // Layar layer's name
      jresp.addProperty("layer", req.getParameter("layerName"));
      jresp.addProperty("radius", Math.round(radius));
      
      if (sresp.getTotal() > sresp.getPage() * sresp.getPerpage()) {
        jresp.addProperty("nextPageKey", sresp.getPage() + 1);
        jresp.addProperty("morePages", true);
      }
      
      //
      // Retrieve atoms
      //
      
      AtomRetrieveRequest arreq = new AtomRetrieveRequest();
      arreq.setUuids(sresp.getPointUuids());

      AtomRetrieveResponse arresp = ServiceFactory.getInstance().getAtomService().retrieve(arreq);

      JsonArray hotspots = new JsonArray();
      
      if (arresp.getAtomsSize() > 0) {
        for (Atom atom: arresp.getAtoms()) {
          if (!AtomType.POINT.equals(atom.getType())) {
            continue;
          }
          
          Point point = atom.getPoint();
                    
          JsonObject poi = new JsonObject();
          
          //
          // Add attributes
          //
          
          Map<String,List<String>> attributes = point.getAttributes();
          
          JsonParser jparser = new JsonParser();
          
          for (String name: attributes.keySet()) {
            if (!name.startsWith("layar.") && !name.startsWith(AttributeTokenStream.INDEXED_ATTRIBUTE_PREFIX + "layar.")) {
              continue;
            }
            
            if (attributes.get(name).isEmpty()) {
              continue;
            }

            // Strip 'layar.' / '~layar.'
            String attrname = name.substring(6 + (name.startsWith(AttributeTokenStream.INDEXED_ATTRIBUTE_PREFIX) ? 1 : 0));
              
            //
            // Handle various types differently
            //
            
            // JSON Objects/Arrays
            if ("actions".equals(attrname) || "transform".equals(attrname) || "object".equals(attrname)) {
              JsonElement elt = jparser.parse(attributes.get(name).get(0));
              poi.add(attrname, elt);
            } else if ("doNotIndex".equals(attrname)
                       || "inFocus".equals(attrname)
                       || "showSmallBiw".equals(attrname) // API v4
                       || "showBiwOnClick".equals(attrname)) { // API v4
              poi.addProperty(attrname, Boolean.valueOf(attributes.get(name).get(0)));
            } else if ("dimension".equals(attrname) || "relativeAlt".equals(attrname) || "type".equals(attrname)) { 
              poi.addProperty(attrname, Integer.valueOf(attributes.get(name).get(0)));
            } else {
              poi.addProperty(attrname, attributes.get(name).get(0));
            }
          }
          
          //
          // Force standard values
          //
          
          poi.addProperty("id", point.getLayerId() + "!" + point.getPointId());
          poi.addProperty("alt", Math.round(point.getAltitude()));
          double[] latlon = HHCodeHelper.getLatLon(point.getHhcode(), HHCodeHelper.MAX_RESOLUTION);
          poi.addProperty("lat", Math.round(latlon[0] * 1000000));
          poi.addProperty("lon", Math.round(latlon[1] * 1000000));
          poi.addProperty("distance", (360.0*60.0*1852.0/(Math.PI*2.0)) * LatLonUtils.getRadDistance(rlat, rlon, Math.toRadians(latlon[0]), Math.toRadians(latlon[1])));
          
          //
          // Fix mandatory properties
          //
          
          String[] stringProperties = new String[] { "title", "line2", "line3", "line4", "attribution", "imageURL" };
          for (String property: stringProperties) {
            if (!poi.has(property)) {
              poi.addProperty(property, "");
            }
          }
          
          if (!poi.has("type")) {
            poi.addProperty("type", 0);
          }
          
          if (!poi.has("dimension")) {
            poi.addProperty("dimension", 1);
          }
          
          if (!poi.has("actions")) {
            poi.add("actions", new JsonArray());
          }
          
          hotspots.add(poi);
        }
      }
      
      jresp.add("hotspots", hotspots);
      
      if (hotspots.size() > 0) {
        jresp.addProperty("errorCode", 0);
        jresp.addProperty("errorString", "ok");
      } else {
        // Error '20', empty result set
        jresp.addProperty("errorCode", 20);
        
        if (layer.getAttributesSize() > 0 && layer.getAttributes().containsKey("layar.error.20." + lang)) {
          jresp.addProperty("errorString", layer.getAttributes().get("layar.error.20." + lang).get(0));          
        } else if(layer.getAttributesSize() > 0 && layer.getAttributes().containsKey("layar.error.20")) {
          jresp.addProperty("errorString", layer.getAttributes().get("layar.error.20").get(0));
        } else {
          jresp.addProperty("errorString", "no results");
        }
      }
      //
      // Add Layar attributes that are set at the layer level.
      // @see http://layar.pbworks.com/Layar-4-API-changes
      //
      
      if (false && layer.getAttributesSize() > 0) {
        if (layer.getAttributes().containsKey("layar.refreshInterval")) {
          try {
            jresp.addProperty("refreshInterval", Integer.valueOf(layer.getAttributes().get("layar.refreshInterval").get(0)));
          } catch (NumberFormatException nfe) {            
          }
        }
        if (layer.getAttributes().containsKey("layar.refreshDistance")) {
          try {
            jresp.addProperty("refreshDistance", Integer.valueOf(layer.getAttributes().get("layar.refreshDistance").get(0)));
          } catch (NumberFormatException nfe) {            
          }          
        }
        if (layer.getAttributes().containsKey("layar.fullRefresh")) {
          jresp.addProperty("fullRefresh", Boolean.valueOf(layer.getAttributes().get("layar.fullRefresh").get(0)));
        }
        if (layer.getAttributes().containsKey("layar.action")) {
          JsonParser jparser = new JsonParser();
          jresp.add("action", jparser.parse(layer.getAttributes().get("layar.action").get(0)));          
        }
        if (layer.getAttributes().containsKey("layar.responseMessage." + lang)) {
          jresp.addProperty("responseMessage", layer.getAttributes().get("layar.responseMessage." + lang).get(0));
        } else if (layer.getAttributes().containsKey("layar.responseMessage")) {
          jresp.addProperty("responseMessage", layer.getAttributes().get("layar.responseMessage").get(0));          
        }
      }
      
      resp.setCharacterEncoding("utf-8");
      resp.setContentType("application/json");
      resp.getWriter().write(jresp.toString());
    } catch (GeoCoordException gce) {
      gce.printStackTrace();
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;            
    } catch (TException te) {
      te.printStackTrace();
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;            
    } catch (NumberFormatException nfe) {
      nfe.printStackTrace();
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;            
    }
    
  }
}
