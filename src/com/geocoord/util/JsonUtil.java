package com.geocoord.util;

import java.util.ArrayList;
import java.util.Iterator;

import com.geocoord.geo.Coverage;
import com.geocoord.geo.GeoParser;
import com.geocoord.geo.HHCodeHelper;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Geofence;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.Point;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class JsonUtil {
  
  private static enum OPTYPE { PLUS, MINUS, INTERSECTION };

  public static JsonObject toJson(Layer layer) {
    JsonObject json = new JsonObject();
    
    if (null != layer) {
      json.addProperty("type", "layer");
      json.addProperty("name", layer.getLayerId());
      json.addProperty("secret", layer.getSecret());
      json.addProperty("indexed", layer.isIndexed());
      json.addProperty("public", layer.isPublicLayer());
      if (null != layer.getProxyFor()) {
        json.addProperty("proxyfor", layer.getProxyFor());
      }
      JsonArray attributes = new JsonArray();
      
      if (layer.getAttributesSize() > 0) {
        for (String name: layer.getAttributes().keySet()) {
          for (String value: layer.getAttributes().get(name)) {
            JsonObject attr = new JsonObject();
            attr.addProperty("name", name);
            attr.addProperty("value", value);
            attributes.add(attr);
          }
        }
      }
      
      json.add("attr", attributes);
    }

    return json;
  }
  
  public static JsonObject toJson(Point point) {
    JsonObject json = new JsonObject();
    
    if (null != point) {
      json.addProperty("type", "point");
      json.addProperty("layer", point.getLayerId());
      json.addProperty("name", point.getPointId());
      json.addProperty("tags", point.getTags());
      json.addProperty("ts", point.getTimestamp());
      json.addProperty("alt", point.getAltitude());
      double[] latlng = HHCodeHelper.getLatLon(point.getHhcode(), HHCodeHelper.MAX_RESOLUTION);
      json.addProperty("lat", latlng[0]);
      json.addProperty("lon", latlng[1]);
      
      JsonArray attrs = new JsonArray();
      
      if (point.getAttributesSize() > 0) {
        for (String name: point.getAttributes().keySet()) {
          for (String value: point.getAttributes().get(name)) {
            JsonObject attr = new JsonObject();
            attr.addProperty("name", name);
            attr.addProperty("value", value);
            attrs.add(attr);
          }
        }
      }
      
      json.add("attr", attrs);
    }

    return json;    
  }
  
  public static JsonObject toJson(Geofence geofence) {
    JsonObject json = new JsonObject();
    
    if (null != geofence) {
      json.addProperty("type", "geofence");
      json.addProperty("layer", geofence.getLayerId());
      json.addProperty("name", geofence.getGeofenceId());
      json.addProperty("tags", geofence.getTags());
      json.addProperty("ts", geofence.getTimestamp());
      
      JsonArray attrs = new JsonArray();
      
      if (geofence.getAttributesSize() > 0) {
        for (String name: geofence.getAttributes().keySet()) {
          for (String value: geofence.getAttributes().get(name)) {
            JsonObject attr = new JsonObject();
            attr.addProperty("name", name);
            attr.addProperty("value", value);
            attrs.add(attr);
          }
        }
      }
      
      json.add("attr", attrs);
      
      JsonParser parser = new JsonParser();
      json.add("areas", parser.parse(geofence.getDefinition()));
    }

    return json;
  }
  
  /**
   * Attempt to parse a JSON representation of a Point.
   * Only point properties are considered. Extra properties are ignored.
   * 
   * @param json JSON to parse
   * @return The parsed Point or null if the json was not a valid point.
   */
  public static Point pointFromJson(String json) {
    try {
      JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
      
      if (!obj.has("type") || !"point".equals(obj.get("type").getAsString())) {
        return null;
      }
      
      Point point = new Point();
      
      if (obj.has("layer")) {
        point.setLayerId(obj.get("layer").getAsString());
      }
      
      if (obj.has("lat") && obj.has("lon")) {
        double lat = obj.get("lat").getAsDouble();
        double lon = obj.get("lon").getAsDouble();
        point.setHhcode(HHCodeHelper.getHHCodeValue(lat, lon));
      } else {
        return null;
      }

      if (obj.has("alt")) {
        point.setAltitude(obj.get("alt").getAsDouble());
      }

      if (obj.has("name")) {
        point.setPointId(obj.get("name").getAsString());
      } else {
        return null;
      }
      
      if (obj.has("tags")) {
        if (!obj.get("tags").isJsonNull()) {
          point.setTags(obj.get("tags").getAsString());
        }
      }

      if (obj.has("ts")) {
        point.setTimestamp(obj.get("ts").getAsLong());
      }

      if (obj.has("attr") && obj.get("attr").isJsonArray()) {
        JsonArray attrs = obj.get("attr").getAsJsonArray();
        for (JsonElement attr: attrs) {
          if (attr.isJsonObject()) {
            JsonObject attro = attr.getAsJsonObject();
            if (attro.has("name") && attro.has("value")) {
              String name = attro.get("name").getAsString();
              if (NamingUtil.isValidPublicAttributeName(name)) {
                if (0 == point.getAttributesSize() || !point.getAttributes().containsKey(name)) {
                  point.putToAttributes(name, new ArrayList<String>());
                }
                point.getAttributes().get(name).add(attro.get("value").getAsString());
              }
            }
          }
        }
      }
      
      return point;
    } catch (ClassCastException cce) {
      return null;
    } catch (JsonParseException jpe) {
      return null;
    } catch (IllegalStateException ise) {
      return null;
    }
  }
  
  /**
   * Attempt to parse a JSON representation of a Layer 
   * @param json JSON to parse
   * @return The created Layer or null if no valid Layer was found
   */
  public static Layer layerFromJson(String json) {
    try {
      JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
      
      if (!obj.has("type") || !"layer".equals(obj.get("type").getAsString())) {
        return null;
      }
      
      Layer layer = new Layer();
      
      if (obj.has("name")) {
        layer.setLayerId(obj.get("name").getAsString());
      } else {
        return null;
      }

      if (obj.has("secret")) {
        layer.setSecret(obj.get("secret").getAsString());
      } else {
        return null;
      }

      if (obj.has("public")) {
        layer.setPublicLayer(obj.get("public").getAsBoolean());
      } else {
        layer.setPublicLayer(true);
      }

      if (obj.has("indexed")) {
        layer.setIndexed(obj.get("indexed").getAsBoolean());
      } else {
        layer.setIndexed(true);
      }

      if (obj.has("proxyfor")) {
        layer.setProxyFor(obj.get("proxyfor").getAsString());
      } else {
        layer.unsetProxyFor();
      }
      
      if (obj.has("attr") && obj.get("attr").isJsonArray()) {
        JsonArray attrs = obj.get("attr").getAsJsonArray();
        for (JsonElement attr: attrs) {
          if (attr.isJsonObject()) {
            JsonObject attro = attr.getAsJsonObject();
            if (attro.has("name") && attro.has("value")) {
              String name = attro.get("name").getAsString();
              if (NamingUtil.isValidPublicAttributeName(name)) {
                if (0 == layer.getAttributesSize() || !layer.getAttributes().containsKey(name)) {
                  layer.putToAttributes(name, new ArrayList<String>());
                }
                layer.getAttributes().get(name).add(attro.get("value").getAsString());
              }
            }
          }
        }
      }
      
      return layer;
    } catch (UnsupportedOperationException uoe) {
      return null;
    } catch (ClassCastException cce) {
      return null;
    } catch (JsonParseException jpe) {
      return null;
    } catch (IllegalStateException ise) {
      return null;
    }   
  }
  
  public static Coverage coverageFromJson(JsonArray areas, int resolution) throws GeoCoordException {
    Iterator<JsonElement> iter = areas.iterator();
    
    Coverage coverage = new Coverage();
          
    while(iter.hasNext()) {
      JsonObject area = iter.next().getAsJsonObject();
      
      // Extract mode
      OPTYPE op = OPTYPE.PLUS;
      if (!area.has("mode") || "+".equals(area.get("mode").getAsString())) {
        op = OPTYPE.PLUS;
      } else if ("-".equals(area.get("mode").getAsString())) {
        op = OPTYPE.MINUS;
      } else if ("&".equals(area.get("mode").getAsString())) {
        op = OPTYPE.INTERSECTION;
      } else {
        throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_AREA_MODE);
      }
     
      // Extract area definition
      if (!area.has("def")) {
        throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_AREA_DEFINITION);
      }
      
      String def = area.get("def").getAsString();
      
      Coverage c = GeoParser.parseArea(def, resolution);
      
      switch (op) {
        case PLUS:
          coverage.merge(c);
          break;
        case MINUS:
          coverage = Coverage.minus(coverage, c);
          break;
        case INTERSECTION:
          coverage = Coverage.intersection(coverage, c);
      }
    }
    
    return coverage;
  }
  
  public static Geofence geofenceFromJson(String json) {
    try {
      JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
      
      if (!obj.has("type") || !"geofence".equals(obj.get("type").getAsString())) {
        return null;
      }
      
      if (!obj.has("areas")) {
        return null;
      }
      
      Geofence geofence = new Geofence();
      
      if (obj.has("layer")) {
        geofence.setLayerId(obj.get("layer").getAsString());
      }
      
      if (obj.has("name")) {
        geofence.setGeofenceId(obj.get("name").getAsString());
      } else {
        return null;
      }
      
      if (obj.has("tags")) {
        if (!obj.get("tags").isJsonNull()) {
          geofence.setTags(obj.get("tags").getAsString());
        }
      }

      if (obj.has("ts")) {
        geofence.setTimestamp(obj.get("ts").getAsLong());
      }

      if (obj.has("attr") && obj.get("attr").isJsonArray()) {
        JsonArray attrs = obj.get("attr").getAsJsonArray();
        for (JsonElement attr: attrs) {
          if (attr.isJsonObject()) {
            JsonObject attro = attr.getAsJsonObject();
            if (attro.has("name") && attro.has("value")) {
              String name = attro.get("name").getAsString();
              if (NamingUtil.isValidPublicAttributeName(name)) {
                if (0 == geofence.getAttributesSize() || !geofence.getAttributes().containsKey(name)) {
                  geofence.putToAttributes(name, new ArrayList<String>());
                }
                geofence.getAttributes().get(name).add(attro.get("value").getAsString());
              }
            }
          }
        }
      }
      
      geofence.setDefinition(obj.get("areas").toString());
      
      try {
        // Generate coverage at resolution -4
        Coverage coverage = coverageFromJson(obj.get("areas").getAsJsonArray(), -4);
        // Optimize coverage
        coverage.optimize(0L);
        geofence.setCells(coverage.getAllCells());
      } catch (GeoCoordException gce) {
        return null;
      }
      
      return geofence;
    } catch (ClassCastException cce) {
      return null;
    } catch (JsonParseException jpe) {
      return null;
    } catch (IllegalStateException ise) {
      return null;
    }
    
  }
}
