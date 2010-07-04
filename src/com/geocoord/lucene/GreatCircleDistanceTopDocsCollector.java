package com.geocoord.lucene;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.util.PriorityQueue;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.geo.LatLonUtils;
import com.geocoord.geo.filter.GeoFilter;
import com.geocoord.lucene.GeoDataSegmentCache.GeoData;

public class GreatCircleDistanceTopDocsCollector extends TopDocsCollector<GeoScoreDoc> {

  private int docBase = 0;
  private IndexReader currentReader = null;
  private SegmentInfo segmentKey = null;
  
  private double threshold = 0.0;
  
  private long[] uuidMSB = null;
  private long[] uuidLSB = null;
  private long[] hhcodes = null;
  private int[] timestamps = null;

  private long lsb;
  private long msb;
  private long hhcode;
  

  private int segdocid;

  private GeoData gdata = new GeoData();

  private double refRadLat = 0.0;
  private double refRadLon = 0.0;
  
  private boolean farthestFirst = false;
  
  private final GeoFilter filter;
    
  private static class HitQueue extends PriorityQueue<GeoScoreDoc> {
    
    public HitQueue(int size) {
      initialize(size);
    }
    
    @Override
    protected final boolean lessThan(GeoScoreDoc hitA, GeoScoreDoc hitB) {
      if (hitA.score == hitB.score)
        return hitA.doc > hitB.doc; 
      else
        return hitA.score < hitB.score;
    }
  }
  
  /**
   * Constructor.
   * 
   * @param refhhcode HHCode of reference point from which great circle distances are computed.
   * @param farthestFirst Consider points the farthest first (i.e. put them on the top).
   * @param size Number of top results to collect
   * @param threshold Maximum distance (or minimum if value is < 0) to consider. If threshold is 0, don't impose a threshold.
   * @param optional GeoFilter to decide whether or not to keep a point.
   */
  public GreatCircleDistanceTopDocsCollector(long refhhcode, boolean farthestFirst, int size, double threshold, GeoFilter filter) {
    super(new HitQueue(size));

    // Convert threshold to radians.
    this.threshold = threshold / (360.0*60.0*1852.0/(Math.PI*2.0));
    this.farthestFirst = farthestFirst;
    this.filter = filter;
    
    // Extract 
    double[] latlon = HHCodeHelper.getLatLon(refhhcode, HHCodeHelper.MAX_RESOLUTION);
    
    refRadLat = Math.toRadians(latlon[0]);
    refRadLon = Math.toRadians(latlon[1]);
  }
  
  @Override
  public boolean acceptsDocsOutOfOrder() {
    return true;
  }
  
  @Override
  public void collect(int docId) throws IOException {
    //
    // Retrieve GeoData for this docId
    //
    
    if (null != hhcodes) {
      segdocid = docId;

      //
      // The current reader is a SegmentReader for which we have
      // direct access to the cached data, so we access this directly.
      //
      hhcode = hhcodes[segdocid];
      lsb = uuidLSB[segdocid];
      msb = uuidMSB[segdocid];
    } else {
      if (!GeoDataSegmentCache.getGeoData(this.currentReader, docId, gdata)) {
        return;
      }      
      hhcode = gdata.hhcode;
      lsb = gdata.uuidLSB;
      msb = gdata.uuidMSB;
      //timestamp = gdata.timestamp;      
    }
    
    double[] latlon = HHCodeHelper.getLatLon(hhcode, HHCodeHelper.MAX_RESOLUTION);

    //
    // Apply optional GeoFilter
    //
    
    if (null != filter && !filter.contains(latlon[0], latlon[1])) {
      return;      
    }
    
    //
    // Compute distance (score) from reference point
    //
    
    float score = (float) LatLonUtils.getRadDistance(refRadLat, refRadLon, Math.toRadians(latlon[0]), Math.toRadians(latlon[1]));
  
    // Do not consider the point if it violates the threshold.
    if ((threshold > 0 && score > threshold) || (threshold < 0 && score < -threshold)) {
      return;
    }
    
    // If we want the closest points first, make the score negative so bigger score means closer.
    if (!farthestFirst) {
      score = ((float) -1.0) * score;
    }
    
    GeoScoreDoc gsd = new GeoScoreDoc(this.docBase + docId, score, lsb, msb);

    totalHits++;
    pq.insertWithOverflow(gsd);
  }
  
  @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    //
    // We do an optimization in the case the next reader is a segment reader.
    // We should normally have cached data for the segment, so we reference those
    // directly to avoid doing a lookup in every 'collect' call.
    //
    
    if (reader instanceof SegmentReader) {
      this.segmentKey = GeoDataSegmentCache.getSegmentKey((SegmentReader) reader);
    
      //
      // Retrieve cached arrays
      //
      
      uuidMSB = GeoDataSegmentCache.getUuidMSB(segmentKey);
      uuidLSB = GeoDataSegmentCache.getUuidLSB(segmentKey);
      hhcodes = GeoDataSegmentCache.getHhcodes(segmentKey);
      timestamps = GeoDataSegmentCache.getTimestamps(segmentKey);
    } else {
      this.segmentKey = null;
      hhcodes = null;
      uuidMSB = null;
      uuidLSB = null;
      timestamps = null;
    }
    
    this.currentReader = reader;
    this.docBase = docBase;
  }
  
  @Override
  public void setScorer(Scorer arg0) throws IOException {
    // Ignore Scorer
  }  
}
