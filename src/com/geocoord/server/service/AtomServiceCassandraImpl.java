package com.geocoord.server.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;

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
import org.apache.thrift.TException;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.ActivityEventType;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomStoreRequest;
import com.geocoord.thrift.data.AtomStoreResponse;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.AtomRemoveRequest;
import com.geocoord.thrift.data.AtomRemoveResponse;
import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.services.AtomService;
import com.geocoord.util.LayerUtils;
import com.geocoord.util.NamingUtil;
import com.google.common.base.Charsets;

public class AtomServiceCassandraImpl implements AtomService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(AtomServiceCassandraImpl.class);
  
  @Override
  public AtomStoreResponse store(AtomStoreRequest request) throws GeoCoordException, TException {

    Cassandra.Client client = null;
    
    try {
      //
      // Force the user from the Cookie and the timestamp of the atom envelope
      // and generate the rowkey (The ATOM_ROWKEY prefix plus the base64 version of the double FNV of the layer!atom combo).
      //
      
      Atom atom = request.getAtom();
      long timestamp = System.currentTimeMillis();
      atom.setTimestamp(timestamp);
      StringBuilder rowkey = new StringBuilder(Constants.CASSANDRA_ATOM_ROWKEY_PREFIX);
      
      //
      // Force the user from the Cookie.
      //
            
      switch (atom.getType()) {
        case POINT:
          if (!NamingUtil.isValidAtomName(atom.getPoint().getPointId())) {
            throw new GeoCoordException(GeoCoordExceptionCode.ATOM_INVALID_NAME);
          }
          atom.getPoint().setUserId(request.getCookie().getUserId());         
          rowkey.append(new String(Base64.encode(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(atom.getPoint().getLayerId(), atom.getPoint().getPointId()))), Charsets.UTF_8));
          break;
        case COVERAGE:
          if (!NamingUtil.isValidAtomName(atom.getCoverage().getCoverageId())) {
            throw new GeoCoordException(GeoCoordExceptionCode.ATOM_INVALID_NAME);
          }
          atom.getCoverage().setUserId(request.getCookie().getUserId());         
          rowkey.append(new String(Base64.encode(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(atom.getCoverage().getLayerId(), atom.getCoverage().getCoverageId()))), Charsets.UTF_8));
          break;          
      }
            
      //
      // Store atom in the Cassandra backend
      //

      ByteBuffer col = ByteBuffer.allocate(16);
      col.order(ByteOrder.BIG_ENDIAN);
      
      // FIXME(hbs): externalize column name generation
      // Build the column name, i.e. <RTS><NANOOFFSET>
      long nanooffset = ServiceFactory.getInstance().getCassandraHelper().getNanoOffset();
      col.putLong(Long.MAX_VALUE - timestamp);
      col.putLong(Long.MAX_VALUE - nanooffset);
      
      byte[] colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(atom);
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(col.array());
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey.toString(), colpath, colvalue, timestamp, ConsistencyLevel.ONE);
      
      //
      // Return the client
      //
      
      ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);
      client = null;

      //
      // Now store an activity record for the layer and cell
      //
      
      ActivityEvent event = new ActivityEvent();
      event.setType(ActivityEventType.STORE);
      event.addToAtoms(atom);
            
      ServiceFactory.getInstance().getActivityService().record(event);
      
      //
      // Return the response
      //      
      
      AtomStoreResponse response = new AtomStoreResponse();
      response.setAtom(atom);
      
      return response;
    } catch (InvalidRequestException ire) {
      logger.error("store", ire);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TimedOutException toe) {
      logger.error("store", toe);      
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (UnavailableException ue) {
      logger.error("store", ue);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TException te) { 
      logger.error("store", te);
      throw new GeoCoordException(GeoCoordExceptionCode.THRIFT_ERROR);      
    } finally {
      //
      // Return the client if needed
      //

      if (null != client) {
        
        ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);        
      }
    }
  }
  
  
  @Override
  public AtomRetrieveResponse retrieve(AtomRetrieveRequest request) throws GeoCoordException, TException {

    Cassandra.Client client = null;
    
    try {

      //
      // Attempt to retrieve the latest version of the atom
      //
      
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
      colparent.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);

      StringBuilder sb = new StringBuilder();
      sb.append(Constants.CASSANDRA_ATOM_ROWKEY_PREFIX);
      
      if (null != request.getUuid()) {
        sb.append(new String(Base64.encode(request.getUuid())));
      } else {
        sb.append(new String(Base64.encode(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(request.getLayer(), request.getAtom())))));
      }
      
      String rowkey = sb.toString();
      
      // FIXME(hbs): fix consistency????
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);
      List<ColumnOrSuperColumn> coscs = client.get_slice(Constants.CASSANDRA_KEYSPACE, rowkey, colparent, slice, ConsistencyLevel.ONE);
      
      //
      // Return the client
      //
      
      ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);
      client = null;
      
      if (1 != coscs.size()) {
        throw new GeoCoordException(GeoCoordExceptionCode.ATOM_NOT_FOUND);
      }
      
      //
      // Return the response
      //      
      
      AtomRetrieveResponse response = new AtomRetrieveResponse();
      response.setAtom((Atom) ServiceFactory.getInstance().getThriftHelper().deserialize(new Atom(), coscs.get(0).getColumn().getValue()));
      
      return response;
    } catch (InvalidRequestException ire) {
      logger.error("retrieve", ire);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TimedOutException toe) {
      logger.error("retrieve", toe);      
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (UnavailableException ue) {
      logger.error("retrieve", ue);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TException te) { 
      logger.error("retrieve", te);
      throw new GeoCoordException(GeoCoordExceptionCode.THRIFT_ERROR);      
    } finally {
      //
      // Return the client if needed
      //

      if (null != client) {
        
        ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);        
      }
    }
  }
  
  @Override
  public AtomRemoveResponse remove(AtomRemoveRequest request) throws GeoCoordException, TException {

    Cassandra.Client client = null;
    
    try {
      Atom atom = request.getAtom();

      // Mark the atom as deleted.
      atom.setDeleted(true);
      
      long timestamp = System.currentTimeMillis();
      long nanooffset = ServiceFactory.getInstance().getCassandraHelper().getNanoOffset(); 

      StringBuilder rowkey = new StringBuilder(Constants.CASSANDRA_ATOM_ROWKEY_PREFIX);
      
      String layerId = null;
      String atomId = null;
      
      switch (atom.getType()) {
        case POINT:
          layerId = atom.getPoint().getLayerId();
          atomId = atom.getPoint().getPointId();
          break;
      }

      rowkey.append(new String(Base64.encode(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(layerId, atomId)))));
      
      //
      // Store atom in the Cassandra backend
      //
      
      ByteBuffer col = ByteBuffer.allocate(16);
      col.order(ByteOrder.BIG_ENDIAN);
      
      // FIXME(hbs): externalize column name generation
      // Build the column name, i.e. <RTS><NANOOFFSET>
      col.putLong(Long.MAX_VALUE - timestamp);
      col.putLong(Long.MAX_VALUE - nanooffset);
      
      byte[] colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(atom);
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(col.array());
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey.toString(), colpath, colvalue, timestamp, ConsistencyLevel.ONE);
      
      //
      // Return the client
      //
      
      ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);
      client = null;

      //
      // Now store an activity record for the layer and cell
      //
      
      ActivityEvent event = new ActivityEvent();
      event.setType(ActivityEventType.REMOVE);
      event.addToAtoms(atom);
            
      ServiceFactory.getInstance().getActivityService().record(event);
      
      //
      // Return the response
      //      
      
      AtomRemoveResponse response = new AtomRemoveResponse();
      response.setAtom(atom);
      
      return response;
    } catch (InvalidRequestException ire) {
      logger.error("remove", ire);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TimedOutException toe) {
      logger.error("remove", toe);      
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (UnavailableException ue) {
      logger.error("remove", ue);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TException te) { 
      logger.error("remove", te);
      throw new GeoCoordException(GeoCoordExceptionCode.THRIFT_ERROR);      
    } finally {
      //
      // Return the client if needed
      //

      if (null != client) {
        
        ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);        
      }
    }    
  }
}
