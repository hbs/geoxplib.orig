package com.geocoord.server.service;

import java.util.UUID;

import org.apache.cassandra.service.EmbeddedCassandraService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.geocoord.server.ServiceFactory;
import com.geocoord.server.servlet.GuiceBootstrap;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomRemoveRequest;
import com.geocoord.thrift.data.AtomRemoveResponse;
import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.data.AtomStoreRequest;
import com.geocoord.thrift.data.AtomStoreResponse;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Cookie;
import com.geocoord.thrift.data.Coverage;
import com.geocoord.thrift.data.Point;
import com.geocoord.util.NamingUtil;

public class AtomServiceCassandraImplTestCase {
  private static GuiceBootstrap gbs = null;
  
  @BeforeClass
  public static void init() throws Exception {
    //
    // Initialize Guice
    //
    
    gbs = new GuiceBootstrap();
    gbs.init();
    
    //
    // Create a Cassandra embedded service
    //
    
    try {
      System.setProperty("storage-config", "test/conf");
      EmbeddedCassandraService cassandra = new EmbeddedCassandraService();
      cassandra.init();
      Thread t = new Thread(cassandra);
      t.setDaemon(true);
      t.start();    
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testStore() throws Exception {
    AtomStoreRequest request = new AtomStoreRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(UUID.randomUUID().toString());
    request.setCookie(cookie);
    
    //
    // Test with a POINT
    //
    
    Atom atom = new Atom();
    atom.setType(AtomType.POINT);
    atom.setTimestamp(System.currentTimeMillis());
    Point point = new Point();
    point.setPointId("com.geoxp.test.atom.point");
    point.setHhcode(43L);
    point.setTimestamp(System.currentTimeMillis());
    point.setLayerId("com.geoxp.test.layer.point");    
    atom.setPoint(point);
    request.setAtom(atom);
    
    AtomStoreResponse response = ServiceFactory.getInstance().getAtomService().store(request);
    Assert.assertEquals(point, response.getAtom().getPoint());
    
    //
    // Test with a Coverage
    //
    
    Coverage coverage = new Coverage();
    coverage.setDefinition("FOOBAR");
    coverage.setCoverageId("com.geoxp.test.atom.coverage");
    coverage.setLayerId("com.geoxp.test.layer.coverage");
    coverage.setHhcode(44L);
    coverage.setTimestamp(System.currentTimeMillis());

    atom.setType(AtomType.COVERAGE);
    atom.unsetPoint();
    atom.setCoverage(coverage);
    request.setAtom(atom);

    response = ServiceFactory.getInstance().getAtomService().store(request);
    Assert.assertEquals(coverage, response.getAtom().getCoverage());
  }
  
  @Test
  public void testRetrieve_ByLayerAndAtom() throws Exception {
    AtomStoreRequest request = new AtomStoreRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(UUID.randomUUID().toString());
    request.setCookie(cookie);
    
    Atom atom = new Atom();
    atom.setType(AtomType.POINT);
    atom.setTimestamp(System.currentTimeMillis());
    Point point = new Point();
    point.setPointId("com.geoxp.test.atom");
    point.setHhcode(43L);
    point.setTimestamp(42L);
    point.setLayerId("com.geoxp.test.layer");    
    atom.setPoint(point);
    request.setAtom(atom);
    
    AtomStoreResponse response = ServiceFactory.getInstance().getAtomService().store(request);

    AtomRetrieveRequest arreq = new AtomRetrieveRequest();
    arreq.setLayer("com.geoxp.test.layer");
    arreq.setAtom("com.geoxp.test.atom");
    
    AtomRetrieveResponse arresp = ServiceFactory.getInstance().getAtomService().retrieve(arreq);
    
    Assert.assertEquals(point, arresp.getAtom().getPoint());
  }

  @Test
  public void testRetrieve_ByUuid() throws Exception {
    AtomStoreRequest request = new AtomStoreRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(UUID.randomUUID().toString());
    request.setCookie(cookie);
    
    Atom atom = new Atom();
    atom.setType(AtomType.POINT);
    atom.setTimestamp(System.currentTimeMillis());
    Point point = new Point();
    point.setPointId("com.geoxp.test.atom");
    point.setHhcode(43L);
    point.setTimestamp(42L);
    point.setLayerId("com.geoxp.test.layer");    
    atom.setPoint(point);
    request.setAtom(atom);
    
    AtomStoreResponse response = ServiceFactory.getInstance().getAtomService().store(request);

    AtomRetrieveRequest arreq = new AtomRetrieveRequest();
    arreq.setLayer("com.geoxp.test.layer");
    arreq.setAtom("com.geoxp.test.atom");
    arreq.setUuid(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(arreq.getLayer(), arreq.getAtom())));
    arreq.unsetAtom();
    arreq.unsetLayer();
    AtomRetrieveResponse arresp = ServiceFactory.getInstance().getAtomService().retrieve(arreq);
    
    Assert.assertEquals(atom, arresp.getAtom());
  }

  @Test
  public void testRemove() throws Exception {
    AtomStoreRequest request = new AtomStoreRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(UUID.randomUUID().toString());
    request.setCookie(cookie);
    
    Atom atom = new Atom();
    atom.setType(AtomType.POINT);
    atom.setTimestamp(System.currentTimeMillis());
    Point point = new Point();
    point.setPointId("com.geoxp.test.atom");
    point.setHhcode(43L);
    point.setTimestamp(42L);
    point.setLayerId("com.geoxp.test.layer");    
    atom.setPoint(point);
    request.setAtom(atom);
    
    AtomStoreResponse response = ServiceFactory.getInstance().getAtomService().store(request);

    AtomRemoveRequest arreq = new AtomRemoveRequest();
    arreq.setAtom(atom.deepCopy());
    AtomRemoveResponse arresp = ServiceFactory.getInstance().getAtomService().remove(arreq);
    Assert.assertNotSame(atom.isDeleted(), arresp.getAtom().isDeleted());
    atom.setDeleted(true);
    Assert.assertEquals(atom, arresp.getAtom());
  }
}
