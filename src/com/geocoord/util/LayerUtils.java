package com.geocoord.util;

import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.Layer;

public class LayerUtils {
  public static String getLayerRowkey(Layer layer) {
    StringBuilder sb = new StringBuilder();
    sb.append(Constants.CASSANDRA_LAYER_ROWKEY_PREFIX);
    sb.append(layer.getLayerId());
    
    return sb.toString();
  }
  
  public static String getUserLayerRowkey(Layer layer) {
    StringBuilder sb = new StringBuilder();
    sb.append(Constants.CASSANDRA_USERLAYERS_ROWKEY_PREFIX);
    sb.append(layer.getUserId());
    return sb.toString();
  }
}
