package com.geocoord.lucene;

public class GeoCoordIndex {
  public static final String ID_FIELD = "id";
  public static final String TYPE_FIELD = "type";
  public static final String TAGS_FIELD = "tags";
  public static final String ATTR_FIELD = "attr";
  public static final String GEO_FIELD = "geo";
  public static final String LAYER_FIELD = "layer";
  public static final String USER_FIELD = "user";
  
  //
  // Three fields to store the timestamp (so we can sort on ts).
  // 64bit TS (in ms since epoch) is split in 32/16/16 bits
  public static final String TSHIGH_FIELD = "tsH";
  public static final String TSMID_FIELD = "tsM";
  public static final String TSLOW_FIELD = "tsL";
}
