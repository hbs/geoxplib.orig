package com.geocoord.lucene;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * Outputs multiple tokens per timestamp.
 * 
 */
public class TimestampTokenStream extends TokenStream {
  
  private TermAttribute termAttr = null;

  private SimpleDateFormat sdf = null;
  
  private int variant = 0;
  
  /**
   * Buffer holding the next token
   */
  private StringBuffer sb = new StringBuffer(16);

  private TokenStream input = null;
 
  public TimestampTokenStream(TokenStream input) {
    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    
    this.input = input;
    // The TermAttribute we will return
    this.termAttr = addAttribute(TermAttribute.class);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    
    //
    // Do we have any more variants to emit?
    //
    
    if (variant > 0 && variant < 9) {
      switch (variant) {
        case 1: // :MM
          this.termAttr.setTermBuffer(sb.substring(13, 16));
          break;
        case 2: // HH:
          this.termAttr.setTermBuffer(sb.substring(11, 14));
          break;
        case 3: // HH:MM
          this.termAttr.setTermBuffer(sb.substring(11, 16));
          break;
        case 4: // -DD
          this.termAttr.setTermBuffer(sb.substring(7, 10));
          break;
        case 5: // -MM-
          this.termAttr.setTermBuffer(sb.substring(4, 8));
          break;
        case 6: // MM-DD
          this.termAttr.setTermBuffer(sb.substring(5,10));
          break;
        case 7: // YYYY
          this.termAttr.setTermBuffer(sb.substring(0, 4));
          break;
        case 8: // YYYY-MM
          this.termAttr.setTermBuffer(sb.substring(0, 7));
          break;
        case 9: // YYYY-MM-DD
          this.termAttr.setTermBuffer(sb.substring(0, 10));
          break;
      }
      variant--;
      return true;
    }

    if (!input.incrementToken()) {
      return false;
    }
    
    //
    // Clear current timestamp
    //
    
    sb.setLength(0);
    
    //
    // Retrieve next timestamp
    //

    TermAttribute term = input.getAttribute(TermAttribute.class);

    long ts = Long.parseLong(term.term());
          
    sb.append(sdf.format(new Date(ts)));

    // Emit YYYY-MM-DD
    this.termAttr.setTermBuffer(sb.substring(0,10));
    variant = 8;
    
    return true;
  }


}
