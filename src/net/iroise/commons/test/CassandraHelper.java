package net.iroise.commons.test;

import org.apache.cassandra.service.EmbeddedCassandraService;

public class CassandraHelper {
  public static final void start() throws Exception {
    //
    // Setup Cassandra
    //
    
    System.setProperty("storage-config", "test/conf");
    EmbeddedCassandraService cassandra = new EmbeddedCassandraService();
    cassandra.init();
    Thread t = new Thread(cassandra);
    t.setDaemon(true);
    t.start();    
  }
}
