package com.geoxp.server.service;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.iroise.commons.test.CassandraHelper;

import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.data.AtomStoreRequest;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Cookie;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerCreateRequest;
import com.geocoord.thrift.data.LayerCreateResponse;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.data.SearchRequest;
import com.geocoord.thrift.data.SearchResponse;
import com.geocoord.thrift.data.SearchType;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserCreateRequest;
import com.geocoord.thrift.data.UserCreateResponse;
import com.geocoord.thrift.data.Atom;
import com.geoxp.geo.Coverage;
import com.geoxp.geo.GeoParser;
import com.geoxp.geo.HHCodeHelper;
import com.geoxp.server.ServiceFactory;
import com.geoxp.server.service.SearchServiceLuceneImpl;
import com.geoxp.server.servlet.GuiceBootstrap;

public class SearchServiceLuceneImplTestCase {
  
  @BeforeClass
  public static void setUp() throws Exception {    
    
    //
    // Setup a Cassandra instance
    //
    
    CassandraHelper.start();
    
    //
    // Initialize injector
    //
    
    GuiceBootstrap gbs = new GuiceBootstrap();
    gbs.init();    
  }

  @Test
  public void testDistSearch() throws Exception {
    //
    // Create user/layer
    //
    
    //
    // Create user
    //
    
    String layerName = "com.geoxp.test.searchserviceluceneimpltestcase.testdistsearch.point";

    UserCreateRequest request = new UserCreateRequest();
    User user = new User();
    request.setUser(user);
    UserCreateResponse response = ServiceFactory.getInstance().getUserService().create(request);
    user = response.getUser();
        
    //
    // Create a layer
    //

    LayerCreateRequest lreq = new LayerCreateRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(user.getUserId());
    
    Layer layer = new Layer();
    layer.setLayerId(layerName + "." + user.getUserId());
    layer.setIndexed(true);
    
    lreq.setLayer(layer);
    lreq.setCookie(cookie);

    LayerCreateResponse lresp = ServiceFactory.getInstance().getLayerService().create(lreq);
    layer = lresp.getLayer();

    //
    // Store/index points
    //
    
    long nano = System.nanoTime();
    
    for (int i = 0; i < 1000; i++) {
      
      Point point = new Point();
      point.setHhcode(HHCodeHelper.getHHCodeValue(48.0 + i * 0.000001 * (i % 2 == 0 ? 1 : -1), -4.5 + i *0.000001 * (i % 2 == 0 ? 1 : -1)));
      point.setPointId("point-" + i / 2);
      point.setLayerId(layer.getLayerId());
      point.putToAttributes("layar.title", new ArrayList<String>() {{ add("Title"); }});
      point.putToAttributes("layar.line2", new ArrayList<String>() {{ add("Line 2"); }});
      point.putToAttributes("layar.line3", new ArrayList<String>() {{ add("Line 3"); }});
      point.putToAttributes("layar.line4", new ArrayList<String>() {{ add("Line 4"); }});
      point.putToAttributes("layar.attribution", new ArrayList<String>() {{ add("Attribution"); }});
      point.putToAttributes("layar.attribution", new ArrayList<String>() {{ add("Attribution"); }});
      point.putToAttributes("layar.action", new ArrayList<String>() {{ add("[]"); }});
      point.putToAttributes("layar.imageURL", new ArrayList<String>() {{ add("http://farm1.static.flickr.com/1/buddyicons/60822044@N00.jpg?1263041345#60822044@N00"); }});
      point.putToAttributes("layar.type", new ArrayList<String>() {{ add("0"); }});
      point.putToAttributes("layar.dimension", new ArrayList<String>() {{ add("1"); }});
      point.putToAttributes("~layar", new ArrayList<String>() {{ add("true"); }});
      
      Atom atom = new Atom();
      atom.setType(AtomType.POINT);
      atom.setPoint(point);
      
      AtomStoreRequest sreq = new AtomStoreRequest();
      sreq.setCookie(cookie);
      sreq.setAtom(atom);
      
      ServiceFactory.getInstance().getAtomService().store(sreq);      
    }
    
    System.out.println("Stored points in " + (System.nanoTime() - nano) / 1000000.0 + " ms.");
    
    try {
      Thread.sleep(2000L);
    } catch (InterruptedException ie) {
      
    }
    
    // Force index commit
    ServiceFactory.getInstance().getSearchService().commit();
    
    //
    // Issue Search
    //
    
    SearchRequest search = new SearchRequest();
    search.setType(SearchType.DIST);
    search.setCenter(HHCodeHelper.getHHCodeValue(48, -4.5));
    search.addToLayers(layer.getLayerId());
    search.setQuery("attr:(layar=true)");
    Coverage coverage = GeoParser.parseCircle("48.0:-4.5:150000", -2);
    search.setArea(coverage.getAllCells());
    
    search.setPage(1);
    search.setPerpage(100);
    
    nano = System.nanoTime();
    SearchResponse sresponse = ServiceFactory.getInstance().getSearchService().search(search);

    System.out.println((System.nanoTime() - nano) / 1000000.0);
    
    //
    // Retrieve atoms
    //
    
    AtomRetrieveRequest arreq = new AtomRetrieveRequest();
    arreq.setUuids(sresponse.getPointUuids());

    nano = System.nanoTime();
    AtomRetrieveResponse arresp = ServiceFactory.getInstance().getAtomService().retrieve(arreq);
    System.out.println((System.nanoTime() - nano) / 1000000.0);
    System.out.println(arresp);
    
    Assert.assertEquals(1, sresponse.getPage());
    Assert.assertEquals(search.getPerpage(), sresponse.getPerpage());
    Assert.assertEquals(search.getPerpage(), sresponse.getPointUuidsSize());
    Assert.assertEquals(500, sresponse.getTotal());
  }
  
  @Test
  public void testLightEscape() {
    String str = "foo:(bar OR ~glop) OR bar:[foo TO bar] OR bar:{foo TO bar} OR glop^0.8 OR tst?23";
    System.out.println(SearchServiceLuceneImpl.lightEscape(str));
  }
}
