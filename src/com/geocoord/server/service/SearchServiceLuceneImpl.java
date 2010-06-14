package com.geocoord.server.service;

import java.io.IOException;
import java.nio.ByteBuffer;

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
import com.geocoord.lucene.FQDNTokenStream;
import com.geocoord.lucene.GeoCoordAnalyzer;
import com.geocoord.lucene.GeoCoordIndex;
import com.geocoord.lucene.GeoScoreDoc;
import com.geocoord.lucene.GreatCircleDistanceTopDocsCollector;
import com.geocoord.lucene.HHCodeTokenStream;
import com.geocoord.lucene.IndexManager;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.SearchRequest;
import com.geocoord.thrift.data.SearchResponse;
import com.geocoord.thrift.services.SearchService;
import com.google.inject.Inject;

public class SearchServiceLuceneImpl implements SearchService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceLuceneImpl.class);
  
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
        throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_NOT_IMPLEMENTED);
        //break;
      case DIST:
        return doDistSearch(request);
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
      sb.append(request.getQuery());
      sb.append(")");
    }
    
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

      GreatCircleDistanceTopDocsCollector collector = new GreatCircleDistanceTopDocsCollector(request.getCenter(), false, size, request.getThreshold()); 
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
  
  @Override
  public void commit() throws GeoCoordException, TException {
    try {
      this.manager.getWriter().commit();
    }catch (IOException ioe) {
      throw new GeoCoordException(GeoCoordExceptionCode.SEARCH_ERROR);
    }
  }
}
