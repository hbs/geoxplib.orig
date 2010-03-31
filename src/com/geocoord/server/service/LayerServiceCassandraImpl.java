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
import com.geocoord.thrift.data.LayerCreateRequest;
import com.geocoord.thrift.data.LayerCreateResponse;
import com.geocoord.thrift.data.LayerRemoveRequest;
import com.geocoord.thrift.data.LayerRemoveResponse;
import com.geocoord.thrift.data.LayerRetrieveRequest;
import com.geocoord.thrift.data.LayerRetrieveResponse;
import com.geocoord.thrift.data.LayerUpdateRequest;
import com.geocoord.thrift.data.LayerUpdateResponse;
import com.geocoord.thrift.services.LayerService;

public class LayerServiceCassandraImpl implements LayerService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(LayerServiceCassandraImpl.class);
  
  @Override
  public LayerCreateResponse create(LayerCreateRequest request) throws GeoCoordException, TException {

    Cassandra.Client client = null;
    
    try {
      //
      // Generate a layer id and set the timestamp
      //
      
      UUID layerId = UUID.randomUUID();
      
      Layer layer = request.getLayer();
      layer.setLayerId(layerId.toString());
      layer.setTimestamp(System.currentTimeMillis());
      
      //
      // Generate a HMAC key
      //
      
      layer.setHmacKey(new byte[Constants.LAYER_HMAC_KEY_BYTE_SIZE]);
      ServiceFactory.getInstance().getCryptoHelper().getSecureRandom().nextBytes(layer.getHmacKey());
      
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
      
      StringBuilder sb = new StringBuilder(Constants.CASSANDRA_LAYER_ROWKEY_PREFIX);
      sb.append(layer.getLayerId());
      String rowkey = sb.toString();
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(col.array());
      
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, layer.getTimestamp(), ConsistencyLevel.ONE);
      
      //
      // Store the layer creation in a per user row.
      // This is done so we can count the number of layers per user.
      //
      // Row key is UL<USER UUID>
      // Column key is <LAYER UUID>
      //
      
      col.rewind();
      col.putLong(layerId.getMostSignificantBits());
      col.putLong(layerId.getLeastSignificantBits());
      
      sb.setLength(0);
      sb.append(Constants.CASSANDRA_USERLAYERS_ROWKEY_PREFIX);
      sb.append(layer.getUserId());
      rowkey = sb.toString();
      
      colpath.setColumn(col.array());
      
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
      
      StringBuilder sb = new StringBuilder();
      sb.append(Constants.CASSANDRA_LAYER_ROWKEY_PREFIX);
      sb.append(request.getLayerId());
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
        throw new GeoCoordException(GeoCoordExceptionCode.LAYER_NOT_FOUND);
      }
            
      //
      // Deserialize data
      //
      
      Layer layer = new Layer();
      ServiceFactory.getInstance().getThriftHelper().deserialize(layer, coscs.get(0).getColumn().getValue());
      
      //
      // If the deleted flag is true, throw an exception
      //
      
      if (layer.isDeleted()) {
        throw new GeoCoordException(GeoCoordExceptionCode.LAYER_DELETED);
      }
      
      LayerRetrieveResponse response = new LayerRetrieveResponse();
      response.setLayer(layer);
      
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
    // Make sure the HMAC key is still set
    //
    
    if (null == layer.getHmacKey() || Constants.LAYER_HMAC_KEY_BYTE_SIZE != layer.getHmacKey().length) {
      throw new GeoCoordException(GeoCoordExceptionCode.LAYER_MISSING_HMAC);
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
      
      StringBuilder sb = new StringBuilder(Constants.CASSANDRA_LAYER_ROWKEY_PREFIX);
      sb.append(layer.getLayerId());
      String rowkey = sb.toString();
      
      ColumnPath colpath = new ColumnPath();
      colpath.setColumn_family(Constants.CASSANDRA_HISTORICAL_DATA_COLFAM);
      colpath.setColumn(col.array());
      
      client.insert(Constants.CASSANDRA_KEYSPACE, rowkey, colpath, colvalue, layer.getTimestamp(), ConsistencyLevel.ONE);

      //
      // Remove the layer from the per user row.
      // This is done so we can count the number of layers per user.
      //
      // Row key is UL<USER UUID>
      // Column key is <LAYER UUID>
      //
      
      UUID layerId = UUID.fromString(layer.getLayerId());
      
      col.rewind();
      col.putLong(layerId.getMostSignificantBits());
      col.putLong(layerId.getLeastSignificantBits());
      
      sb.setLength(0);
      sb.append(Constants.CASSANDRA_USERLAYERS_ROWKEY_PREFIX);
      sb.append(layer.getUserId());
      rowkey = sb.toString();
      
      colpath.setColumn(col.array());
      
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
    // Return the response
    //      
      
    LayerRemoveResponse response = new LayerRemoveResponse();
    response.setLayer(layer);
    
    return response;
  }
}
