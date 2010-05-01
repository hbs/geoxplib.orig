package com.geocoord.util;

import java.io.IOException;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;

import com.geocoord.geo.Coverage;
import com.geocoord.geo.HHCodeHelper;
import com.geocoord.lucene.IndexManager;

public class GeoIndexStressTest extends Thread {

  static IndexManager indexManager;
  
  static {
    try {
      indexManager = new IndexManager();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private final int count;
  
  public GeoIndexStressTest(int count) {
    this.count = count;
  }
  
  @Override
  public void run() {
    
    QueryParser qp = new QueryParser(Version.LUCENE_30, "tags", new WhitespaceAnalyzer());
    
    long total = 0;
    
    int count = this.count;
    
    while(count > 0) {
      long nano = System.nanoTime();
      
      
      IndexSearcher searcher = indexManager.borrowSearcher();
      
      //
      // Generate random rectancle
      //
      
      double swlat = Math.random() * 180.0 - 90.0;
      double swlon = Math.random() * 360.0 - 180.0;
      
      // Up to 10 degrees latitude/20 deg lon
      double nelat = swlat + Math.random() * 1.0;
      double nelon = swlon + Math.random() * 2.0;
      
      Coverage c = HHCodeHelper.coverRectangle(swlat, swlon, nelat, nelon, -4);
      c.reduce(64);
      String geo = c.toString(" OR ");
      
      try {
        Query query = qp.parse("geo:(" + geo + ") AND type:(POINT)");

        TopDocs top = searcher.search(query, 100);
      
        System.out.println("Found " + top.totalHits + " docs in " + (System.nanoTime() - nano) + " ns for area " + c.area() + " [" + c.toString() + "]");
        
        indexManager.returnSearcher(searcher);
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      count--;
      total += System.nanoTime() - nano;

    }
    
    System.out.println("###### " + this.count + " queries in " + (total / 1000000.0) + " ms, that's " + (this.count / (total / 1000000000.0)) + " qps with an avg of " + ((total / 1000000.0) / this.count) + " ms/query");
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    for (int i = 0; i < 4; i++) {
      GeoIndexStressTest gist = new GeoIndexStressTest(1000000);
      gist.start();
    }
  }

}
