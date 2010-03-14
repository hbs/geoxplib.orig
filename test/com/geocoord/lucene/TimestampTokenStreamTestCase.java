package com.geocoord.lucene;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import junit.framework.TestCase;

public class TimestampTokenStreamTestCase extends TestCase {
  
  public void testSplit_19700101T0000() throws IOException {
    StringReader reader = new StringReader("0");
    TimestampTokenStream tts = new TimestampTokenStream(new WhitespaceAnalyzer().tokenStream(GeoCoordIndex.TS_FIELD, reader));
    
    StringBuilder sb = new StringBuilder();
    
    while(tts.incrementToken()) {
      TermAttribute term = tts.getAttribute(TermAttribute.class);
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(term.term());
    }
    
    assertEquals("1970-01-01 1970-01 1970 01-01 -01- -01 00:00 00: :00", sb.toString());
  }

  public void testSplit_19700101T0001() throws IOException {
    StringReader reader = new StringReader("60000");
    TimestampTokenStream tts = new TimestampTokenStream(new WhitespaceAnalyzer().tokenStream(GeoCoordIndex.TS_FIELD, reader));
    
    StringBuilder sb = new StringBuilder();
    
    while(tts.incrementToken()) {
      TermAttribute term = tts.getAttribute(TermAttribute.class);
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(term.term());
    }
    
    assertEquals("1970-01-01 1970-01 1970 01-01 -01- -01 00:01 00: :01", sb.toString());
  }

  public void testSplit_19700102T2359() throws IOException {
    StringReader reader = new StringReader("" + (86400000L + 23*3600000L + 59*60000L + 59999L));
    TimestampTokenStream tts = new TimestampTokenStream(new WhitespaceAnalyzer().tokenStream(GeoCoordIndex.TS_FIELD, reader));
    
    StringBuilder sb = new StringBuilder();
    
    while(tts.incrementToken()) {
      TermAttribute term = tts.getAttribute(TermAttribute.class);
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(term.term());
    }
    
    assertEquals("1970-01-02 1970-01 1970 01-02 -01- -02 23:59 23: :59", sb.toString());
  }

}
