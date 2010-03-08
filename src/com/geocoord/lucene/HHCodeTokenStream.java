package com.geocoord.lucene;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * Outputs multiple tokens per HHCode (one for each possible resolution given the hhcode size).
 * 
 * If a HHCode is prefixed with '#', only emit this HHCode. This is used most notably by
 * areas which were pre processed.
 */
public class HHCodeTokenStream extends TokenStream {
  
  private TermAttribute termAttr = null;

  /**
   * Flag indicating whether or not to generate multiple resolution
   * HHCodes from a single one
   */
  private boolean multiresolution = false;
  
  /**
   * Buffer holding the next HHCode to return as a Term
   */
  private StringBuffer sb = new StringBuffer(16);

  private TokenStream input = null;
 
  public HHCodeTokenStream(TokenStream input) {
    this.input = input;
    // The TermAttribute we will return
    this.termAttr = addAttribute(TermAttribute.class);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    
    //
    // If the current HHCode is to be emitted at multiple
    // resolutions, decrease resolution until done
    //
    
    if (multiresolution && sb.length() > 0) {
      this.termAttr.setTermBuffer(sb.toString());
      sb.setLength(sb.length() - 1);
      return true;
    }
    
    if (!input.incrementToken()) {
      return false;
    }
    
    //
    // Clear current HHCode
    //
    
    sb.setLength(0);
    
    //
    // Retrieve next HHCode
    //

    TermAttribute term = input.getAttribute(TermAttribute.class);
    
    char[] chars = term.termBuffer();
    
    int idx = 0;
    
    if (chars[0] == '#') {
      multiresolution = false;
      idx++;
    } else {
      multiresolution = true;
    }
    
    //
    // Convert it to lower case.
    // INFO(hbs): no attempt is made to validate the HHCode.
    //
    
    while(idx < term.termLength()) {
      sb.append(Character.toLowerCase(chars[idx]));
      idx++;
    }

    this.termAttr.setTermBuffer(sb.toString());
    sb.setLength(sb.length() - 1);
    
    return true;
  }


}
