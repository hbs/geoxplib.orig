package com.geocoord.server.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexWriter;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.geo.Coverage;
import com.geocoord.geo.HHCodeHelper;
import com.geocoord.lucene.GeoCoordIndex;
import com.geocoord.lucene.GeoDataSegmentCache;
import com.geocoord.lucene.IndexManager;
import com.geocoord.lucene.UUIDTokenStream;
import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.Geofence;
import com.geocoord.thrift.data.Point;
import com.geocoord.thrift.services.ActivityService;
import com.geocoord.util.NamingUtil;
import com.google.inject.Inject;

/**
 * ActivityService which uses Lucene to index the atoms.
 * 
 */
public class ActivityServiceLuceneIndexer implements ActivityService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(ActivityServiceLuceneIndexer.class);
  
  private IndexManager manager;
  
  @Inject
  public ActivityServiceLuceneIndexer(IndexManager manager) {
    this.manager = manager;
  }
  
  /**
   * Per thread ByteBuffer instance
   */
  private ThreadLocal<ByteBuffer> perThreadByteBuffer = new ThreadLocal<ByteBuffer>() {
    @Override
    protected ByteBuffer initialValue() { ByteBuffer bb = ByteBuffer.allocate(16); bb.order(ByteOrder.BIG_ENDIAN); return bb; }
  };
  
  /**
   * Per thread UUIDTokenStream instance
   */
  private ThreadLocal<UUIDTokenStream> perThreadUUIDTokenStream = new ThreadLocal<UUIDTokenStream>() {
    @Override
    protected UUIDTokenStream initialValue() { return new UUIDTokenStream(); }
  };
  
  @Override
  public void record(ActivityEvent event) throws GeoCoordException, TException {
    
    switch(event.getType()) {
      case REMOVE:
        doRemove(event);
        break;
      case STORE:
        doStore(event);
        break;
    }
  }
  
  /**
   * Perform the removal of Atoms from the index.
   * 
   * @param event Removal event
   * @throws GeoCoordException
   */
  private void doRemove(ActivityEvent event) throws GeoCoordException {
    
    ByteBuffer bb = perThreadByteBuffer.get();
    
    for (Atom atom: event.getAtoms()) {
      
      bb.rewind();
      
      try {
        switch(atom.getType()) {
          case POINT:
            Point point = atom.getPoint();
            bb.put(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(point.getLayerId(), point.getPointId())));
            GeoDataSegmentCache.deleteByUUID(manager.getWriter(), bb.getLong(0), bb.getLong(8));
            break;
        }        
      } catch (IOException ioe) {
        logger.error("doRemove", ioe);
      }
    }    
  }

  /**
   * Store Atoms in the index.
   * 
   * @param event
   * @throws GeoCoordException
   */
  private void doStore(ActivityEvent event) throws GeoCoordException {
    for (Atom atom: event.getAtoms()) {
      
      if (!atom.isIndexed()) {
        continue;
      }
      try {
        switch(atom.getType()) {
          case POINT:
            Point point = atom.getPoint();            
            doStorePoint(point);
            break;
          case GEOFENCE:
            Geofence geofence = atom.getGeofence();
            doStoreGeofence(geofence);
            break;
        }        
      } catch (IOException ioe) {
        logger.error("doStore", ioe);
      }
    }        
  }
  
  private void doStoreGeofence(Geofence geofence) throws IOException {

    Document doc = new Document();
    UUIDTokenStream uuidTokenStream = perThreadUUIDTokenStream.get();
    ByteBuffer bb = perThreadByteBuffer.get();
    bb.rewind();
    
    //
    // Compute UUID of point
    //
    
    bb.put(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(geofence.getLayerId(), geofence.getGeofenceId())));
    UUID uuid = new UUID(bb.getLong(0), bb.getLong(8));
    
    //
    // Reset UUIDTokenStream with geofence data. We use the hhcode field
    // to store the area of the geofence.
    //
    
    long area = new Coverage(geofence.getCells()).area();
    
    uuidTokenStream.reset(uuid,area,geofence.getTimestamp());
    
    // Attach payload to ID field
    Field field = new Field(GeoCoordIndex.ID_FIELD, uuidTokenStream);      
    doc.add(field);

    // Add Type
    field = new Field(GeoCoordIndex.TYPE_FIELD, "GEOFENCE", Store.NO, Index.NOT_ANALYZED_NO_NORMS, TermVector.NO);
    doc.add(field);

    //
    // Generate a String representation of the coverage
    //
    
    Coverage c = new Coverage(geofence.getCells());
    
    // Add HHCodes for the coverage, use # prefix so the cells are indexed as is and not split
    // by the analyzer
    field = new Field(GeoCoordIndex.GEO_FIELD, c.toString(" ", "#"), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);      
    doc.add(field);

    // Add Layer
    field = new Field(GeoCoordIndex.LAYER_FIELD, geofence.getLayerId(), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
    doc.add(field);

    // Add attributes
    if (geofence.getAttributesSize() > 0) {
      StringBuilder sb = new StringBuilder();
      
      for (String attr: geofence.getAttributes().keySet()) {
        sb.setLength(0);
        sb.append(attr);
        for (String value: geofence.getAttributes().get(attr)) {
          sb.setLength(attr.length());
          sb.append(":");
          sb.append(value);
          field = new Field(GeoCoordIndex.ATTR_FIELD, sb.toString(), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
          doc.add(field);
        }        
      }
    }
    
    // Add tags
    if (null != geofence.getTags()) {
      field = new Field(GeoCoordIndex.TAGS_FIELD, geofence.getTags(), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
      doc.add(field);
    }
    
    // Add User
    field = new Field(GeoCoordIndex.USER_FIELD, geofence.getUserId(), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
    doc.add(field);

    // TODO(hbs): add timestamp
    
    //
    // Delete potential previous version of geofence
    //
    
    GeoDataSegmentCache.deleteByUUID(manager.getWriter(), uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    
    //
    // Add document to the index
    //
    
    manager.getWriter().addDocument(doc);
    
  }
  
  /**
   * Store a POINT Atom in the index.
   * 
   * @param point
   * @throws IOException
   */
  private void doStorePoint(Point point) throws IOException {

    Document doc = new Document();
    UUIDTokenStream uuidTokenStream = perThreadUUIDTokenStream.get();
    ByteBuffer bb = perThreadByteBuffer.get();
    bb.rewind();
    
    //
    // Compute HHCode of point
    //
        
    long hhcode = point.getHhcode();

    //
    // Compute UUID of point
    //
    
    bb.put(NamingUtil.getDoubleFNV(NamingUtil.getLayerAtomName(point.getLayerId(), point.getPointId())));
    UUID uuid = new UUID(bb.getLong(0), bb.getLong(8));
    
    //
    // Reset UUIDTokenStream with point data
    //
    
    uuidTokenStream.reset(uuid,hhcode,point.getTimestamp());
    
    // Attach payload to ID field
    Field field = new Field(GeoCoordIndex.ID_FIELD, uuidTokenStream);      
    doc.add(field);

    // Add Type
    field = new Field(GeoCoordIndex.TYPE_FIELD, "POINT", Store.NO, Index.NOT_ANALYZED_NO_NORMS, TermVector.NO);
    doc.add(field);

    String hhstr = HHCodeHelper.toString(hhcode);
    
    // Add HHCodes for the atom's cells
    field = new Field(GeoCoordIndex.GEO_FIELD, hhstr, Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);      
    doc.add(field);

    // Add parent cells
    //field = new Field(GeoCoordIndex.GEO_PARENTS_FIELD, hhstr, Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);      
    //doc.add(field);

    // Add Layer
    field = new Field(GeoCoordIndex.LAYER_FIELD, point.getLayerId(), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
    doc.add(field);

    // Add attributes
    if (point.getAttributesSize() > 0) {
      StringBuilder sb = new StringBuilder();
      
      for (String attr: point.getAttributes().keySet()) {
        sb.setLength(0);
        sb.append(attr);
        for (String value: point.getAttributes().get(attr)) {
          sb.setLength(attr.length());
          sb.append(":");
          sb.append(value);
          field = new Field(GeoCoordIndex.ATTR_FIELD, sb.toString(), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
          doc.add(field);
        }        
      }
    }
    
    // Add tags
    if (null != point.getTags()) {
      field = new Field(GeoCoordIndex.TAGS_FIELD, point.getTags(), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
      doc.add(field);
    }
    
    // Add User
    field = new Field(GeoCoordIndex.USER_FIELD, point.getUserId(), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
    doc.add(field);

    // TODO(hbs): add timestamp
    
    //
    // Delete potential previous version of point
    //
    
    GeoDataSegmentCache.deleteByUUID(manager.getWriter(), uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    
    //
    // Add document to the index
    //
    
    manager.getWriter().addDocument(doc);
  }
}
