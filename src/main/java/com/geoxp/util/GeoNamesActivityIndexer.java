package com.geoxp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.lucene.search.IndexSearcher;

import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.ActivityEventType;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.services.ActivityService;
import com.geoxp.geo.HHCodeHelper;
import com.geoxp.lucene.GeoDataSegmentCache;
import com.geoxp.lucene.IndexManager;
import com.geoxp.server.service.ActivityServiceLuceneIndexer;

public class GeoNamesActivityIndexer {

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
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    
    int count = 0;

    String user = UUID.randomUUID().toString();

    IndexSearcher searcher = null;
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        break;
      }

      if (line.startsWith("RC")) {
        continue;        
      }
      
      String[] tokens = line.split("\\t");

      long hhcode = HHCodeHelper.getHHCodeValue(Double.valueOf(tokens[3]), Double.valueOf(tokens[4]));

      Point point = new Point();      
      point.setLayerId(args[0]);
      // Build an artificial id
      point.setPointId(tokens[1] + ":" + tokens[2]);
      point.setAltitude(0);
      point.setHhcode(hhcode);
      point.setName(tokens[22]);
      point.setTags(tokens[23]);
      point.setTimestamp(System.currentTimeMillis());
      point.setUserId(user);

      if (!"".equals(tokens[10])) {
        List<String> values = new ArrayList<String>();
        values.add(tokens[10]);
        point.putToAttributes("dsg", values);
      }

      Atom atom = new Atom();
      atom.setPoint(point);
      atom.setTimestamp(point.getTimestamp());
      atom.setType(AtomType.POINT);
      
      ActivityEvent event = new ActivityEvent();
      event.setType(ActivityEventType.STORE);
      event.addToAtoms(atom);
      
      activityService.record(event);
      count++;
      if (count % 10000 == 0) {
        if (null != searcher) {
          indexManager.returnSearcher(searcher);
        }

        searcher = indexManager.borrowSearcher();
        System.out.print("*");
      }
    }

    System.out.println("Recorded " + count + " events.");
    indexManager.getWriter().commit();
    GeoDataSegmentCache.stats();
  }
}
