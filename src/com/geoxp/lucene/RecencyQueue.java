package com.geoxp.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.PriorityQueue;

public class RecencyQueue extends PriorityQueue<ScoreDoc> {
  
  /**
   * Flag indicating if we should go forward in time instead of
   * backward.
   */
  private final boolean forward;
  
  private final IndexReader reader;

  /**
   * Time pivot, results are compared to this timestamp.
   */
  private final long pivot;
  
  /**
   * 
   * @param searcher GeoCoordIndexSearcher instance to retrieve timestamps.
   * @param size     Size of queue
   * @param forward  Should we go forward in time instead of backward
   */
  public RecencyQueue(IndexSearcher searcher, int size, boolean forward) {
    super.initialize(size);
    
    this.forward = forward;
    this.reader = searcher.getIndexReader();
    this.pivot = Long.MAX_VALUE;
  }

  @Override
  protected boolean lessThan(ScoreDoc doc0, ScoreDoc doc1) {
    if (this.forward) {
      return (GeoDataSegmentCache.getTimestamp(reader, doc0.doc) < GeoDataSegmentCache.getTimestamp(reader, doc1.doc));
    } else {
      return (GeoDataSegmentCache.getTimestamp(reader, doc0.doc) > GeoDataSegmentCache.getTimestamp(reader, doc1.doc));
    }
  }
}
