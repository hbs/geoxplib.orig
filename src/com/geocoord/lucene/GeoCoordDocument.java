package com.geocoord.lucene;

import org.apache.lucene.document.Document;

public class GeoCoordDocument extends Document {
  long[] uuid = new long[2];
  
  public void setUUID(long msb, long lsb) {
    uuid[0] = msb;
    uuid[1] = lsb;
  }
  
  public long[] getUUID() {
    long[] id = new long[2];
    id[0] = uuid[0];
    id[1] = uuid[1];
    return id;
  }
}
