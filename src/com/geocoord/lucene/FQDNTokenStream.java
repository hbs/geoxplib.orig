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

  public static final String FQDN_NO_EXPAND_PREFIX = "#";
  
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

  /**
  * Minimum number of tokens (>= 1).
  * With 1, com.foo will be split into com and com.foo
  * With 2, com.foo will only emit com.foo
  */
  private final int mintokens = 2;
  
  /**
   * 
   * @param input
   */
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

      // If term should not be split, simply remove the prefix.
      if (term.term().startsWith(FQDN_NO_EXPAND_PREFIX)) {
        tokens = null;
        sb.setLength(0);
        sb.append(term.term());
        sb.deleteCharAt(0);
      } else {
        
        // Split layer name on '.'
        
        tokens = term.term().split("\\.");
        tokenDepth = 0;
        sb.setLength(0);
        
        int i = mintokens;
        
        while (tokenDepth < tokens.length && tokenDepth < mintokens - 1) {
          sb.append(tokens[tokenDepth++]);
          
          if (tokenDepth < tokens.length) {
            sb.append(".");
          }
        }
        
      }
    }

    if (null != tokens && tokenDepth < tokens.length) {
      sb.append(tokens[tokenDepth++]);      
      
      if (tokenDepth < tokens.length) {
        sb.append(".");
      }
    }
    
    this.termAttr.setTermBuffer(sb.toString());
    
    return true;
  }
}
