package com.geocoord.server.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/*
import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.SlicePredicate;
*/
import org.apache.thrift.TException;

import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.data.PointStoreRequest;
import com.geocoord.thrift.data.PointStoreResponse;
import com.geocoord.thrift.services.PointService;

public class PointServiceImpl implements PointService.Iface {
  
  //@Override
  public PointStoreResponse store(PointStoreRequest request) throws GeoCoordException, TException {
    //
    // Loop through the points, splitting them into two categories, new/old
    // and assigning UUID to new ones on the fly
    //
    
    Map<String,Point> updates = new HashMap<String, Point>();
    Map<String,Point> creations = new HashMap<String, Point>();
    
    Set<String> layers = new HashSet<String>();
    for (Point point: request.getPoints()) {
      layers.add(point.getGclid());
      if (null != point.getGcpid()) {
        updates.put(point.getGcpid(), point);
      } else {
        point.setGcpid(UUID.randomUUID().toString());
        creations.put(point.getGcpid(), point);
      }
    }
    
    //
    // Retrieve the involved layers and check that the user is
    // allowed to use them.
    //
    
    
    
    /*
    Cassandra.Client client = new Cassandra.Client();
    ColumnParent cp = new ColumnParent(Constants.CASSANDRA_ADMATOMS_CF);
    List<String> layerKeys = new ArrayList<String>();
    layerKeys.addAll(layers);
    SlicePredicate slice = new SlicePredicate();
    slice.addToColumn_names(Constants.CASSANDRA_OWNER_COLUMN.getBytes());
    Map<String,List<ColumnOrSuperColumn>> layerData = client.multiget_slice(Constants.CASSANDRA_GEOCOORD_KEYSPACE, layerKeys, cp, slice, ConsistencyLevel.ONE);
    */
    
    //
    // Retrieve latest values of points to update and check that
    // the requesting user has the right to update them
    //
    
    return null;
  }
}
