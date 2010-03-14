package com.geocoord.lucene;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

import com.geocoord.geo.Coverage;
import com.geocoord.geo.HHCodeHelper;
import com.geocoord.thrift.data.Centroid;
import com.geocoord.thrift.data.CentroidPoint;

/**
 * Custom Lucene Collector to compute centroids of results.
 */
public class CentroidCollector extends Collector {
  
  private int markerThreshold = 0;
  private int maxVertices = 0;
  
  private int docBase = 0;
  private GeoCoordIndexSearcher searcher = null;
  
  private Map<Long,Set<Long>> cellsByMask = new HashMap<Long, Set<Long>>();
  
  private Map<Long,Map<Long,Centroid>> centroids = new HashMap<Long, Map<Long,Centroid>>();
  
  /**
   * Create a new Centroid Collector.
   * 
   * @param searcher The GeoCoordIndexSearcher instance to use to retrieve per doc id payload.
   * @param cells The Coverage for which to compute centroids (one for each cell in the Coverage).
   * @param markerThreshold If a cell has less than that many markers, include them
   * @param maxVertices Do not continue computing a centroid after that many points were used for it, this speeds up
   *                    things with a precision penalty. Use 0 to not use this optimization.
   */
  public CentroidCollector(GeoCoordIndexSearcher searcher, Coverage coverage, int markerThreshold, int maxVertices) {
    
    this.searcher = searcher;
    this.markerThreshold = markerThreshold;
    this.maxVertices = maxVertices;
    
    //
    // Generate a list of masks to test the HHCodes against
    // and for each one the valid values
    //
  
    for (int resolution: coverage.getResolutions()) {
      long mask = 0xffffffffffffffffL << (64 - 2 * (resolution - 2));
      
      cellsByMask.put(mask, new HashSet<Long>());
      cellsByMask.get(mask).addAll(coverage.getCells(resolution));
    }
    
    //
    // Initialize the list of centroids
    //
    
    for (long mask: cellsByMask.keySet()) {
      centroids.put(mask, new HashMap<Long, Centroid>());
      for (long value: cellsByMask.get(mask)) {
        centroids.get(mask).put(value, new Centroid());
      }
    }
  }
  
  public Set<Centroid> getCentroids() {
    Set<Centroid> c = new HashSet<Centroid>();
    for (long mask: centroids.keySet()) {
      for (long value: centroids.get(mask).keySet()) {
        c.add(centroids.get(mask).get(value));
      }
    }
    
    return c;
  }
  
  @Override
  public boolean acceptsDocsOutOfOrder() {
    return true;
  }
  
  @Override
  public void collect(int docId) throws IOException {

    //
    // Retrieve hhcode for this docId
    //
    
    long hhcode = searcher.getHHCode(this.docBase + docId);
    
    long[] latlon = new long[2];        

    //
    // Apply all masks
    //
    
    for (long mask: cellsByMask.keySet()) {
      long value = hhcode & mask;
      
      //
      // The hhcode is in a cell we want the centroid for
      //
      
      if (cellsByMask.get(mask).contains(value)) {
        //
        // Retrieve the Centroid
        //
        
        Centroid centroid = centroids.get(mask).get(value);
        
        // Check if we need to update the centroid position
        if (0 == maxVertices || centroid.getCount() < maxVertices) {
          HHCodeHelper.stableSplitHHCode(hhcode, 32, latlon);

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
          //
          // Clear the set of markers if we were above the threshold
          //
          
          if (centroid.getCount() < this.markerThreshold) {
            CentroidPoint cp = new CentroidPoint();
            cp.setId(new UUID(searcher.getUUIDMSB(docId), searcher.getUUIDLSB(docId)).toString());
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
    }
  }
    
  @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    this.docBase = docBase;
  }
  
  @Override
  public void setScorer(Scorer arg0) throws IOException {
    // Ignore scorer.
  }
}
