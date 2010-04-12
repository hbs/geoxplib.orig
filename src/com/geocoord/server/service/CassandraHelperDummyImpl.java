package com.geocoord.server.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import net.iroise.commons.thrift.pool.PoolableTServiceClientFactory;
import net.iroise.commons.thrift.pool.TServiceClientPool;
import net.iroise.commons.thrift.pool.TSocketFactory;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ColumnPath;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;
import org.bouncycastle.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.util.CassandraHelper;

/**
 * Dummy implementation that opens a new connection each time hold is called.
 */
public class CassandraHelperDummyImpl implements CassandraHelper {

  private static final Logger logger = LoggerFactory.getLogger(CassandraHelperDummyImpl.class);

  private static final long nanoOrigin = System.nanoTime();
  
  private static final TServiceClientPool<Cassandra.Client> cassandraPool;
  
  static {
    //
    // Initialize the pool
    //
    
    PoolableTServiceClientFactory<Cassandra.Client> factory = new PoolableTServiceClientFactory<Client>(new Cassandra.Client.Factory(), new TBinaryProtocol.Factory(), new TSocketFactory("localhost", 9160)); 
    cassandraPool = new TServiceClientPool<Cassandra.Client>(factory);    
  }
  
  public CassandraHelperDummyImpl() {
  }
  
  public Client holdClient(String cluster) throws GeoCoordException {
    //
    // Ignore the cluster, connect to localhost.
    //
    
    try {
      return cassandraPool.borrowObject();
    } catch (TTransportException tte) {
      logger.error("holdClient", tte);
      GeoCoordException gce = new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_HOLD_ERROR);
      throw gce;
    } catch (Exception e) {
      logger.error("holdClient", e);
      GeoCoordException gce = new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_HOLD_ERROR);
      throw gce;      
    }
  }

  /**
   * Attempts to be the first one to store a column.
   * This is done the following way:
   * 
   * Store a column under a key that is the concatenation of 8 bytes of timestamp or reverse timestamp and bytes of an id. Do this store with
   * a consistency of QUORUM.
   * 
   * WARNING: ALWAYS use the same value for the 'reverseTimestamp' parameter for a given row, otherwise weird things will happen.
   * 
   * Read a column slice of length 2 for the given row key with a consistency of QUORUM too.
   * If the sole column returned is the one stored then it means we were the first one to store and we win.
   * If the returned column differs, delete the one we recorded.
   */
  public boolean lock(Client client, String keyspace, String colfam, String rowkey, byte[] colname, byte[] colvalue, boolean lifo) throws GeoCoordException {
    
    Throwable throwable = null;
    
    long ts = System.currentTimeMillis();
        
    ColumnPath cp = new ColumnPath(colfam);
    cp.setColumn(colname);

    try {
      //
      // Write a column at the given rowkey with QUORUM consistency
      //
            
      client.insert(keyspace, rowkey, cp, colvalue, ts, ConsistencyLevel.QUORUM);

      //
      // Read up to 3 columns with consistency level QUORUM
      //
      
      SlicePredicate slice = new SlicePredicate();
      
      SliceRange range = new SliceRange();
      range.setCount(3);
      range.setStart(new byte[0]);
      range.setFinish(new byte[0]);
      range.setReversed(false);
      
      slice.setSlice_range(range);
      
      ColumnParent colparent = new ColumnParent();
      colparent.setColumn_family(colfam);
      
      List<ColumnOrSuperColumn> coscs = client.get_slice(keyspace, rowkey, colparent, slice, ConsistencyLevel.QUORUM);

      //
      // If there are more than two results, consider the locking to have failed, because that probably means 
      // there were two concurrent attempts to lock an already locked row.
      //

      if (coscs.size() == 3) {
        throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_LOCK_FAILED);                
      }
      
      //
      // If there was a single result, check if it's us
      //
      
      if ((coscs.size() == 1) && Arrays.areEqual(coscs.get(0).getColumn().getName(), colname)) {
        return true;
      } else if (coscs.size() == 1) {
        throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_LOCK_FAILED);        
      }
     
      //
      // If using a fifo lock, the latest column won't be the first one returned
      // as colname will have increased. Therefore if lifo is false, we check the first returned column
      // to see if it's the one we just inserted.
      //
      // If lifo is true, we check the second column as latest columns will be inserted first.
      //
      
      if ((lifo && !Arrays.areEqual(coscs.get(1).getColumn().getName(), colname)
          || (!lifo ) && !Arrays.areEqual(coscs.get(0).getColumn().getName(), colname))) {
        throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_LOCK_FAILED);        
      }
      
      return true;
    } catch (GeoCoordException gce) {
      throwable = gce;
      return false;
    } catch (TException te) {
      throwable = te;
      return false;
    } catch (TimedOutException toe) {
      throwable = toe;
      return false;
    } catch (UnavailableException ue) {
      throwable = ue;
      return false;
    } catch (InvalidRequestException ire) {
      throwable = ire;
      return false;
    } finally {
      if (null != throwable) {
        throwable.printStackTrace();
        logger.error("lock", throwable);
        //
        // Attempt to remove the column we just wrote, use ts + 1 so we're AFTER the write
        //
        
        try { client.remove(keyspace, rowkey, cp, ts + 1, ConsistencyLevel.ONE); } catch (Exception e) { logger.error("lock", e); }
      }
    }
  }

  public void releaseClient(Client client) throws GeoCoordException {
    try {
      cassandraPool.returnObject(client);
    } catch (Exception e) {
      logger.error("releaseClient", e);
      GeoCoordException gce = new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_RELEASE_ERROR);
      throw gce;      
    }
    //client.getInputProtocol().getTransport().close();
  }

  @Override
  public long getNanoOffset() {
    return System.nanoTime() - nanoOrigin;
  }
}
