package com.geocoord.lucene;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.bouncycastle.util.encoders.Base64;

import com.geocoord.util.CryptoUtil;
import com.google.common.base.Charsets;

public class AttributeTokenStream extends TokenStream {

  public static final String INDEXED_ATTRIBUTE_PREFIX = "~";
  private final TermAttribute termAttr;
  private final TokenStream input;
  private final ByteBuffer bbuf;
  
  public AttributeTokenStream(TokenStream input) {
    this.input = input;
    // The TermAttribute we will return
    this.termAttr = addAttribute(TermAttribute.class);
    this.bbuf = ByteBuffer.allocate(8);
    this.bbuf.order(ByteOrder.BIG_ENDIAN);
  }

  @Override
  public boolean incrementToken() throws IOException {
    
    //
    // Only attributes that start with '#' are indexed
    //
    
    TermAttribute term = null;
    
    while(this.input.incrementToken()) {
      //
      // Retrieve next key=value 
      //

      term = input.getAttribute(TermAttribute.class);
      
      if (term.term().startsWith(INDEXED_ATTRIBUTE_PREFIX)) {
        break;
      }
      
      term = null;
    }
    
    if (null == term) {
      return false;
    }
    
    //
    // Compute FNV1a64 of term. Trim INDEXED_ATTRIBUTE_PREFIX
    //
    
    byte[] termbytes = term.term().getBytes(Charsets.UTF_8);
    long fnv = CryptoUtil.FNV1a64(termbytes, 1, termbytes.length - 1);
    
    this.bbuf.rewind();
    this.bbuf.putLong(fnv);
    
    // Output its b64 representation, striping the end '='
    this.termAttr.setTermBuffer(new String(Base64.encode(this.bbuf.array()), 0, 11, Charsets.UTF_8));
    
    return true;
  }
}
