package com.geocoord.lucene;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.bouncycastle.util.Arrays;
import org.junit.Test;


public class UUIDTokenStreamTestCase {

  @Test
  public void testPayload() throws IOException {
    UUIDTokenStream uts = new UUIDTokenStream();
    
    UUID uuid = UUID.randomUUID();
    long hhcode = 0xfedcba9876543210L;
    long timestamp = System.currentTimeMillis();
    
    uts.reset(uuid, hhcode, timestamp);
    Assert.assertTrue(uts.incrementToken());
    
    ByteBuffer bbuf = ByteBuffer.allocate(UUIDTokenStream.PAYLOAD_SIZE);
    bbuf.order(ByteOrder.BIG_ENDIAN);
    bbuf.putLong(uuid.getMostSignificantBits());
    bbuf.putLong(uuid.getLeastSignificantBits());
    bbuf.putLong(hhcode);
    bbuf.putLong(timestamp);
    
    PayloadAttribute pl = uts.getAttribute(PayloadAttribute.class);
            
    Assert.assertTrue(Arrays.areEqual(bbuf.array(), pl.getPayload().toByteArray()));
  }
}
