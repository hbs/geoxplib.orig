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
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.data.UserAliasRequest;
import com.geocoord.thrift.data.UserAliasResponse;
import com.geocoord.thrift.data.UserCreateRequest;
import com.geocoord.thrift.data.UserCreateResponse;
import com.geocoord.thrift.data.UserRetrieveRequest;
import com.geocoord.thrift.data.UserRetrieveResponse;
import com.geocoord.thrift.services.UserService;
import com.google.common.base.Charsets;

public class UserServiceCassandraImpl implements UserService.Iface {

  private static final Logger logger = LoggerFactory.getLogger(UserServiceCassandraImpl.class);
  
  @Override
  public UserCreateResponse create(UserCreateRequest request) throws GeoCoordException, TException {

    Cassandra.Client client = null;
    
    try {
      //
      // Generate a layer id and set the timestamp
      //
      
      UUID userId = UUID.randomUUID();
      
      User user = request.getUser();
      user.setUserId(userId.toString());
      user.setTimestamp(System.currentTimeMillis());
      
      //
      // Generate a HMAC key
      //
      
      user.setSecret(new byte[Constants.USER_HMAC_KEY_BYTE_SIZE]);
      ServiceFactory.getInstance().getCryptoHelper().getSecureRandom().nextBytes(user.getSecret());
      
      //
      // Store user in the Cassandra backend
      //
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

      ByteBuffer col = ByteBuffer.allocate(16);
      col.order(ByteOrder.BIG_ENDIAN);
      
      // FIXME(hbs): externalize column name generation
      // Build the column name, i.e. <RTS><NANOOFFSET>
      col.putLong(Long.MAX_VALUE - user.getTimestamp());
      col.putLong(Long.MAX_VALUE - ServiceFactory.getInstance().getCassandraHelper().getNanoOffset());
      
      byte[] colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(user);
      
      StringBuilder sb = new StringBuilder(Constants.CASSANDRA_USER_ROWKEY_PREFIX);
      sb.append(user.getUserId());
      String rowkey = sb.toString();
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(col.array());
      
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, user.getTimestamp(), ConsistencyLevel.ONE);
                  
      //
      // Return the response
      //      
      
      UserCreateResponse response = new UserCreateResponse();
      response.setUser(user);
      
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
  public UserAliasResponse alias(UserAliasRequest request) throws GeoCoordException, TException {

    Cassandra.Client client = null;
    
    try {
      //
      // Store user in the Cassandra backend
      //
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);
      
      byte[] colvalue = request.getUserId().getBytes(Charsets.UTF_8);
      
      StringBuilder sb = new StringBuilder(Constants.CASSANDRA_USER_ALIAS_ROWKEY_PREFIX);
      sb.append(request.getAlias());
      String rowkey = sb.toString();
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      // Allocate dummy column name
      colpath.setColumn(new byte[1]);
      
      // We assume the identity has been verified so we don't attempt anything like a lock
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, System.currentTimeMillis(), ConsistencyLevel.QUORUM);
                  
      //
      // Return the response
      //      
      
      UserAliasResponse response = new UserAliasResponse();      
      return response;
    } catch (InvalidRequestException ire) {
      logger.error("alias", ire);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TimedOutException toe) {
      logger.error("alias", toe);      
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (UnavailableException ue) {
      logger.error("alias", ue);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TException te) { 
      logger.error("alias", te);
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
  public UserRetrieveResponse retrieve(UserRetrieveRequest request) throws GeoCoordException, TException {
    
    //
    // If alias is set, retrieve user by alias
    //
    
    if (null == request.getUserId() && null != request.getAlias()) {
      return retrieveByAlias(request);
    }
    
    Cassandra.Client client = null;
    
    try {
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

      //
      // Retrieve the last version of the user data
      //
      
      StringBuilder sb = new StringBuilder();
      sb.append(Constants.CASSANDRA_USER_ROWKEY_PREFIX);
      sb.append(request.getUserId());
      String rowkey = sb.toString();
      
      SlicePredicate slice = new SlicePredicate();
      
      SliceRange range = new SliceRange();
      range.setCount(1);
      range.setStart(new byte[0]);
      range.setFinish(new byte[0]);

      slice.setSlice_range(range);
      
      ColumnParent colparent = new ColumnParent();
      colparent.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      
      List<ColumnOrSuperColumn> coscs = client.get_slice(Constants.CASSANDRA_KEYSPACE, rowkey, colparent, slice, ConsistencyLevel.ONE);
      
      if (1 != coscs.size()) {
        throw new GeoCoordException(GeoCoordExceptionCode.USER_NOT_FOUND);
      }
            
      //
      // Deserialize data
      //
      
      User user = new User();
      ServiceFactory.getInstance().getThriftHelper().deserialize(user, coscs.get(0).getColumn().getValue());
      
      /*
      //
      // If the deleted flag is true, throw an exception
      //
            
      if (user.isDeleted()) {
        throw new GeoCoordException(GeoCoordExceptionCode.USER_DELETED);
      }
      */
      
      UserRetrieveResponse response = new UserRetrieveResponse();
      response.setUser(user);
      
      //
      // If instructed to do so, retrieve the layers
      //
      
      if (!request.isSetIncludeLayers()) {
        return response;
      }
   
      //
      // Read layers (at most maxLayers)
      // FIXME(hbs): take care of large values of maxLayers later on
      //
      
      range.setCount(user.getMaxLayers());
      range.setStart(new byte[0]);
      range.setFinish(new byte[0]);
      slice.setSlice_range(range);
      
      List<ColumnOrSuperColumn> cols = client.get_slice(Constants.CASSANDRA_KEYSPACE, rowkey, colparent, slice, ConsistencyLevel.QUORUM);
      
      for (ColumnOrSuperColumn col: cols) {
        Layer layer = new Layer();
        ServiceFactory.getInstance().getThriftHelper().deserialize(layer, col.getColumn().getValue());
        response.addToLayers(layer);
      }
      
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

  private UserRetrieveResponse retrieveByAlias(UserRetrieveRequest request) throws GeoCoordException, TException {
    Cassandra.Client client = null;
    
    try {
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

      //
      // Retrieve the uuid associated with this alias
      //
      
      StringBuilder sb = new StringBuilder();
      sb.append(Constants.CASSANDRA_USER_ALIAS_ROWKEY_PREFIX);
      sb.append(request.getAlias());
      String rowkey = sb.toString();
      
      SlicePredicate slice = new SlicePredicate();
      
      SliceRange range = new SliceRange();
      range.setCount(1);
      range.setStart(new byte[0]);
      range.setFinish(new byte[0]);

      slice.setSlice_range(range);
      
      ColumnParent colparent = new ColumnParent();
      colparent.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      
      List<ColumnOrSuperColumn> coscs = client.get_slice(Constants.CASSANDRA_KEYSPACE, rowkey, colparent, slice, ConsistencyLevel.ONE);
      
      //
      // Release client
      //
      
      ServiceFactory.getInstance().getCassandraHelper().releaseClient(client);        
      client = null;
      
      if (1 != coscs.size()) {
        throw new GeoCoordException(GeoCoordExceptionCode.USER_NOT_FOUND);
      }
            
      //
      // Rebuild UUID
      //
      
      UUID uuid = UUID.fromString(new String(coscs.get(0).getColumn().getValue(), Charsets.UTF_8));
      
      //
      // Retrieve user by UUID
      //
      
      request.setUserId(uuid.toString());
      request.setAlias(null);
      return retrieve(request);      
    } catch (InvalidRequestException ire) {
      logger.error("retrieveByAlias", ire);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TimedOutException toe) {
      logger.error("retrieveByAlias", toe);      
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (UnavailableException ue) {
      logger.error("retrieveByAlias", ue);
      throw new GeoCoordException(GeoCoordExceptionCode.CASSANDRA_ERROR);
    } catch (TException te) { 
      logger.error("retrieveByAlias", te);
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
  
  public com.geocoord.thrift.data.UserUpdateResponse update(com.geocoord.thrift.data.UserUpdateRequest request) throws GeoCoordException ,TException {
    return null;
  };
}
