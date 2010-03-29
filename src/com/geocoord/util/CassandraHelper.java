package com.geocoord.util;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Cassandra.Client;

import com.geocoord.thrift.data.GeoCoordException;

public interface CassandraHelper {
  /**
   * Retrieve a Cassandra.Client for the given cluster.
   * The retrieved Client can be used exclusively until it is released.
   * 
   * @param cluster Name of cluster to connect to.
   * @return
   */
  public Cassandra.Client holdClient(String cluster) throws GeoCoordException;
  
  /**
   * Release a Cassandra.Client previously obtained via holdClient.
   * 
   * @param client Client to release.
   * @throws GeoCoordException
   */
  public void releaseClient(Cassandra.Client client) throws GeoCoordException;
  
  /**
   * Attempt to obtain an advisory lock for the given rowkey in the given colfam.
   * 
   * @param client Cassandra.Client to use.
   * @param colfam Column Family
   * @param rowkey Row key
   * @return True upon success, false otherwise
   * @throws GeoCoordException
   */
  public boolean lock(Client client, String keyspace, String colfam, String rowkey, byte[] value) throws GeoCoordException;
  
  
  /**
   * Return a positive nanosecond offset since the start of the app (or close to it).
   * @return
   */
  public long getNanoOffset();
  
}
