package com.geoxp.lucene;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.junit.Assert;
import org.junit.Test;


public class FQDNTokenStreamTestCase {

  @Test
  public void testExpanded() throws IOException {
    StringReader reader = new StringReader("com.geoxp.simple.test.layer.name");
    FQDNTokenStream lts = new FQDNTokenStream(new WhitespaceAnalyzer().tokenStream(GeoCoordIndex.LAYER_FIELD, reader));
    
    StringBuilder sb = new StringBuilder();
    
    while(lts.incrementToken()) {
      TermAttribute term = lts.getAttribute(TermAttribute.class);
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(term.term());
    }
    
    Assert.assertEquals("com.geoxp. com.geoxp.simple. com.geoxp.simple.test. com.geoxp.simple.test.layer. com.geoxp.simple.test.layer.name", sb.toString());
  }

  @Test
  public void testNotExpanded() throws IOException {
    StringReader reader = new StringReader(FQDNTokenStream.FQDN_NO_EXPAND_PREFIX + "com.geoxp.simple.test.layer.name");
    FQDNTokenStream lts = new FQDNTokenStream(new WhitespaceAnalyzer().tokenStream(GeoCoordIndex.LAYER_FIELD, reader));
    
    StringBuilder sb = new StringBuilder();
    
    while(lts.incrementToken()) {
      TermAttribute term = lts.getAttribute(TermAttribute.class);
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(term.term());
    }
    
    Assert.assertEquals("com.geoxp.simple.test.layer.name", sb.toString());
  }
}
