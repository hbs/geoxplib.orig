package com.geoxp.server.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.geoxp.server.ServiceFactory;
import com.geoxp.util.LayerUtils;
import com.geoxp.util.NamingUtil;
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
      
      byte[] colvalue = null;
      
      switch (atom.getType()) {
        case POINT:
          if (!NamingUtil.isValidAtomName(atom.getPoint().getPointId())) {
            throw new GeoCoordException(GeoCoordExceptionCode.ATOM_INVALID_NAME);
          }
          //
          // Force the user from the Cookie.
          //
                
          atom.getPoint().setUserId(request.getCookie().getUserId());         
          rowkey.append(new String(Base64.encode(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(atom.getPoint().getLayerId(), atom.getPoint().getPointId()))), Charsets.UTF_8));
          colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(atom);
          break;
        case GEOFENCE:
          if (!NamingUtil.isValidAtomName(atom.getGeofence().getGeofenceId())) {
            throw new GeoCoordException(GeoCoordExceptionCode.ATOM_INVALID_NAME);
          }
          //
          // Force the user from the Cookie.
          //
                
          atom.getGeofence().setUserId(request.getCookie().getUserId());         
          rowkey.append(new String(Base64.encode(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(atom.getGeofence().getLayerId(), atom.getGeofence().getGeofenceId()))), Charsets.UTF_8));
          //
          // Don't store the cells of a Geofence
          //
          
          Map<Integer,Set<Long>> cells = atom.getGeofence().getCells();
          atom.getGeofence().unsetCells();
          colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(atom);
          atom.getGeofence().setCells(cells);
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
      
      List<String> keys = new ArrayList<String>();
      
      if (request.getUuidsSize() > 0) {
        for (byte[] uuid: request.getUuids()) {
          sb.setLength(Constants.CASSANDRA_ATOM_ROWKEY_PREFIX.length());
          sb.append(new String(Base64.encode(uuid)));
          keys.add(sb.toString());
        }
      } else {
        sb.append(new String(Base64.encode(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(request.getLayer(), request.getAtom())))));
        keys.add(sb.toString());
      }
      
      // FIXME(hbs): fix consistency????
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);
      
      Map<String, List<ColumnOrSuperColumn>> results = client.multiget_slice(Constants.CASSANDRA_KEYSPACE, keys, colparent, slice, ConsistencyLevel.ONE);
      
      //
      // Return the client
      //
      
      ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);
      client = null;

      AtomRetrieveResponse response = new AtomRetrieveResponse();

      for (String key: keys) {
        List<ColumnOrSuperColumn> coscs = results.get(key);
        if (null != coscs && coscs.size() > 0) {
          Atom atm = (Atom) ServiceFactory.getInstance().getThriftHelper().deserialize(new Atom(), coscs.get(0).getColumn().getValue());
          // Only add non deleted atoms
          if (!atm.isDeleted()) {
            response.addToAtoms(atm);
          }
        }
      }
            
      //
      // Return the response
      //      
      
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
    
    Atom atom = new Atom();

    // Mark the atom as deleted.
    atom.setDeleted(true);
      
    long timestamp = System.currentTimeMillis();
    long nanooffset = ServiceFactory.getInstance().getCassandraHelper().getNanoOffset(); 

    ActivityEvent event = new ActivityEvent();
    event.setType(ActivityEventType.REMOVE);
    event.setTimestamp(request.getTimestamp());
    
    AtomRemoveResponse response = new AtomRemoveResponse();

    StringBuilder rowkey = new StringBuilder();

    for (byte[] uuid: request.getUuids()) {
      rowkey.setLength(0);
      rowkey.append(Constants.CASSANDRA_ATOM_ROWKEY_PREFIX);
      rowkey.append(new String(Base64.encode(uuid)));

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
        
      try {
        client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);
        client.insert(Constants.CASSANDRA_KEYSPACE, rowkey.toString(), colpath, colvalue, timestamp, ConsistencyLevel.ONE);
        event.addToUuids(uuid);
        response.addToUuids(uuid);
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
          client = null;
        }
      }    
    }
            
    //
    // Now store an activity record for the layer and cell
    //
      
    ServiceFactory.getInstance().getActivityService().record(event);
      
    //
    // Return the response
    //      
                
    return response;
  }
}
