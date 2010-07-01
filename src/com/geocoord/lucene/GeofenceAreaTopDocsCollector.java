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
import com.geocoord.lucene.GeoDataSegmentCache.GeoData;

public class GeofenceAreaTopDocsCollector extends TopDocsCollector<GeoScoreDoc> {

  private int docBase = 0;
  private IndexReader currentReader = null;
  private SegmentInfo segmentKey = null;
  
  private double threshold = 0.0;
  
  private long[] uuidMSB = null;
  private long[] uuidLSB = null;
  private long[] areas = null;

  private long lsb;
  private long msb;
  private long area;
  
  private int segdocid;

  private GeoData gdata = new GeoData();

  private boolean desc = true;
  
  private static class HitQueue extends PriorityQueue<GeoScoreDoc> {
    
    public HitQueue(int size) {
      initialize(size);
    }
    
    @Override
    protected final boolean lessThan(GeoScoreDoc hitA, GeoScoreDoc hitB) {
      if (hitA.area == hitB.area)
        return hitA.doc > hitB.doc; 
      else
        return hitA.area < hitB.area;
    }
  }
  
  /**
   * Constructor.
   * 
   * @param size Number of top results to collect
   * @param desc Return geofences with largest area first
   */
  public GeofenceAreaTopDocsCollector(int size, boolean desc) {
    super(new HitQueue(size));
    this.desc = desc;
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
    
    if (null != areas) {
      segdocid = docId;

      //
      // The current reader is a SegmentReader for which we have
      // direct access to the cached data, so we access this directly.
      //
      area = areas[segdocid];
      lsb = uuidLSB[segdocid];
      msb = uuidMSB[segdocid];
    } else {
      if (!GeoDataSegmentCache.getGeoData(this.currentReader, docId, gdata)) {
        return;
      }      
      area = gdata.hhcode;
      lsb = gdata.uuidLSB;
      msb = gdata.uuidMSB;
    }
    
    //
    // Compute score from area
    //
    
    double[] latlon = HHCodeHelper.getLatLon(area, HHCodeHelper.MAX_RESOLUTION);
           
    // If we want the largest geofences first, copy the area as is, otherwise invert it.

    if (!desc) {
      area = -area;
    }
    
    GeoScoreDoc gsd = new GeoScoreDoc(this.docBase + docId, 0.0f, lsb, msb, area);

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
      areas = GeoDataSegmentCache.getHhcodes(segmentKey);
    } else {
      this.segmentKey = null;
      areas = null;
      uuidMSB = null;
      uuidLSB = null;
    }
    
    this.currentReader = reader;
    this.docBase = docBase;
  }
  
  @Override
  public void setScorer(Scorer arg0) throws IOException {
    // Ignore Scorer
  }  
}
