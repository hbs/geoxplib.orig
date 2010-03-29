package com.geocoord.server.service;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ColumnPath;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
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
  
  public Client holdClient(String cluster) throws GeoCoordException {
    //
    // Ignore the cluster, connect to localhost.
    //
    
    try {
      TTransport tr = new TSocket("localhost", 9160);
      TProtocol proto = new TBinaryProtocol(tr);
      Cassandra.Client client = new Cassandra.Client(proto);
      tr.open();
      
      return client;
    } catch (TTransportException tte) {
      logger.error("holdClient", tte);
      GeoCoordException gce = new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_HOLD_ERROR);
      throw gce;
    }
  }

  /**
   * Attempts to be the first one to store a column.
   * This is done the following way:
   * 
   * Store a column under a key that is the concatenation of 8 bytes of timestamp and 16 bytes of an UUID. Do this store with
   * a consistency of QUORUM.
   * 
   * Read a column slice of length 1 for the given row key with a consistency of QUORUM too.
   * If the sole column returned is the one stored then it means we were the first one to store and we win.
   * If the returned column differs, delete the one we recorded.
   */
  public boolean lock(Client client, String keyspace, String colfam, String rowkey, byte[] value) throws GeoCoordException {
    
    Throwable throwable = null;
    
    long ts = System.currentTimeMillis();

    // ByteBuffer for colname <TS><UUID MSB><UUID LSB>
    final ByteBuffer colname = ByteBuffer.allocate(24);
    colname.order(ByteOrder.BIG_ENDIAN);

    // Generate Random UUID
    UUID uuid = UUID.randomUUID();
    
    colname.putLong(ts);
    colname.putLong(uuid.getMostSignificantBits());
    colname.putLong(uuid.getLeastSignificantBits());
    
    ColumnPath cp = new ColumnPath(colfam);
    cp.setColumn(colname.array());

    try {
      //
      // Write a column at the given rowkey with a column name of TS
      //
            
      client.insert(keyspace, rowkey, cp, value, ts, ConsistencyLevel.QUORUM);

      //
      // Read a single column, if it is the same one, then ok, otherwise
      // delete it and return false
      //
      
      SlicePredicate slice = new SlicePredicate();
      
      SliceRange range = new SliceRange();
      range.setCount(1);
      range.setStart(new byte[0]);
      range.setFinish(new byte[0]);

      slice.setSlice_range(range);
      
      ColumnParent colparent = new ColumnParent();
      colparent.setColumn_family(colfam);
      
      List<ColumnOrSuperColumn> coscs = client.get_slice(keyspace, rowkey, colparent, slice, ConsistencyLevel.QUORUM);
      
      //ColumnOrSuperColumn cosc = client.get(keyspace, rowkey, cp, ConsistencyLevel.QUORUM);
      
      if (!(coscs.size() == 1) || !Arrays.areEqual(coscs.get(0).getColumn().getName(), colname.array())) {
        throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_LOCK_FAILED);
      }
      //ColumnOrSuperColumn(column:Column(name:00 00 01 27 A5 8E 21 4C C6 8B 45 27 17 0D 4F 40 B2 8E 3B C7 7E 85 94 42, value:00 00, timestamp:1269792907596))

      
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
        // Attempt to remove the column we just wrote
        //
        
        try { client.remove(keyspace, rowkey, cp, ts, ConsistencyLevel.ONE); } catch (Exception e) { logger.error("lock", e); }
      }
    }
  }

  public void releaseClient(Client client) throws GeoCoordException {
    client.getInputProtocol().getTransport().close();
  }

  @Override
  public long getNanoOffset() {
    return System.nanoTime() - nanoOrigin;
  }
}
