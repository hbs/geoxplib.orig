package com.geocoord.lucene;

import org.junit.Test;

import com.geocoord.server.service.ActivityServiceLuceneIndexer;
import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.ActivityEventType;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Point;

public class GeoDataSegmentCacheTestCase {
  @Test
  public void testTrack() throws Exception {
    IndexManager manager = new IndexManager("/var/tmp/" + GeoDataSegmentCacheTestCase.class.getName(), true);
        
    ActivityServiceLuceneIndexer asli = new ActivityServiceLuceneIndexer(manager);
    
    ActivityEvent event = new ActivityEvent();
    
    long now = System.currentTimeMillis();
    event.setType(ActivityEventType.STORE);
    event.setTimestamp(now);
    
    Atom atom = new Atom();
    atom.setType(AtomType.POINT);
    Point point = new Point();
    point.setUserId("user");
    point.setHhcode(now);
    point.setLayerId("layer");
    point.setPointId("point");
    
    atom.setPoint(point);
    event.addToAtoms(atom);
    
    //asli.record(event);
    //asli.record(event);
    //asli.record(event);
    asli.record(event);
    asli.record(event);
    
    manager.getWriter().commit();
    manager.borrowSearcher();
    
    manager.getWriter().close();    
  }
}
