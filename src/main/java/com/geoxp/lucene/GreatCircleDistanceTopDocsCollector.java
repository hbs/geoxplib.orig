package com.geoxp.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.util.PriorityQueue;

import com.geoxp.geo.HHCodeHelper;
import com.geoxp.geo.LatLonUtils;
import com.geoxp.geo.filter.GeoFilter;
import com.geoxp.lucene.GeoDataSegmentCache.GeoData;

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
  private int timestamp;

  private int segdocid;

  private GeoData gdata = new GeoData();

  private double refRadLat = 0.0;
  private double refRadLon = 0.0;
  
  private boolean farthestFirst = false;
  
  private final GeoFilter filter;
   
  /**
   * Maps that keep track of the GeoScoreDocs currently in the top docs
   * It is used to avoid duplicate points.
   */
  private Map<long[],GeoScoreDoc> uuidToGeoScoreDoc = new HashMap<long[], GeoScoreDoc>();
  
  private long[] uuid;
  
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
    
    public final boolean remove(GeoScoreDoc doc) {
      System.out.println("REMOVE");
      int removed = 0;
      
      int i = 1;
      while (i <= this.size()) {
        if (heap[i].getUuidMsb() == doc.getUuidMsb() && heap[i].getUuidLsb() == doc.getUuidLsb()) {
          removed = i;
          break;
        }
        i++;
      }

      // Doc was not found
      if (0 == removed) {
        return false;        
      }
      
      // Shift the elements before the one we found
      i = removed;
      while (i > 0) {
        heap[i] = heap[i - 1];
        i--;
      }
      
      // pop
      pop();
      return true;
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
      timestamp = timestamps[segdocid];
    } else {
      if (!GeoDataSegmentCache.getGeoData(this.currentReader, docId, gdata)) {
        return;
      }      
      hhcode = gdata.hhcode;
      lsb = gdata.uuidLSB;
      msb = gdata.uuidMSB;
      timestamp = gdata.timestamp;      
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
    
    GeoScoreDoc gsd = new GeoScoreDoc(this.docBase + docId, score, lsb, msb, timestamp);

    //
    // Check if an atom with the same UUID is already in the queue
    //
    
    uuid = new long[2];
    uuid[0] = gsd.getUuidMsb();
    uuid[1] = gsd.getUuidLsb();
    GeoScoreDoc oldGsd = uuidToGeoScoreDoc.get(uuid);
    
    // An atom with this uuid is already in the queue
    if (null != oldGsd) {
      System.out.println("DUPLICATE");
      if (oldGsd.timestamp < gsd.timestamp) {
        // Atom in queue is older than new atom, we
        // therefore remove it from the queue.
        ((HitQueue)pq).remove(oldGsd);
        uuidToGeoScoreDoc.remove(uuid);
      } else {
        // Ignore older version of Atom
        return;
      }
    }
    
    GeoScoreDoc dropped = pq.insertWithOverflow(gsd);
    
    if (null == dropped) {
      // GeoScoreDoc was added, keep track
      uuidToGeoScoreDoc.put(uuid, gsd);
    } else if (dropped != gsd) {
      // GeoScoreDoc was added, keep track
      uuidToGeoScoreDoc.put(uuid, gsd);
      uuid = new long[2];
      uuid[0] = dropped.getUuidMsb();
      uuid[1] = dropped.getUuidLsb();
      uuidToGeoScoreDoc.remove(uuid);
    }
    
    totalHits++;    
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
