package com.geocoord.util;

import com.geocoord.thrift.data.Layer;
import com.google.gson.JsonObject;

public class JsonUtil {
  public static JsonObject toJson(Layer layer) {
    JsonObject json = new JsonObject();
    
    if (null != layer) {
      json.addProperty("name", layer.getLayerId());
      json.addProperty("secret", layer.getSecret());
      json.addProperty("indexed", layer.isIndexed());
      json.addProperty("public", layer.isPublicLayer());      
    }

    return json;
  }
}
