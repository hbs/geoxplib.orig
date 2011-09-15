package com.geoxp.heatmap;

import java.util.concurrent.ConcurrentHashMap;

public class TileBuilderRegistry {
  
  private static ConcurrentHashMap<String, TileBuilder> builders = new ConcurrentHashMap<String, TileBuilder>();
  
  public static TileBuilder register(String name, TileBuilder builder) {
    return builders.putIfAbsent(name, builder);
  }
  
  public static TileBuilder get(String name) {
    return builders.get(name);
  }
  
  public static TileBuilder unregister(String name) {
    return builders.remove(name);
  }
}
