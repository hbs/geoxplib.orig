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

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.lucene.GeoCoordIndex;
import com.geocoord.lucene.GeoDataSegmentCache;
import com.geocoord.lucene.IndexManager;
import com.geocoord.lucene.UUIDTokenStream;
import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.Atom;
import com.geocoord.thrift.data.GeoCoordException;
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
   * Per thread Document instance
   */
  private ThreadLocal<Document> perThreadDocument = new ThreadLocal<Document>() {
    @Override
    protected Document initialValue() { return new Document(); }
  };
  
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
      
      try {
        switch(atom.getType()) {
          case POINT:
            Point point = atom.getPoint();
            doStorePoint(point);
            break;
        }        
      } catch (IOException ioe) {
        logger.error("doRemove", ioe);
      }
    }        
  }
  
  private void doStorePoint(Point point) throws IOException {

    // FIXME(hbs): Is it safe to reuse Document instance???
    Document doc = perThreadDocument.get();
    UUIDTokenStream uuidTokenStream = perThreadUUIDTokenStream.get();
    ByteBuffer bb = perThreadByteBuffer.get();
    
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
    
    // Add HHCode
    field = new Field(GeoCoordIndex.GEO_FIELD, HHCodeHelper.toString(hhcode), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);      
    doc.add(field);

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
    // Add document to the index
    //
    
    manager.getWriter().addDocument(doc);  
  }
}
