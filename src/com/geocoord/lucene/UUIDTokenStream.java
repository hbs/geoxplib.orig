package com.geocoord.lucene;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Payload;

/**
 * This class emits a unique Term with an attached payload.
 * The payload contains the document UUID, its position (HHCode) and timestamp.
 * 
 * The payload can be loaded in memory by GeoCoordIndexSearcher
 */

public class UUIDTokenStream extends TokenStream {

  public enum PayloadAttr {
    UUID_MSB,
    UUID_LSB,
    HHCODE,
    TIMESTAMP,
  };
  
  public static final int PAYLOAD_SIZE = 32;

  /**
   * Offset for the Most Significant Bits of the UUID
   */
  public static final int UUIDMSB_OFFSET = 0;

  /**
   * Offset for the Least Significant Bits of the UUID
   */
  public static final int UUIDLSB_OFFSET = 8;
  
  /**
   * Offset for the HHCode
   */
  public static final int HHCODE_OFFSET = 16;
  
  /**
   * Offset for the Timestamp 
   */
  public static final int TIMESTAMP_OFFSET = 24;
  
  /**
   * Value of the UUID term
   */
  public static final String UUIDTermValue = "UUID-HHCODE-TS";

  /**
   * ByteBuffer for payload UUID (128bits)|HHCode (64bits)|Timestamp (64bits)
   */
  private ByteBuffer payload = ByteBuffer.allocate(PAYLOAD_SIZE);
    
  private TermAttribute termAttr = null;
  private PayloadAttribute payloadAttr = null;
  
  private boolean reset = false;
  
  public UUIDTokenStream() {
    // Force byte order to BIG_ENDIAN
    payload.order(ByteOrder.BIG_ENDIAN);
    
    this.termAttr = addAttribute(TermAttribute.class);
    this.termAttr.setTermBuffer(UUIDTermValue);

    this.payloadAttr = addAttribute(PayloadAttribute.class);
  }

  /**
   * Reset the token stream for a new doc
   * 
   * @param uuid UUID of the doc
   * @param hhcode HHCode of the doc
   * @param timestamp Timestamp of the doc
   */
  public void reset(UUID uuid, long hhcode, long timestamp) {
    this.payload.rewind();
    this.payload.putLong(UUIDMSB_OFFSET, uuid.getMostSignificantBits());
    this.payload.putLong(UUIDLSB_OFFSET, uuid.getLeastSignificantBits());
    this.payload.putLong(HHCODE_OFFSET, hhcode);
    this.payload.putLong(TIMESTAMP_OFFSET, timestamp);
    this.reset = true;
    
    // Copy the payload
    byte[] pl = new byte[PAYLOAD_SIZE];
    payload.get(pl);
    this.payloadAttr.setPayload(new Payload(pl));
  }
  
  /**
   * Advance stream to next token.
   * Since we're only emitting one token, set 'reset' to false
   * if it was true.
   */
  @Override
  public boolean incrementToken() throws IOException {
    if (reset) {
      // Only return one token
      reset = false;
      return true;
    } else {
      return false;
    }
  }
  
  public static long getPayloadAttribute(ByteBuffer payload, PayloadAttr attr) {
    
    payload.order(ByteOrder.BIG_ENDIAN);
    
    switch (attr) {
      case UUID_MSB:
        return payload.getLong(UUIDMSB_OFFSET);
      case UUID_LSB:
        return payload.getLong(UUIDLSB_OFFSET);
      case HHCODE:
        return payload.getLong(HHCODE_OFFSET);
      case TIMESTAMP:
        return payload.getLong(TIMESTAMP_OFFSET);
    }
    
    // This should NEVER be reached if the above switch covers all of PayloadAttribute
    return 0L;
  }
}
