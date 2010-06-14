package com.geocoord.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;

import com.geocoord.geo.Coverage;
import com.geocoord.geo.HHCodeHelper;
import com.geocoord.lucene.GeoDataSegmentCache.GeoData;
import com.geocoord.thrift.data.Centroid;
import com.geocoord.thrift.data.CentroidPoint;

/**
 * Custom Lucene Collector to compute centroids of results.
 */
public class CentroidCollector extends Collector {
  
  private int markerThreshold = 0;
  private int maxVertices = 0;
  
  private int docBase = 0;
  private IndexReader currentReader = null;
  private SegmentInfo segmentKey = null;
  
  private long[] uuidMSB = null;
  private long[] uuidLSB = null;
  private long[] hhcodes = null;
  private int[] timestamps = null;

  private long hhcode = 0;
  private long uuidmsb;
  private long uuidlsb;
  private int timestamp;
  private int segdocid;
  
  private Map<Integer,Long> masksByResolution = new HashMap<Integer, Long>();
  private Map<Long,Set<Long>> cellsByMask = new HashMap<Long, Set<Long>>();
  
  private Map<Long,Map<Long,Centroid>> centroids = new HashMap<Long, Map<Long,Centroid>>();
  
  private int collected = 0;
  
  private long[] clipbbox = null;
  
  private long[] latlon = new long[2];        

  private final Coverage coverage;
  
  private GeoData gdata = new GeoData();
  
  private final IndexReader reader;
  
  
  /**
   * Create a new Centroid Collector.
   * 
   * @param searcher The GeoCoordIndexSearcher instance to use to retrieve per doc id payload.
   * @param cells The Coverage for which to compute centroids (one for each cell in the Coverage).
   * @param markerThreshold If a cell has less than that many markers, include them
   * @param maxVertices Do not continue computing a centroid after that many points were used for it, this speeds up
   *                    things with a precision penalty. Use 0 to not use this optimization.
   * @param clipToBbox Bounding box in which all results must lie. Use null to ignore.
   */
  public CentroidCollector(IndexSearcher searcher, Coverage coverage, int markerThreshold, int maxVertices, double[] clipToBbox) {
  
    this.reader = searcher.getIndexReader();
    this.coverage = coverage;
    this.markerThreshold = markerThreshold;
    this.maxVertices = maxVertices;
    
    if (null != clipToBbox) {
      this.clipbbox = new long[4];
      this.clipbbox[0] = HHCodeHelper.toLongLat(clipToBbox[0]);
      this.clipbbox[1] = HHCodeHelper.toLongLon(clipToBbox[1]);
      this.clipbbox[2] = HHCodeHelper.toLongLat(clipToBbox[2]);
      this.clipbbox[3] = HHCodeHelper.toLongLon(clipToBbox[3]);
    }
    
    //
    // Generate a list of masks to test the HHCodes against
    // and for each one the valid values.
    //
    // Generate set of centroids.
    // FIXME(hbs): we could only compute bounding boxes for the resulting
    //             centroids (in getCentroids), but at that time, resolution is
    //             less easily available.
    //
  
    for (int resolution: coverage.getResolutions()) {
      long mask = 0xf000000000000000L >> (64 - 2 * (32 - resolution + 2));
      
      masksByResolution.put(resolution, mask);
      centroids.put(mask, new HashMap<Long, Centroid>());
      
      cellsByMask.put(mask, new HashSet<Long>());
      cellsByMask.get(mask).addAll(coverage.getCells(resolution));
      
      for (long value: cellsByMask.get(mask)) {
        Centroid c = new Centroid();
        double[] bbox = HHCodeHelper.getHHCodeBBox(value, resolution);
        
        c.setBottomLat(bbox[0]);
        c.setLeftLon(bbox[1]);
        c.setTopLat(bbox[2]);
        c.setRightLon(bbox[3]);
        
        centroids.get(mask).put(value, c);
      }
    }
  }
  
  public Set<Centroid> getCentroids() {
    
    System.out.println("Collected " + collected + " hits.");
    Set<Centroid> cs = new HashSet<Centroid>();
    for (long mask: centroids.keySet()) {
      for (long value: centroids.get(mask).keySet()) {
        Centroid c = centroids.get(mask).get(value);
        if (c.getCount() > 0) {
          c.setLat(HHCodeHelper.toLat(c.getLongLat()));
          c.setLon(HHCodeHelper.toLon(c.getLongLon()));
          cs.add(c);
        }
      }
    }
    
    return cs;
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
      segdocid = docId - this.docBase;

      //
      // The current reader is a SegmentReader for which we have
      // direct access to the cached data, so we access this directly.
      //
      hhcode = hhcodes[segdocid];
    } else {
      if (!GeoDataSegmentCache.getGeoData(reader, docId, gdata)) {
        return;
      }      
      hhcode = gdata.hhcode;
      //timestamp = gdata.timestamp;      
    }
    
    //
    // The hhcode is in a cell we want the centroid for
    //
      
    int hhres = coverage.getCoarsestResolution(hhcode);
      
    Long mask = masksByResolution.get(hhres);

    //
    // The following could happen if the hhcode was selected by another coverage,
    // for example a coarser one.
    //
      
    if (null == mask) {
      return;
    }
      
    //
    // Compute the value (i.e. the HHCode & Mask).
    // This is the cell the point is in
    //
      
    long value = hhcode & mask.longValue();

    // If no such cell exists in the Coverage we are interested in, return immediately
    if (!cellsByMask.get(mask).contains(value)) {
      return;
    }
        
    //
    // Check if the point is in the clipbbox
    //
        
    if (null != clipbbox) {
      HHCodeHelper.stableSplitHHCode(hhcode, HHCodeHelper.MAX_RESOLUTION, latlon);
      
      // If the result lies outside of the clipbbox, ignore it
      if (latlon[0] < clipbbox[0] || latlon[0] > clipbbox[2] || latlon[1] < clipbbox[1] || latlon[1] > clipbbox[3]) {
        return;
      }
    }
    
    collected++;

    //
    // Retrieve the Centroid
    //
        
    Centroid centroid = centroids.get(mask).get(value);
        
    // Check if we need to update the centroid position
    if (0 == maxVertices || centroid.getCount() < maxVertices) {
      // Split hhcode if not yet done
      if (null == clipbbox) {
        HHCodeHelper.stableSplitHHCode(hhcode, HHCodeHelper.MAX_RESOLUTION, latlon);
      }

      //
      // Update centroid value.
      // FIXME(hbs): we compute it using the long value, this is not a correct
      //             computation since we should really do some great circle math...
      //             Let's say it's good enough for now given the use (display a marker...)
      //
          
      int count = centroid.getCount();
          
      long clat = centroid.getLongLat() * count + latlon[0];
      long clon = centroid.getLongLon() * count + latlon[1];
          
      count++;
          
      centroid.setLongLat(clat / count);
      centroid.setLongLon(clon / count);
    }
        
    //
    // Check if we need to keep track of the point we just found.
    //
        
    if (0 != this.markerThreshold && centroid.getCount() <= this.markerThreshold) {
      if (centroid.getCount() < this.markerThreshold) {
        //
        // Record one extra point
        //
        CentroidPoint cp = new CentroidPoint();
        if (null != hhcodes) {
          // Use the directly accessible segment cache data.
          uuidmsb = uuidMSB[segdocid];
          uuidlsb = uuidLSB[segdocid];
          //timestamp = timestamps[segdocid];
          cp.setId(new UUID(uuidmsb, uuidlsb).toString());
        } else {
          //
          // Clear the set of markers as we are now above the threshold
          //
              
          cp.setId(new UUID(gdata.uuidMSB, gdata.uuidLSB).toString());          
        }
        double[] dlatlon = HHCodeHelper.getLatLon(hhcode, HHCodeHelper.MAX_RESOLUTION);
        cp.setLat(dlatlon[0]);
        cp.setLon(dlatlon[1]);
        centroid.addToPoints(cp);
      } else {
        centroid.getPoints().clear();
      }
    }
        
    // Increment count
    centroid.setCount(centroid.getCount() + 1);      
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
    // Ignore scorer.
  }
}
