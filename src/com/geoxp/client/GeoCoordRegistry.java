package com.geoxp.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Widget;

public class GeoCoordRegistry {

  public static final String MAIN_MODULE = "GeoCoordModule";

  private static Map<String,Object> registry = new HashMap<String,Object>();
        
  public static void register(String key, Object object) {
    registry.put(key, object);
  }
        
  public static Object lookup(String key) {
    return registry.get(key);
  }
}
