package com.geocoord.lucene;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.queryParser.QueryParser;
import org.bouncycastle.util.encoders.Base64;

import com.geocoord.util.CryptoUtil;
import com.google.common.base.Charsets;

public class AttributeTokenStream extends TokenStream {

  public static final String INDEXED_ATTRIBUTE_PREFIX = "~";
  public static final String ATTRIBUTE_NAME_VALUE_SEPARATOR = "=";
  
  private final TermAttribute termAttr;
  private final TokenStream input;
  private final ByteBuffer bbuf;
  private final boolean hash;
  /**
   * Should we consider only prefixed attributes?
   */
  private final boolean onlyprefixed;
  
  /**
   * Should we add the prefix when outputing values?
   */
  private final boolean addprefix;
  
  public AttributeTokenStream(TokenStream input) {
    this(input, true, true, false);
  }

  public AttributeTokenStream(TokenStream input, boolean hash) {
    this(input, hash, true, false);
  }

  public AttributeTokenStream(TokenStream input, boolean hash, boolean onlyprefixed, boolean addprefix) {
    this.input = input;
    // The TermAttribute we will return
    this.termAttr = addAttribute(TermAttribute.class);
    this.bbuf = ByteBuffer.allocate(8);
    this.bbuf.order(ByteOrder.BIG_ENDIAN);
    this.hash = hash;
    this.onlyprefixed = onlyprefixed;
    this.addprefix = addprefix;
  }

  @Override
  public boolean incrementToken() throws IOException {
    
    //
    // Only attributes that start with INDEXED_ATTRIBUTE_PREFIX are indexed
    //
    
    TermAttribute term = null;
    
    while(this.input.incrementToken()) {
      //
      // Retrieve next key=value 
      //

      term = input.getAttribute(TermAttribute.class);
      
      if (!onlyprefixed || term.term().startsWith(INDEXED_ATTRIBUTE_PREFIX)) {
        break;
      }
      
      term = null;
    }
    
    if (null == term) {
      return false;
    }
    
    //
    // Compute FNV1a64 of term. Trim INDEXED_ATTRIBUTE_PREFIX if onlyprefixed is true
    //
   
    if (hash) {
      byte[] termbytes = term.term().getBytes(Charsets.UTF_8);
      long fnv = CryptoUtil.FNV1a64(termbytes, onlyprefixed ? 1 : 0, termbytes.length - (onlyprefixed ? 1 : 0));
      
      this.bbuf.rewind();
      this.bbuf.putLong(fnv);
      
      // Output its b64 representation, striping the end '='
      this.termAttr.setTermBuffer(new String(Base64.encode(this.bbuf.array()), 0, 11, Charsets.UTF_8));      
    } else {
      if (!addprefix || term.term().startsWith(INDEXED_ATTRIBUTE_PREFIX)) {
        this.termAttr.setTermBuffer(QueryParser.escape(term.term()));
      } else {
        this.termAttr.setTermBuffer(QueryParser.escape((addprefix ? INDEXED_ATTRIBUTE_PREFIX : "") + term.term()));
      }
    }
    
    return true;
  }
}
