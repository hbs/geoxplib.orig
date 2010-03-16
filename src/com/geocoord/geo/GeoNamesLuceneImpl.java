package com.geocoord.geo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.lucene.CentroidCollector;
import com.geocoord.lucene.GeoCoordIndex;
import com.geocoord.lucene.GeoCoordIndexSearcher;
import com.geocoord.thrift.data.Centroid;
import com.geocoord.thrift.data.CentroidRequest;
import com.geocoord.thrift.data.CentroidResponse;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.services.CentroidService;

public class GeoNamesLuceneImpl implements CentroidService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(GeoNamesLuceneImpl.class);
  
  private GeoCoordIndexSearcher searcher = null;
  
  public GeoNamesLuceneImpl() throws IOException {
    searcher = new GeoCoordIndexSearcher(FSDirectory.open(new File("/var/tmp/GNS-Lucene-Index")));
  }
  
  public CentroidResponse search(CentroidRequest request) throws GeoCoordException, TException {

    //
    // Compute coverage of the rectangle for point retrieval.
    //
    
    Coverage searchCoverage = HHCodeHelper.coverRectangle(request.getBottomLat(), request.getLeftLon(), request.getTopLat(), request.getRightLon(), 0);
    searchCoverage.optimize(0x0L);
    
    //
    // Create CentroidCollector
    //

    // Compute coverage for centroids - It has a finer resolution than the search coverage so we compute more centroids
    Coverage centroidCoverage = HHCodeHelper.coverRectangle(request.getBottomLat(), request.getLeftLon(), request.getTopLat(), request.getRightLon(), -2);
    
    // FIXME(hbs): does not work when crossing the international dateline
    double[] bbox = new double[4];
    bbox[0] = request.getBottomLat();
    bbox[1] = request.getLeftLon();
    bbox[2] = request.getTopLat();
    bbox[3] = request.getRightLon();
    
    
    CentroidCollector cc = new CentroidCollector(searcher, centroidCoverage, request.getPointThreshold(), request.getMaxCentroidPoints(), null);
    
    //
    // Build GeoQuery
    //
    
    StringBuilder sb = new StringBuilder();
    sb.append(GeoCoordIndex.GEO_FIELD);
    sb.append(":(");
    sb.append(searchCoverage.toString());
    sb.append(")");
    
    QueryParser qp = new QueryParser(Version.LUCENE_30, GeoCoordIndex.TAGS_FIELD, new WhitespaceAnalyzer());
    
    try {
      
      System.out.println(qp.parse(sb.toString()));
      searcher.search(qp.parse(sb.toString()), cc);
    } catch (IOException ioe) {
      logger.error("search", ioe);
    } catch (ParseException pe) {
      logger.error("search", pe);      
    }
    
    CentroidResponse resp = new CentroidResponse();
    resp.setCentroids(new ArrayList<com.geocoord.thrift.data.Centroid>());
    // Compute k-means on collected centroids
    resp.getCentroids().addAll(KMeans.getCentroids(16, cc.getCentroids()));
    
    System.out.println("# of centroids " + resp.getCentroidsSize());
    for (Centroid c: resp.getCentroids()) {
      System.out.println(c);
    }
    
    return resp;
  }
  
  public static void main(String[] args) throws Exception {
    GeoNamesLuceneImpl impl = new GeoNamesLuceneImpl();
    
    CentroidRequest request = new CentroidRequest();
    
    request.setBottomLat(-90);
    request.setTopLat(90);
    request.setLeftLon(-180);
    request.setRightLon(180);
    /*
    request.setBottomLat(45);
    request.setTopLat(50);
    request.setLeftLon(-5);
    request.setRightLon(5);
    */
    
    for (int i = 0; i < 3; i++) {
      //request.setMaxCentroidPoints((i+1) * 5);
      request.setPointThreshold(5);
      long nano = System.nanoTime();
      impl.search(request);
      System.out.println((System.nanoTime() - nano) / 1000000.0);
    }
  }
}
