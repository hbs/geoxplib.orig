package com.geocoord.geo;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.thrift.data.CentroidPoint;
import com.geocoord.thrift.data.CentroidRequest;
import com.geocoord.thrift.data.CentroidResponse;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.services.CentroidService;

public class GeoNamesLuceneImpl implements CentroidService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(GeoNamesLuceneImpl.class);
  
  private IndexSearcher searcher = null;
  
  public GeoNamesLuceneImpl() throws IOException {
    searcher = new IndexSearcher(FSDirectory.open(new File("/var/tmp/GNS-Lucene-Index")));
  }
  
  public CentroidResponse search(CentroidRequest request) throws GeoCoordException, TException {
    
    // Compute coverage of the rectangle.
    Coverage coverage = HHCodeHelper.coverRectangle(request.getBottomLat(), request.getLeftLon(), request.getTopLat(), request.getRightLon(), 4);
    System.out.println(coverage);
    
    int topN = Math.max(request.getMaxCentroidPoints(), request.getPointThreshold());
    
    Map<String,com.geocoord.thrift.data.Centroid> centroids = new HashMap<String, com.geocoord.thrift.data.Centroid>();

    for (int res: coverage.keySet()) {
      for (long hhcode: coverage.get(res)) {
        String hhstr = HHCodeHelper.toString(hhcode, res);
        try {
          TopDocs td = searcher.search(new TermQuery(new Term(Constants.LUCENE_CELLS_FIELD, hhstr)), topN);
          
          com.geocoord.thrift.data.Centroid centroid = new com.geocoord.thrift.data.Centroid();
          
          //
          // If there are more than topN points, assign centroid to the center with a weight
          // of total - topN
          //
          
          long[] cent;
          int count;
          
          System.out.println("TOTAL=" + td.totalHits + "  TOPN=" + topN);
          
          if (td.totalHits > topN) {
            // Assign the centroid to the center with a weight of 1.
            cent = HHCodeHelper.center(hhcode, res);
            //count = td.totalHits - topN;
            count = 1;
          } else {
            cent = new long[2];
            count = 0;
          }

          for (ScoreDoc sdoc: td.scoreDocs) {
            Document doc = searcher.doc(sdoc.doc);
            String hhcodestr = doc.getField(Constants.LUCENE_HHCODE_FIELD).stringValue();
            
            try {
              hhcode = Long.valueOf(hhcodestr, 16);
            } catch (NumberFormatException nfe) {
              hhcode = new BigInteger(hhcodestr, 16).longValue();
            }
            long[] hh = HHCodeHelper.splitHHCode(hhcode, 32);
            
            cent[0] += hh[0];
            cent[1] += hh[1];
            count++;
            
            if (td.totalHits <= request.getPointThreshold()) {
              CentroidPoint p = new CentroidPoint();
              p.setLat(HHCodeHelper.toLat(hh[0]));
              p.setLat(HHCodeHelper.toLon(hh[1]));
              p.setId(doc.getField(Constants.LUCENE_ID_FIELD).stringValue());
              centroid.addToPoints(p);
            }
          }

          if (count > 0) {
            cent[0] /= count;
            cent[1] /= count;
          }
          
          centroid.setCentroid(new CentroidPoint());
          centroid.getCentroid().setLat(HHCodeHelper.toLat(cent[0]));
          centroid.getCentroid().setLon(HHCodeHelper.toLon(cent[1]));            
          centroid.setCount(td.totalHits);
          
          centroids.put(hhstr, centroid);
        } catch (IOException ioe) {          
        }
      }
    }

    CentroidResponse resp = new CentroidResponse();
    resp.setCentroids(new ArrayList<com.geocoord.thrift.data.Centroid>());
    resp.getCentroids().addAll(centroids.values());
    
    System.out.println(resp);
    return resp;
  }
  
  public static void main(String[] args) throws Exception {
    GeoNamesLuceneImpl impl = new GeoNamesLuceneImpl();
    
    CentroidRequest request = new CentroidRequest();
    
    request.setBottomLat(-90);
    request.setTopLat(90);
    request.setLeftLon(-180);
    request.setRightLon(180);
    
    
    for (int i = 0; i < 3; i++) {
      request.setMaxCentroidPoints((i+1) * 5);
      request.setPointThreshold(5);
      long nano = System.nanoTime();
      impl.search(request);
      System.out.println((System.nanoTime() - nano) / 1000000.0);
    }
  }
}
