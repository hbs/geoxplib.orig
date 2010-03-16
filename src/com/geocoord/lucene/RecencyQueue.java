package com.geocoord.lucene;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.PriorityQueue;

public class RecencyQueue extends PriorityQueue<ScoreDoc> {
  
  /**
   * Flag indicating if we should go forward in time instead of
   * backward.
   */
  private final boolean forward;
  
  private final GeoCoordIndexSearcher searcher;
  /**
   * 
   * @param searcher GeoCoordIndexSearcher instance to retrieve timestamps.
   * @param size     Size of queue
   * @param forward  Should we go forward in time instead of backward
   */
  public RecencyQueue(GeoCoordIndexSearcher searcher, int size, boolean forward) {
    super.initialize(size);
    
    this.forward = forward;
    this.searcher = searcher;
  }

  @Override
  protected boolean lessThan(ScoreDoc doc0, ScoreDoc doc1) {
    return this.forward & (searcher.getTimestamp(doc0.doc) < searcher.getTimestamp(doc1.doc));
  }
}
