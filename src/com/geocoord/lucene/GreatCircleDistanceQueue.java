package com.geocoord.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.PriorityQueue;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.geo.LatLonUtils;

/**
 * Collect topN results, closest/farthest from a given position.
 * @author hbs
 *
 */
public class GreatCircleDistanceQueue extends PriorityQueue<DistanceScoreDoc> {
  
  private final IndexSearcher searcher;
  private final IndexReader reader;
  private final boolean farthest;
  
  /**
   * Position from which to compute distances.
   */
  private final double[] from;
  
  /**
   * 
   * @param searcher GeoCoordIndexSearcher instance to retrieve timestamps.
   * @param size     Size of queue
   * @param from     HHCode of the position from which to compute distances
   * @param farthest Flag indicating if points the farthest should be considered.
   */
  public GreatCircleDistanceQueue(IndexSearcher searcher, int size, long from, boolean farthest) {
    super.initialize(size);
    this.farthest = farthest;
    this.searcher = searcher;
    this.reader = searcher.getIndexReader();
    
    // Retrieve lat/lon
    this.from = HHCodeHelper.getLatLon(from, HHCodeHelper.MAX_RESOLUTION);
    // Convert lat/lon in radians
    this.from[0] = Math.toRadians(this.from[0]);
    this.from[1] = Math.toRadians(this.from[1]);
  }

  @Override
  protected boolean lessThan(DistanceScoreDoc doc0, DistanceScoreDoc doc1) {
    if (Double.POSITIVE_INFINITY == doc0.getDistance()) {
      double[] to = HHCodeHelper.getLatLon(GeoDataSegmentCache.getHHCode(reader, doc0.doc), HHCodeHelper.MAX_RESOLUTION);
      // Conv to radians
      to[0] = Math.toRadians(to[0]);
      to[1] = Math.toRadians(to[1]);
      doc0.setDistance(LatLonUtils.getRadDistance(from[0], from[1], to[0], to[1]));
    }
    if (Double.POSITIVE_INFINITY == doc1.getDistance()) {
      double[] to = HHCodeHelper.getLatLon(GeoDataSegmentCache.getHHCode(reader, doc1.doc), HHCodeHelper.MAX_RESOLUTION);
      // Conv to radians
      to[0] = Math.toRadians(to[0]);
      to[1] = Math.toRadians(to[1]);
      doc1.setDistance(LatLonUtils.getRadDistance(from[0], from[1], to[0], to[1]));      
    }
    
    if (farthest) {
      // Only keep farthest points
      return doc0.getDistance() > doc1.getDistance();
    } else {
      // Only keep closest points
      return doc0.getDistance() < doc1.getDistance();
    }
  }
}
