package com.geocoord.server.service;

import org.apache.cassandra.thrift.Cassandra;
import org.junit.BeforeClass;
import org.junit.Test;

import com.geocoord.util.CassandraHelper;


public class CassandraHelperDummyImplTestCase {
  
  private static CassandraHelper helper = null;
  
  @BeforeClass
  public static void init() {
    helper = new CassandraHelperDummyImpl();
  }
  
  @Test
  public void testHold() throws Exception {
    Cassandra.Client client = helper.holdClient("GeoXP");
    boolean lock = helper.lock(client, "GeoXP", "HistoricalData", "Lcom.geoxp.testlayer", new byte[16], new byte[2048], true);
    System.out.println(lock);
  }
}
