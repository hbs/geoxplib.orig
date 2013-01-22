package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.index.MergePolicy.OneMerge;

import com.geoxp.lucene.GeoDataSegmentCache;

public class GeoXPConcurrentMergeScheduler extends ConcurrentMergeScheduler {
  
  private final IndexWriter writer;
  
  public GeoXPConcurrentMergeScheduler(IndexWriter writer) {
    this.writer = writer;
  }
  
  @Override
  protected void doMerge(OneMerge merge) throws IOException {
    super.doMerge(merge);
    
    // 
    // Reflect the merge in the segment cache.
    // We need to do this otherwise deleteByUUID might miss deletes by not
    // finding atoms in the known segments.
    // This is why deleteByUUID calls waitForMerges, because when waitForMerges
    // returns, GeoDataSegmentCache.commitMerge will have been called and the
    // merged segments will have been replaced by the resulting single segment.
    //
    GeoDataSegmentCache.commitMerge(this.writer, merge.segments, merge.info);
  }
}
