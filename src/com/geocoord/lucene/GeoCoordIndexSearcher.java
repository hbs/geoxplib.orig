package com.geocoord.lucene;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.search.IndexSearcher;

import com.geocoord.lucene.UUIDTokenStream.PayloadAttr;

public class GeoCoordIndexSearcher extends IndexSearcher {
  
  /**
   * Per docid hhcode
   */
  private long[] hhcodes = null;
  
  /**
   * Per docid UUID MSB
   */
  private long[] uuidmsb = null;
  
  /**
   * Per docid UUID LSB
   */
  private long[] uuidlsb = null;
  
  /**
   * Per docid timestamp
   */
  private long[] timestamps = null;
  
  public GeoCoordIndexSearcher(IndexReader reader) throws IOException {
    super(reader);
    retrieveUUIDPayloads();
  }  
  
  /**
   * Retrieve per doc UUID payload (@see UUIDTokenStream)
   */
  private void retrieveUUIDPayloads() throws IOException {
    IndexReader reader = this.getIndexReader();
    
    //
    // Allocate payload arrays 
    //
    hhcodes = new long[reader.maxDoc()];
    uuidmsb = new long[reader.maxDoc()];
    uuidlsb = new long[reader.maxDoc()];
    timestamps = new long[reader.maxDoc()];
    
    //
    // Loop on term positions for UUID
    //
    
    TermPositions tp = null;
    
    try {
      tp = reader.termPositions(new Term(GeoCoordIndex.ID_FIELD, UUIDTokenStream.UUIDTermValue));
      
      ByteBuffer payload = null;
          
      while(tp.next()) {
        
        int docid = tp.doc();
        tp.nextPosition();
        
        //
        // Retrieve payload
        //
        
        
        if (tp.isPayloadAvailable()) {
          int len = tp.getPayloadLength();
          
          // Allocate the ByteBuffer if the payload size changed or
          // if it's the first allocation
          if (null == payload || payload.limit() != len) {
            payload = ByteBuffer.allocate(len);
          }
          
          tp.getPayload(payload.array(), 0);

          hhcodes[docid] = UUIDTokenStream.getPayloadAttribute(payload, PayloadAttr.HHCODE);
          uuidmsb[docid] = UUIDTokenStream.getPayloadAttribute(payload, PayloadAttr.UUID_MSB);
          uuidlsb[docid] = UUIDTokenStream.getPayloadAttribute(payload, PayloadAttr.UUID_LSB);
          timestamps[docid] = UUIDTokenStream.getPayloadAttribute(payload, PayloadAttr.TIMESTAMP);
        }
      }      
    } finally {
      if (null != tp) {
        tp.close();
      }
    }
  }
  
  public long getHHCode(int docId) {
    return this.hhcodes[docId];
  }
  public long getUUIDMSB(int docId) {
    return this.uuidmsb[docId];
  }
  public long getUUIDLSB(int docId) {
    return this.uuidlsb[docId];
  }
}
