package com.geoxp.geo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.geocoord.thrift.data.Centroid;
import com.geocoord.thrift.data.CentroidPoint;

public class KMeans {
  
  /**
   * Run the K-means clustering algorithm on a collection of Centroids.
   * 
   * @param k Number of centers to use for K-means
   * @param markerThreshold Threshold of number of markers below which individual points will be ouput instead of their cluster.
   *                        This value MUST be the same as the one used to compute the collection of Centroids, otherwise weird
   *                        things might happen.
   * @param centroids Collection of centroids to run K-means on.
   * @return
   */
  public static Set<Centroid> getCentroids(int k, int markerThreshold, Collection<Centroid> centroids) {
    
    // Map of centroid to HHCode components
    final Map<Centroid,long[]> centroidHHCodes = new HashMap<Centroid, long[]>();
    
    long[] hhcomponents = new long[2];

    //
    // For each centroid in the collection, we check if it has individual points,
    // if so we add those individual points as centroids and discard the centroid itself
    // as we assume those points were the only ones used to compute the centroid.
    // If the centroid has no points, we add the centroid to the map.
    //
    
    for (Centroid centroid: centroids) {
      if (centroid.getPointsSize() > 0) {
        for (CentroidPoint p: centroid.getPoints()) {
          Centroid c = new Centroid();
          c.setLat(p.getLat());
          c.setLon(p.getLon());
          c.setCount(1);
          c.addToPoints(p);
    
          long[] coords = HHCodeHelper.splitHHCode(HHCodeHelper.getHHCodeValue(c.getLat(), c.getLon()), HHCodeHelper.MAX_RESOLUTION);
          centroidHHCodes.put(c, coords);          
        }
      } else {
        long[] coords = HHCodeHelper.splitHHCode(HHCodeHelper.getHHCodeValue(centroid.getLat(), centroid.getLon()), HHCodeHelper.MAX_RESOLUTION);
        centroidHHCodes.put(centroid, coords);                  
      }
    }
    
    //
    // Sort the centroids by decreasing count / hhcode
    //
    
    List<Centroid> cc = new ArrayList<Centroid>();
    cc.addAll(centroidHHCodes.keySet());
    
    Collections.sort(cc, new Comparator<Centroid>() {
      public int compare(Centroid o1, Centroid o2) {
        int d = Long.signum(o1.getCount() - o2.getCount());
        
        if (0 != d) {
          return d;
        }
        
        return (Long.signum(centroidHHCodes.get(o1)[0] - centroidHHCodes.get(o2)[0]));
      }
    });
        
    //
    // Take k first values of cc
    //
    
    if (k > cc.size()) {
      k = cc.size();
    }
    
    long[] centerLat = new long[k];
    long[] centerLon = new long[k];    
    long[] centerWeight = new long[k];
        
    for (int i = 0; i < k; i++) {
      centerLat[i] = centroidHHCodes.get(cc.get(i))[0];
      centerLon[i] = centroidHHCodes.get(cc.get(i))[1];
      centerWeight[i] = 0L;
    }
    
    Map<Integer,Set<Centroid>> clusters = new HashMap<Integer, Set<Centroid>>();
    Map<Integer,Set<Centroid>> prevclusters = new HashMap<Integer, Set<Centroid>>();
    
    int loop = 20;
    
    // FIXME(hbs): test stability of means
    while (loop > 0 && (prevclusters.isEmpty() || clusters.equals(prevclusters))) {
      
      loop--;
      
      if (!clusters.isEmpty()) {
        prevclusters.putAll(clusters);
        clusters.clear();
      }

      // Reinit weights
      for (int i = 0; i < k; i++) {
        centerWeight[i] = 0;
      }

      for (Centroid centroid: centroidHHCodes.keySet()) {
        int idx = -1;
        long d = Long.MAX_VALUE;
        
        //
        // Compute distance to all k centers, keeping track of the shortest
        //
        for (int i = 0; i < k; i++) {
          long dd = (centroidHHCodes.get(centroid)[0] - centerLat[i]) * (centroidHHCodes.get(centroid)[0] - centerLat[i]);
          dd += (centroidHHCodes.get(centroid)[1] - centerLon[i]) * (centroidHHCodes.get(centroid)[1] - centerLon[i]);
          
          if (dd < d) {
            d = dd;
            idx = i;
          }
        }

        // Create center centroid set if needed
        if (!clusters.containsKey(idx)) {
          clusters.put(idx, new HashSet<Centroid>());
        }
        
        // Centroid goes in the set of center k
        clusters.get(idx).add(centroid);
      }
      
      //
      // Now compute new centers
      //
      
      for (int i = 0; i < k; i++) {
        if (!clusters.containsKey(i)) {
          continue;
        }
        for (Centroid c: clusters.get(i)) {
          centerLat[i] = centerLat[i] * centerWeight[i] + centroidHHCodes.get(c)[0] * c.getCount();
          centerLon[i] = centerLon[i] * centerWeight[i] + centroidHHCodes.get(c)[1] * c.getCount();          
          centerWeight[i] += c.getCount();
          centerLat[i] = centerLat[i] / centerWeight[i];
          centerLon[i] = centerLon[i] / centerWeight[i];        
        }      
      }      
    }
    
    //
    // Return the set of centroids
    //
    
    Set<Centroid> kmeans = new HashSet<Centroid>();
    
    for (int i = 0; i < k; i++) {
      if (centerWeight[i] > 0) {
        Centroid c = new Centroid();
        c.setLat(HHCodeHelper.toLat(centerLat[i]));
        c.setLon(HHCodeHelper.toLon(centerLon[i]));
        c.setCount((int) centerWeight[i]);
        kmeans.add(c);
        
        //
        // Add individual points if a marker threshold was defined
        //
    
        if (markerThreshold > 0 && c.getCount() <= markerThreshold) {
          for (Centroid centroid: clusters.get(i)) {
            if (1 == centroid.getCount()) {
              c.addToPoints(centroid.getPoints().get(0));
            }
          }
        }
      }
    }
    
    return kmeans;
  }
}
