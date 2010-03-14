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
    // Compute coverage of the rectangle.
    //
    
    Coverage coverage = HHCodeHelper.coverRectangle(request.getBottomLat(), request.getLeftLon(), request.getTopLat(), request.getRightLon(), 4);
    
    //
    // Create CentroidCollector
    //
    
    CentroidCollector cc = new CentroidCollector(searcher, coverage, request.getPointThreshold(), request.getMaxCentroidPoints());
    
    //
    // Build GeoQuery
    //
    
    StringBuilder sb = new StringBuilder();
    sb.append(GeoCoordIndex.GEO_FIELD);
    sb.append(":(");
    sb.append(coverage.toString());
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
    resp.getCentroids().addAll(cc.getCentroids());
    
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
    
    
    for (int i = 0; i < 3; i++) {
      //request.setMaxCentroidPoints((i+1) * 5);
      request.setPointThreshold(5);
      long nano = System.nanoTime();
      impl.search(request);
      System.out.println((System.nanoTime() - nano) / 1000000.0);
    }
  }
}
