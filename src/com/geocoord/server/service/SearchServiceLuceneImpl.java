package com.geocoord.server.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.util.Version;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.geo.Coverage;
import com.geocoord.geo.HHCodeHelper;
import com.geocoord.geo.KMeans;
import com.geocoord.geo.filter.GeoFilter;
import com.geocoord.geo.filter.ViewportGeoFilter;
import com.geocoord.lucene.CentroidCollector;
import com.geocoord.lucene.FQDNTokenStream;
import com.geocoord.lucene.GeoCoordAnalyzer;
import com.geocoord.lucene.GeoCoordIndex;
import com.geocoord.lucene.GeoScoreDoc;
import com.geocoord.lucene.GeofenceAreaTopDocsCollector;
import com.geocoord.lucene.GreatCircleDistanceTopDocsCollector;
import com.geocoord.lucene.HHCodeTokenStream;
import com.geocoord.lucene.IndexManager;
import com.geocoord.lucene.TagAttrAnalyzer;
import com.geocoord.thrift.data.Centroid;
import com.geocoord.thrift.data.CentroidPoint;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.SearchRequest;
import com.geocoord.thrift.data.SearchResponse;
import com.geocoord.thrift.services.SearchService;
import com.google.inject.Inject;

public class SearchServiceLuceneImpl implements SearchService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceLuceneImpl.class);
  
  /**
   * Maximum number of clusters to collect
   */
  private static final int MAX_CLUSTERS = 1024;
  
  /**
   * Maximum number of cells in search area.
   */
  private static final int MAX_SEARCH_COVERAGE_CELLS = 256;
  
  /**
   * Maximum size of a result page.
   */
  private static final int MAX_PERPAGE = 500;
  /**
   * Maximum size of results to collect.
   */
  private static final int MAX_COLLECT_SIZE = 50000;
  
  @Inject
  private IndexManager manager = null;
  
  @Inject
  public SearchServiceLuceneImpl(IndexManager manager) {
    this.manager = manager;
  }
  
  @Override
  public SearchResponse search(SearchRequest request) throws GeoCoordException, TException {
    
    switch (request.getType()) {
      case CLUSTER:
        return doClusterSearch(request);
      case DIST:
        return doDistSearch(request);
      case GEOFENCE:
        return doGeofenceSearch(request);
      case RAW:
        throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_NOT_IMPLEMENTED);
        //break;
    }

    return null;
  }
  
  private SearchResponse doDistSearch(SearchRequest request) throws GeoCoordException {
    
    if (request.getArea().isEmpty()) {
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_AREA);
    }

    if (request.getPage() <= 0 || request.getPerpage() <= 0) {
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_PAGING);
    }
    
    //
    // Build Query
    //
    
    StringBuilder sb = new StringBuilder();

    Coverage c = new Coverage(request.getArea());
    
    // Make sure coverage does not have too many cells.
    c.reduce(MAX_SEARCH_COVERAGE_CELLS);
    
    sb.append(GeoCoordIndex.LAYER_FIELD);
    sb.append(":(");
    boolean first = true;
    for (String layer: request.getLayers()) {
      if(!first) {
        sb.append(" OR ");
      }
      // Make sure the layer name does not get expanded.
      sb.append(FQDNTokenStream.FQDN_NO_EXPAND_PREFIX);
      sb.append(layer);
      first = false;
    }
    
    sb.append(") AND ");
    
    sb.append(GeoCoordIndex.GEO_FIELD);
    sb.append(":(");
    sb.append(c.toString(" OR ", Character.toString(HHCodeTokenStream.MONO_RESOLUTION_PREFIX)));
    sb.append(")");
    
    if (null != request.getQuery()) {
      sb.append(" AND (");
      sb.append(parseQuery(request.getQuery()));
      sb.append(")");
    }
    
    //
    // Restrict to a certain type of atoms
    //
    
    sb.append(" AND type:POINT");
    
    // Parse the query
    
    Query query = null;
    
    try {
      query = new QueryParser(Version.LUCENE_30, "tags", new GeoCoordAnalyzer(24)).parse(sb.toString());
      System.out.println(query);
    } catch (ParseException pe) {
      logger.error("doDistSearch", pe);
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_QUERY_PARSE_ERROR);
    }
  
    // Issue the query
    
    IndexSearcher searcher = null;

    // TODO(hbs): limit collect size (page/perpage) so we're not subject to DOS attacks.
    
    // Determine how many results to collect
    int page = Math.abs(request.getPage());
    int perpage = Math.abs(request.getPerpage());
    
    if (perpage > MAX_PERPAGE) {
      logger.error("Invalid perpage parameter: " + perpage);
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_PERPAGE);
    }
    
    int size = perpage * page;
    
    if (size > MAX_COLLECT_SIZE) {
      logger.error("Invalid collect size " + size + " = " + page + " * " + perpage);
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_COLLECT_SIZE);      
    }
    
    try {
      searcher = this.manager.borrowSearcher();

      GeoFilter filter = null;
      
      if (4 == request.getViewportSize()) {
        filter = new ViewportGeoFilter(request.getViewport());
      }
      
      GreatCircleDistanceTopDocsCollector collector = new GreatCircleDistanceTopDocsCollector(request.getCenter(), false, size, request.getThreshold(), filter); 
      
      searcher.search(query, collector);

      TopDocs topdocs = collector.topDocs((request.getPage() - 1) * request.getPerpage());     
      
      SearchResponse response = new SearchResponse();
      
      response.setTotal(collector.getTotalHits());
      response.setPage(page);
      response.setPerpage(perpage);
      response.setType(request.getType());
      
      ByteBuffer bb = ByteBuffer.allocate(16);
      
      for (ScoreDoc doc: topdocs.scoreDocs) {
        
        GeoScoreDoc gdoc = (GeoScoreDoc) doc;
        bb.rewind();
        bb.putLong(gdoc.getUuidMsb());
        bb.putLong(gdoc.getUuidLsb());
        bb.rewind();
        
        byte[] uuid = new byte[16];
        bb.get(uuid);
        
        response.addToPointUuids(uuid);
      }
      
      return response;
    } catch (IOException ioe) {
      logger.error("doDistSearch", ioe);
    } finally {
      if (null != searcher) {
        this.manager.returnSearcher(searcher);
      }
    }
    
    return null;
  }
  
  private SearchResponse doGeofenceSearch(SearchRequest request) throws GeoCoordException {
    
    if (0 == request.getGeofencedSize()) {
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_GEOFENCED_POINTS);
    }

    if (request.getPage() <= 0 || request.getPerpage() <= 0) {
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_PAGING);
    }
    
    //
    // Build Query
    //
    
    StringBuilder sb = new StringBuilder();
    
    sb.append(GeoCoordIndex.LAYER_FIELD);
    sb.append(":(");
    boolean first = true;
    for (String layer: request.getLayers()) {
      if(!first) {
        sb.append(" OR ");
      }
      // Make sure the layer name does not get expanded.
      sb.append(FQDNTokenStream.FQDN_NO_EXPAND_PREFIX);
      sb.append(layer);
      first = false;
    }
    
    sb.append(") AND ");
    
    sb.append(GeoCoordIndex.GEO_FIELD);
    sb.append(":(");
    first = true;
    for (long hhcode: request.getGeofenced()) {
      if (!first) {
        if (request.isGeofenceAll()) {
          sb.append(" AND ");
        } else {
          sb.append(" OR ");
        }
      }
      sb.append("(");
      String hhstr = HHCodeHelper.toString(hhcode, HHCodeHelper.MAX_RESOLUTION);
      for (int i = 0; i < 16; i++) {
        if (0 != i) {
          sb.append(" OR ");
        }
        sb.append(Character.toString(HHCodeTokenStream.MONO_RESOLUTION_PREFIX));
        sb.append(hhstr);
        sb.setLength(sb.length() - i);
      }
      sb.append(")");
      first = false;
    }
    sb.append(")");
    
    if (null != request.getQuery()) {
      sb.append(" AND (");
      sb.append(parseQuery(request.getQuery()));
      sb.append(")");
    }
    
    //
    // Restrict to a certain type of atoms
    //
    
    sb.append(" AND type:GEOFENCE");
    
    // Parse the query
    
    Query query = null;
    
    try {
      query = new QueryParser(Version.LUCENE_30, "tags", new GeoCoordAnalyzer(24)).parse(sb.toString());
      System.out.println(query);
    } catch (ParseException pe) {
      logger.error("doGeofenceSearch", pe);
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_QUERY_PARSE_ERROR);
    }
  
    // Issue the query
    
    IndexSearcher searcher = null;

    // Determine how many results to collect
    int page = Math.abs(request.getPage());
    int perpage = Math.abs(request.getPerpage());
    
    if (perpage > MAX_PERPAGE) {
      logger.error("Invalid perpage parameter: " + perpage);
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_PERPAGE);
    }
    
    int size = perpage * page;
    
    if (size > MAX_COLLECT_SIZE) {
      logger.error("Invalid collect size " + size + " = " + page + " * " + perpage);
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_COLLECT_SIZE);      
    }
    
    try {
      searcher = this.manager.borrowSearcher();

      GeofenceAreaTopDocsCollector collector = new GeofenceAreaTopDocsCollector(size, true); 
      searcher.search(query, collector);

      TopDocs topdocs = collector.topDocs((request.getPage() - 1) * request.getPerpage());     
      
      SearchResponse response = new SearchResponse();
      
      response.setTotal(collector.getTotalHits());
      response.setPage(page);
      response.setPerpage(perpage);
      response.setType(request.getType());
      
      ByteBuffer bb = ByteBuffer.allocate(16);
      
      for (ScoreDoc doc: topdocs.scoreDocs) {
        
        GeoScoreDoc gdoc = (GeoScoreDoc) doc;
        bb.rewind();
        bb.putLong(gdoc.getUuidMsb());
        bb.putLong(gdoc.getUuidLsb());
        bb.rewind();
        
        byte[] uuid = new byte[16];
        bb.get(uuid);
        
        response.addToPointUuids(uuid);
      }
      
      return response;
    } catch (IOException ioe) {
      logger.error("doDistSearch", ioe);
    } finally {
      if (null != searcher) {
        this.manager.returnSearcher(searcher);
      }
    }
    
    return null;
  }
  
  private SearchResponse doClusterSearch(SearchRequest request) throws GeoCoordException {
    
    if (request.getClusterCount() > MAX_CLUSTERS) {
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_COLLECT_SIZE);      
    }

    if (request.getArea().isEmpty()) {
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_AREA);
    }

    //
    // Build Query
    // FIXME(hbs): factor code with doDistSearch
    
    StringBuilder sb = new StringBuilder();

    Coverage c = new Coverage(request.getArea());
    
    // Make sure coverage does not have too many cells.
    c.reduce(MAX_SEARCH_COVERAGE_CELLS);
    
    sb.append(GeoCoordIndex.LAYER_FIELD);
    sb.append(":(");
    boolean first = true;
    for (String layer: request.getLayers()) {
      if(!first) {
        sb.append(" OR ");
      }
      // Make sure the layer name does not get expanded.
      sb.append(FQDNTokenStream.FQDN_NO_EXPAND_PREFIX);
      sb.append(layer);
      first = false;
    }
    
    sb.append(") AND ");
    
    sb.append(GeoCoordIndex.GEO_FIELD);
    sb.append(":(");
    sb.append(c.toString(" OR ", Character.toString(HHCodeTokenStream.MONO_RESOLUTION_PREFIX)));
    sb.append(")");
    
    if (null != request.getQuery()) {
      sb.append(" AND (");
      sb.append(parseQuery(request.getQuery()));
      sb.append(")");
    }
    
    //
    // Restrict to a certain type of atoms
    //
    
    sb.append(" AND type:POINT");
    
    // Parse the query
    
    Query query = null;
    
    try {
      query = new QueryParser(Version.LUCENE_30, "tags", new GeoCoordAnalyzer(24)).parse(sb.toString());
      System.out.println(query);
    } catch (ParseException pe) {
      logger.error("doDistSearch", pe);
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_QUERY_PARSE_ERROR);
    }
  
    // Issue the query
    
    IndexSearcher searcher = null;
        
    try {
      searcher = this.manager.borrowSearcher();

      GeoFilter filter = null;
      
      if (4 == request.getViewportSize()) {
        filter = new ViewportGeoFilter(request.getViewport());
      }
  
      //
      // Create CentroidCollector with the unoptimized original coverage (it was optimized in the reduce call).
      // We want unoptimized coverage so centroids are spread all over the coverage.
      //
      
      CentroidCollector collector = new CentroidCollector(new Coverage(request.getArea()), request.getClusterThreshold(), request.getClusterMax(), filter);
      
      searcher.search(query, collector);

      // Extract collected centroids
      Set<Centroid> centroids = collector.getCentroids();
      
      //
      // Run K-means
      //
      
      centroids = KMeans.getCentroids(request.getClusterCount(), request.getClusterThreshold(), centroids);

      SearchResponse response = new SearchResponse();
      
      response.setType(request.getType());
      
      //
      // Loop over the centroids
      //
      
      for (Centroid centroid: centroids) {
        //
        // If there are points associated with
        // the centroid, add them to the response and
        // discard the centroid.
        //
        
        if (centroid.getPointsSize() > 0) {
          for (CentroidPoint point: centroid.getPoints()) {
            response.addToPointUuids(point.getId());
          }
        } else {
          response.addToCentroids(centroid);
        }
      }
      
      return response;
    } catch (IOException ioe) {
      logger.error("doClusterSearch", ioe);
    } finally {
      if (null != searcher) {
        this.manager.returnSearcher(searcher);
      }
    }
    
    return null;
  }
  
  @Override
  public void commit() throws GeoCoordException, TException {
    try {
      this.manager.getWriter().commit();
    }catch (IOException ioe) {
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_ERROR);
    }
  }
  
  public static String lightEscape(String str) {
    return str.replaceAll("([\\[\\]\\{\\}\\*\\?\\^\\~\\:])", "\\\\$1");
  }
  
  private String parseQuery(String query) throws GeoCoordException {    
    //
    // Parse Query with TagAttr analyzer
    //
    
    try {
      QueryParser qp = new QueryParser(Version.LUCENE_30, GeoCoordIndex.TAGS_FIELD, new TagAttrAnalyzer());
      // Escape ~ [ ] { } * ^ ?        
      Query q = qp.parse(lightEscape(query));
      return q.toString();
    } catch (ParseException pe) {
      logger.error("doDistSearch", pe);
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_QUERY_PARSE_ERROR);        
    } catch (RuntimeException re) {
      logger.error("doDistSearch", re);
      if (re.getCause() instanceof GeoCoordException) {
        throw (GeoCoordException) re.getCause();
      } else {
        throw re;
      }
    }
    
  }
}
