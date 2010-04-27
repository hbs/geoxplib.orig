package com.geocoord.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.lucene.search.IndexSearcher;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.lucene.GeoDataSegmentCache;
import com.geocoord.lucene.IndexManager;
import com.geocoord.server.service.ActivityServiceLuceneIndexer;
import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.ActivityEventType;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.services.ActivityService;

public class SingleDocActivityIndexer {

  static IndexManager indexManager;
  static ActivityService.Iface activityService;
  
  static {
    try {
      indexManager = new IndexManager();
      activityService = new ActivityServiceLuceneIndexer(indexManager);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    
    int count = Integer.valueOf(args[0]);

    String user = "01234567-89ab-cdef-0123-456789abcdef";
    
    IndexSearcher searcher = null;
        
    while(count > 0) {
      long hhcode = System.nanoTime() * System.currentTimeMillis();

      Point point = new Point();      
      point.setLayerId("testlayer");
      // Build an artificial id
      point.setPointId("testid");
      point.setAltitude(0);
      point.setHhcode(hhcode);
      point.setName("name");
      point.setTags("These are the tags associated with that point");
      point.setTimestamp(System.currentTimeMillis());
      point.setUserId(user);

      Atom atom = new Atom();
      atom.setPoint(point);
      atom.setTimestamp(point.getTimestamp());
      atom.setType(AtomType.POINT);
      
      ActivityEvent event = new ActivityEvent();
      event.setType(ActivityEventType.STORE);
      event.addToAtoms(atom);
      
      activityService.record(event);
      count--;
      if (count % 10000 == 0) {
        if (null != searcher) {
          indexManager.returnSearcher(searcher);
        }

        searcher = indexManager.borrowSearcher();
        System.out.print("*");
      }
    }

    GeoDataSegmentCache.stats();
  }
}
