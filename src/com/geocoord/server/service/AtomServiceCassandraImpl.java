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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.server.ServiceFactory;
import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.ActivityEventType;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.AtomUpdateRequest;
import com.geocoord.thrift.data.AtomUpdateResponse;
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.AtomCreateRequest;
import com.geocoord.thrift.data.AtomCreateResponse;
import com.geocoord.thrift.data.AtomRemoveRequest;
import com.geocoord.thrift.data.AtomRemoveResponse;
import com.geocoord.thrift.data.AtomRetrieveRequest;
import com.geocoord.thrift.data.AtomRetrieveResponse;
import com.geocoord.thrift.services.AtomService;

public class AtomServiceCassandraImpl implements AtomService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(AtomServiceCassandraImpl.class);
  
  @Override
  public AtomCreateResponse create(AtomCreateRequest request) throws GeoCoordException, TException {

    Cassandra.Client client = null;
    
    try {
      //
      // Generate a point id and set the timestamp
      // Force the user from the Cookie.
      //
      
      Atom atom = request.getAtom();
      
      long timestamp = System.currentTimeMillis();
      long nanooffset = ServiceFactory.getInstance().getCassandraHelper().getNanoOffset(); 
      String id = UUID.randomUUID().toString();
      
      switch (atom.getType()) {
        case POINT:
          atom.getPoint().setPointId(id);
          atom.setTimestamp(timestamp);
          atom.getPoint().setUserId(request.getCookie().getUserId());
          break;
      }
            
      //
      // Store atom in the Cassandra backend
      //
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

      ByteBuffer col = ByteBuffer.allocate(16);
      col.order(ByteOrder.BIG_ENDIAN);
      
      // FIXME(hbs): externalize column name generation
      // Build the column name, i.e. <RTS><NANOOFFSET>
      col.putLong(Long.MAX_VALUE - timestamp);
      col.putLong(Long.MAX_VALUE - nanooffset);
      
      byte[] colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(atom);
      
      StringBuilder sb = new StringBuilder(Constants.CASSANDRA_ATOM_ROWKEY_PREFIX);
      sb.append(id);
      String rowkey = sb.toString();
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(col.array());
      
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, timestamp, ConsistencyLevel.ONE);
      
      //
      // Return the client
      //
      
      ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);
      client = null;

      //
      // Now store an activity record for the layer and cell
      //
      
      ActivityEvent event = new ActivityEvent();
      event.setType(ActivityEventType.CREATE);
      event.addToAtoms(atom);
            
      ServiceFactory.getInstance().getActivityService().record(event);
      
      //
      // Return the response
      //      
      
      AtomCreateResponse response = new AtomCreateResponse();
      response.setAtom(atom);
      
      return response;
    } catch (InvalidRequestException ire) {
      logger.error("create", ire);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TimedOutException toe) {
      logger.error("create", toe);      
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (UnavailableException ue) {
      logger.error("create", ue);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TException te) { 
      logger.error("create", te);
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
  public AtomUpdateResponse update(AtomUpdateRequest request) throws GeoCoordException, TException {

    Cassandra.Client client = null;
    
    try {
      //
      // Generate a point id and set the timestamp
      // Force the user from the Cookie.
      //
      
      Atom newatom = request.getNewAtom();
      Atom oldatom = request.getAtom();
      
      //
      // Make sure atoms are of the same type
      //
      
      if (!newatom.getType().equals(oldatom.getType())) {
        throw new GeoCoordException(GeoCoordExceptionCode.ATOM_TYPE_MISMATCH);
      }
      
      //
      // Make sure the ids are the same too
      //
    
      String id = null;
      
      switch (newatom.getType()) {
        case POINT:
          id = newatom.getPoint().getPointId();
          if (!id.equals(oldatom.getPoint().getPointId())) {
            throw new GeoCoordException(GeoCoordExceptionCode.ATOM_ID_MISMATCH);
          }
          break;          
      }
      
      
      long timestamp = System.currentTimeMillis();
      long nanooffset = ServiceFactory.getInstance().getCassandraHelper().getNanoOffset(); 
            
      //
      // Store new atom in the Cassandra backend
      //
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

      ByteBuffer col = ByteBuffer.allocate(16);
      col.order(ByteOrder.BIG_ENDIAN);
      
      // FIXME(hbs): externalize column name generation
      // Build the column name, i.e. <RTS><NANOOFFSET>
      col.putLong(Long.MAX_VALUE - timestamp);
      col.putLong(Long.MAX_VALUE - nanooffset);
      
      byte[] colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(newatom);
      
      StringBuilder sb = new StringBuilder(Constants.CASSANDRA_ATOM_ROWKEY_PREFIX);
      sb.append(id);
      String rowkey = sb.toString();
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(col.array());
      
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, timestamp, ConsistencyLevel.ONE);
      
      //
      // Return the client
      //
      
      ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);
      client = null;

      //
      // Now store an activity record for the layer and cell
      //
      
      ActivityEvent event = new ActivityEvent();
      event.setType(ActivityEventType.UPDATE);
      event.addToAtoms(oldatom);
      event.addToAtoms(newatom);
            
      ServiceFactory.getInstance().getActivityService().record(event);
      
      //
      // Return the response
      //      
      
      AtomUpdateResponse response = new AtomUpdateResponse();
      response.setAtom(newatom);
      
      return response;
    } catch (InvalidRequestException ire) {
      logger.error("update", ire);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TimedOutException toe) {
      logger.error("update", toe);      
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (UnavailableException ue) {
      logger.error("update", ue);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TException te) { 
      logger.error("update", te);
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

      String atomid = request.getAtomId();
      
      //
      // Attempt to retrieve the latest version of the atom
      //
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

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
      sb.append(atomid);
      
      String rowkey = sb.toString();
      
      // FIXME(hbs): fix consistency????
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

      String id = null;
      
      switch (atom.getType()) {
        case POINT:
          id = atom.getPoint().getPointId();
          break;
      }
            
      //
      // Store atom in the Cassandra backend
      //
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

      ByteBuffer col = ByteBuffer.allocate(16);
      col.order(ByteOrder.BIG_ENDIAN);
      
      // FIXME(hbs): externalize column name generation
      // Build the column name, i.e. <RTS><NANOOFFSET>
      col.putLong(Long.MAX_VALUE - timestamp);
      col.putLong(Long.MAX_VALUE - nanooffset);
      
      byte[] colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(atom);
      
      StringBuilder sb = new StringBuilder(Constants.CASSANDRA_ATOM_ROWKEY_PREFIX);
      sb.append(id);
      String rowkey = sb.toString();
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(col.array());
      
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, timestamp, ConsistencyLevel.ONE);
      
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
