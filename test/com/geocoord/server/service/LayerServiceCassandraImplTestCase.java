package com.geocoord.server.service;

import java.util.UUID;

import org.apache.cassandra.service.EmbeddedCassandraService;
import org.apache.cassandra.thrift.CassandraDaemon;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.geocoord.server.ServiceFactory;
import com.geocoord.server.servlet.GuiceBootstrap;
import com.geocoord.thrift.data.Cookie;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerCreateRequest;
import com.geocoord.thrift.data.LayerCreateResponse;
import com.geocoord.thrift.data.LayerRemoveRequest;
import com.geocoord.thrift.data.LayerRemoveResponse;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;


public class LayerServiceCassandraImplTestCase {

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
  public void testCreate() throws Exception {
    LayerCreateRequest request = new LayerCreateRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(UUID.randomUUID().toString());
    request.setCookie(cookie);
    
    Layer layer = new Layer();
    layer.setLayerId("com.geoxp.testlayer2");
    request.setLayer(layer);
    
    LayerCreateResponse response = ServiceFactory.getInstance().getLayerService().create(request);
    
    //
    // Reread layer
    //
    
    LayerRetrieveRequest req = new LayerRetrieveRequest();
    req.setLayerId(layer.getLayerId());
    LayerRetrieveResponse resp = ServiceFactory.getInstance().getLayerService().retrieve(req);
    
    Assert.assertEquals(1, resp.getLayersSize());
    Assert.assertEquals(response.getLayer(), resp.getLayers().get(0));
  }
  
  @Test
  public void testUpdate() {
    
  }
  
  @Test
  public void testRemove() throws Exception {
    LayerCreateRequest request = new LayerCreateRequest();
    Cookie cookie = new Cookie();
    cookie.setUserId(UUID.randomUUID().toString());
    request.setCookie(cookie);
    
    Layer layer = new Layer();
    layer.setLayerId("com.geoxp.testlayer3");
    request.setLayer(layer);
    
    LayerCreateResponse response = ServiceFactory.getInstance().getLayerService().create(request);

    //
    // Now delete layer
    //
    
    LayerRemoveRequest dreq = new LayerRemoveRequest();
    dreq.setLayer(layer);
    LayerRemoveResponse dresp = ServiceFactory.getInstance().getLayerService().remove(dreq);
    
    Assert.assertTrue(layer.isDeleted());
    
    //
    // Reread layer
    //
    
    LayerRetrieveRequest req = new LayerRetrieveRequest();
    req.setLayerId(layer.getLayerId());
    
    try {
      LayerRetrieveResponse resp = ServiceFactory.getInstance().getLayerService().retrieve(req);
      Assert.fail();
    } catch (GeoCoordException gce) {
      Assert.assertEquals(GeoCoordExceptionCode.LAYER_DELETED, gce.getCode());
    }    
  }
}
