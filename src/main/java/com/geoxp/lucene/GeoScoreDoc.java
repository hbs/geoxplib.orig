package com.geoxp.lucene;

import org.apache.lucene.search.ScoreDoc;

public class GeoScoreDoc extends ScoreDoc {
  
  private final long uuidLsb;
  private final long uuidMsb;
  
  public final long area;
  
  public final int timestamp;
  
  public GeoScoreDoc(int doc, float score, long uuidLsb, long uuidMsb, int timestamp) {
    super(doc, score);
    this.uuidLsb = uuidLsb;
    this.uuidMsb = uuidMsb;
    this.timestamp = timestamp;
    this.area = 0;
  }

  public GeoScoreDoc(int doc, float score, long uuidLsb, long uuidMsb, long area) {
    super(doc, score);
    this.uuidLsb = uuidLsb;
    this.uuidMsb = uuidMsb;
    this.area = area;
    this.timestamp = 0;
  }

  public long getUuidLsb() {
    return uuidLsb;
  }
  
  public long getUuidMsb() {
    return uuidMsb;
  }
}
