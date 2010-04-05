package com.geocoord.lucene;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.bouncycastle.util.encoders.Base64;

import com.geocoord.util.CryptoUtil;
import com.google.common.base.Charsets;

/**
 * Tokenizes reversed FQDNs by splitting them on '.' (dot) and
 * emitting one token per depth, from top level to full FQDN.
 * 
 * This allows to search on a whole hierarchy of names
 */
public class FQDNTokenStream extends TokenStream {

  private final TermAttribute termAttr;
  private final TokenStream input;
  private final ByteBuffer bbuf;
  
  /**
   * Tokenized fqdn
   */
  private String[] tokens;
  
  /**
   * Current depth in tokens 
   */
  private int tokenDepth;
  
  /**
   * StringBuilder to build the current layer name (at tokenDepth)
   */
  private StringBuilder sb = new StringBuilder();
  
  public FQDNTokenStream(TokenStream input) {
    this.input = input;
    // The TermAttribute we will return
    this.termAttr = addAttribute(TermAttribute.class);
    this.bbuf = ByteBuffer.allocate(8);
    this.bbuf.order(ByteOrder.BIG_ENDIAN);
    
    this.tokenDepth = 0;
  }

  @Override
  public boolean incrementToken() throws IOException {
    
    //
    // If we have no tokens yet or we have consumed them all, retrieve
    // next token from our input
    //

    if (null == tokens || tokenDepth >= tokens.length) {
      if (!this.input.incrementToken()) {
        return false;      
      }

      //
      // Retrieve next layer name
      //

      TermAttribute term = input.getAttribute(TermAttribute.class);

      // Split layer name on '.'
      
      tokens = term.term().split("\\.");
      tokenDepth = 0;
      sb.setLength(0);
    }

    if (sb.length() > 0) {
      sb.append(".");
    }
    
    sb.append(tokens[tokenDepth++]);
    
    this.termAttr.setTermBuffer(sb.toString());
    
    return true;
  }
}
