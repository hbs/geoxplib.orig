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
    
    if (!this.input.incrementToken()) {
      return false;      
    }
    
    //
    // Retrieve next key=value 
    //

    TermAttribute term = input.getAttribute(TermAttribute.class);

    //
    // Compute FNV1a64 of term
    //
    
    long fnv = CryptoUtil.FNV1a64(term.term().getBytes(Charsets.UTF_8));
    
    this.bbuf.rewind();
    this.bbuf.putLong(fnv);
    
    // Output its b64 representation
    this.termAttr.setTermBuffer(new String(Base64.encode(this.bbuf.array()), Charsets.UTF_8));
    
    return true;
  }
}
