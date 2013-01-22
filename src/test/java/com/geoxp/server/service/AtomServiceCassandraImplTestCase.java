package com.geoxp.server.service;

import java.util.UUID;

import org.apache.cassandra.service.EmbeddedCassandraService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomRemoveRequest;
import com.geocoord.thrift.data.AtomRemoveResponse;
import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.data.AtomStoreRequest;
import com.geocoord.thrift.data.AtomStoreResponse;
import com.geocoord.thrift.data.AtomType;
import com.geocoord.thrift.data.Cookie;
import com.geocoord.thrift.data.Geofence;
import com.geocoord.thrift.data.Point;
import com.geoxp.server.ServiceFactory;
import com.geoxp.server.servlet.GuiceBootstrap;
import com.geoxp.util.NamingUtil;

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
    
    Geofence geofence = new Geofence();
    geofence.setDefinition("FOOBAR");
    geofence.setGeofenceId("com.geoxp.test.atom.geofence");
    geofence.setLayerId("com.geoxp.test.layer.geofence");
    geofence.setHhcode(44L);
    geofence.setTimestamp(System.currentTimeMillis());

    atom.setType(AtomType.GEOFENCE);
    atom.unsetPoint();
    atom.setGeofence(geofence);
    request.setAtom(atom);

    response = ServiceFactory.getInstance().getAtomService().store(request);
    Assert.assertEquals(geofence, response.getAtom().getGeofence());
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
    
    Assert.assertEquals(point, arresp.getAtoms().get(0).getPoint());
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
    arreq.addToUuids(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(arreq.getLayer(), arreq.getAtom())));
    arreq.unsetAtom();
    arreq.unsetLayer();
    AtomRetrieveResponse arresp = ServiceFactory.getInstance().getAtomService().retrieve(arreq);
    
    Assert.assertEquals(1, arresp.getAtomsSize());
    Assert.assertEquals(atom, arresp.getAtoms().get(0));
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
    byte[] uuid = NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(point.getLayerId(), point.getPointId()));
    arreq.addToUuids(uuid);
    AtomRemoveResponse arresp = ServiceFactory.getInstance().getAtomService().remove(arreq);
    Assert.assertEquals(1, arresp.getUuidsSize());
    Assert.assertEquals(uuid.length, arresp.getUuids().get(0).length);
    for (int i = 0; i < uuid.length; i++) {
      Assert.assertEquals(uuid[i], arresp.getUuids().get(0)[i]);
    }
  }
}
