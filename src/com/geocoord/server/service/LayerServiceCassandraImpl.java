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
import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerCreateRequest;
import com.geocoord.thrift.data.LayerCreateResponse;
import com.geocoord.thrift.data.LayerRemoveRequest;
import com.geocoord.thrift.data.LayerRemoveResponse;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.LayerUpdateRequest;
import com.geocoord.thrift.data.LayerUpdateResponse;
import com.geocoord.thrift.services.LayerService;
import com.geocoord.util.LayerUtils;
import com.geocoord.util.NamingUtil;
import com.google.common.base.Charsets;

public class LayerServiceCassandraImpl implements LayerService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(LayerServiceCassandraImpl.class);
  
  // Secret should be at least 16 bytes long.
  private static final int MINIMUM_SECRET_SIZE = 16;
  
  @Override
  public LayerCreateResponse create(LayerCreateRequest request) throws GeoCoordException, TException {

    Cassandra.Client client = null;
    
    try {
      Layer layer = request.getLayer();

      //
      // Check that the provided name is valid
      //
      
      if (!NamingUtil.isValidLayerName(layer.getLayerId())) {
        throw new GeoCoordException(GeoCoordExceptionCode.LAYER_INVALID_NAME);
      }
            
      layer.setTimestamp(System.currentTimeMillis());
      
      //
      // Generate a HMAC key
      //
      
      byte[] secret = new byte[Constants.LAYER_HMAC_KEY_BYTE_SIZE];
      ServiceFactory.getInstance().getCryptoHelper().getSecureRandom().nextBytes(secret);
      layer.setSecret(new String(Base64.encode(secret)).replace("=", ""));
      
      //
      // Force the user
      //
      
      layer.setUserId(request.getCookie().getUserId());
      
      //
      // Store layer in the Cassandra backend
      //
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

      ByteBuffer col = ByteBuffer.allocate(16);
      col.order(ByteOrder.BIG_ENDIAN);
      
      // FIXME(hbs): externalize column name generation
      // Build the column name, i.e. <RTS><NANOOFFSET>
      col.putLong(Long.MAX_VALUE - layer.getTimestamp());
      col.putLong(Long.MAX_VALUE - ServiceFactory.getInstance().getCassandraHelper().getNanoOffset());
      
      byte[] colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(layer);
      
      String rowkey = LayerUtils.getLayerRowkey(layer);

      if (!ServiceFactory.getInstance().getCassandraHelper().lock(client, Constants.CASSANDRA_KEYSPACE, Constants.CASSANDRA_HISTORICAL_DATA_COLFAM, rowkey, col.array(), colvalue, true)) {
        throw new GeoCoordException(GeoCoordExceptionCode.LAYER_ALREADY_EXIST);
      }
      
      //
      // Store the layer creation in a per user row.
      // This is done so we can count the number of layers per user.
      //
      // Row key is UL<USER UUID>
      // Column key is <LAYER ID>
      //

      rowkey = LayerUtils.getUserLayerRowkey(layer);
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(layer.getLayerId().getBytes(Charsets.UTF_8));
      
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, layer.getTimestamp(), ConsistencyLevel.ONE);
            
      //
      // Return the response
      //      
      
      LayerCreateResponse response = new LayerCreateResponse();
      response.setLayer(layer);
      
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
  public LayerRetrieveResponse retrieve(LayerRetrieveRequest request) throws GeoCoordException, TException {
    
    Cassandra.Client client = null;
    
    try {
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

      //
      // Retrieve the last version of the layer data
      //
      
      Layer layer = new Layer();
      layer.setLayerId(request.getLayerId());      
      String rowkey = LayerUtils.getLayerRowkey(layer);
      
      SlicePredicate slice = new SlicePredicate();
      
      SliceRange range = new SliceRange();
      range.setCount(1);
      range.setStart(new byte[0]);
      range.setFinish(new byte[0]);
      range.setReversed(false);
      
      slice.setSlice_range(range);
      
      ColumnParent colparent = new ColumnParent();
      colparent.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      
      List<ColumnOrSuperColumn> coscs = client.get_slice(Constants.CASSANDRA_KEYSPACE, rowkey, colparent, slice, ConsistencyLevel.ONE);
      
      if (1 != coscs.size()) {
        throw new GeoCoordException(GeoCoordExceptionCode.LAYER_NOT_FOUND);
      }
            
      //
      // Deserialize data
      //
      
      ServiceFactory.getInstance().getThriftHelper().deserialize(layer, coscs.get(0).getColumn().getValue());
      
      //
      // If the deleted flag is true, throw an exception
      //
      
      if (layer.isDeleted()) {
        throw new GeoCoordException(GeoCoordExceptionCode.LAYER_DELETED);
      }
      
      LayerRetrieveResponse response = new LayerRetrieveResponse();
      response.addToLayers(layer);
      
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
  public LayerUpdateResponse update(LayerUpdateRequest request) throws GeoCoordException, TException {
    
    Layer layer = request.getLayer();
    
    //
    // Make sure the OAuth secret is still set
    //
    
    if (null == layer.getSecret() || layer.getSecret().length() < MINIMUM_SECRET_SIZE) {
      throw new GeoCoordException(GeoCoordExceptionCode.LAYER_MISSING_SECRET);
    }
    
    //
    // Store the new version
    //

    // Update timestamp otherwise it won't be stored at the right place
    
    layer.setTimestamp(System.currentTimeMillis());
    
    Cassandra.Client client = null;
    
    try {
      //
      // Store layer in the Cassandra backend
      //
      
      client = ServiceFactory.getInstance().getCassandraHelper().holdClient(Constants.CASSANDRA_CLUSTER);

      ByteBuffer col = ByteBuffer.allocate(16);
      col.order(ByteOrder.BIG_ENDIAN);
      
      // FIXME(hbs): externalize column name generation
      // Build the column name, i.e. <RTS><NANOOFFSET>
      col.putLong(Long.MAX_VALUE - layer.getTimestamp());
      col.putLong(Long.MAX_VALUE - ServiceFactory.getInstance().getCassandraHelper().getNanoOffset());
      
      byte[] colvalue = ServiceFactory.getInstance().getThriftHelper().serialize(layer);

      String rowkey = LayerUtils.getLayerRowkey(layer);
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(col.array());
      
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, layer.getTimestamp(), ConsistencyLevel.ONE);

      //
      // Remove the layer from the per user row.
      // This is done so we can count the number of layers per user.
      //

      rowkey = LayerUtils.getUserLayerRowkey(layer);      
      colpath.setColumn(layer.getLayerId().getBytes(Charsets.UTF_8));
      
      if (!layer.isDeleted()) {
        // Update per user layer
        client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, layer.getTimestamp(), ConsistencyLevel.ONE);
      } else {
        // Remove per user layer
        //
        // timestamp is the time of remove, as was specified in the following conversation on #cassandra (2010-03-31)
        //
        // [23:35] hbs: Does the remove API call need the exact timestamp of the column to remove or does it need to be 'after' the one in the columns to remove?
        // [23:35] jbathgate: hbs: after the one in the columns
        // [23:35] hbs: ok cool, because the API wiki page seems to indicate it's the exact value that's needed.
        // [23:35] jbathgate: the exact timestamp of when the delete occurred
        // [23:36] hbs: jbathgate: ok, thanks
        //
        
        client.remove(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, System.currentTimeMillis(), ConsistencyLevel.ONE);
      }

      //
      // Return the response
      //      
      
      LayerUpdateResponse response = new LayerUpdateResponse();
      response.setLayer(layer);
      
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
  public LayerRemoveResponse remove(LayerRemoveRequest request) throws GeoCoordException, TException {
    //
    // Insert a deleting marker for this layer
    //
    
    Layer layer = request.getLayer();
      
    // Force the deletion flag
    layer.setDeleted(true);
      
    LayerUpdateRequest lur = new LayerUpdateRequest();
    lur.setLayer(layer);
    update(lur);

    //
    // TODO(hbs): De-index all atoms belonging to this layer..
    //
    
    //
    // Return the response
    //      
      
    LayerRemoveResponse response = new LayerRemoveResponse();
    response.setLayer(layer);
    
    return response;
  }
}
