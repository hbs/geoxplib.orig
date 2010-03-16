package com.geocoord.lucene;

import org.apache.lucene.search.ScoreDoc;

public class DistanceScoreDoc extends ScoreDoc {
  private double distance = Double.POSITIVE_INFINITY;
    
  public DistanceScoreDoc(int docId, float score) {
    super(docId, score);
  }
    
  public void setDistance(double d) {
    this.distance = d;
  }
    
  public double getDistance() {
    return distance;
  }
}
