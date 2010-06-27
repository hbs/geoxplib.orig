package com.geocoord.util;

import java.util.ArrayList;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.Point;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class JsonUtil {
  public static JsonObject toJson(Layer layer) {
    JsonObject json = new JsonObject();
    
    if (null != layer) {
      json.addProperty("name", layer.getLayerId());
      json.addProperty("secret", layer.getSecret());
      json.addProperty("indexed", layer.isIndexed());
      json.addProperty("public", layer.isPublicLayer());   
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
}
