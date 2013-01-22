package com.geoxp.util;

import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.Layer;

public class LayerUtils {
  
  private static final String ENCODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  
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
  
  /**
   * Encode layer generation in a compact way suitable for indexing.
   * 
   * The way we encode generation is by reverse encoding groups of
   * 6 bits until no more non 0 groups exist.
   * If the initial value is 0, we encode it in a special way.
   * 
   * @param generation
   * @return
   */
  public static String encodeGeneration(long generation) {
    
    if (0 == generation) {
      return "A";
    }
    
    StringBuilder sb = new StringBuilder();
    
    while(0 != generation) {
      sb.append(ENCODE_ALPHABET.charAt((int) (generation & 0x3fL)));
      generation >>>= 6;
    }
    
    return sb.toString();
  }
}
