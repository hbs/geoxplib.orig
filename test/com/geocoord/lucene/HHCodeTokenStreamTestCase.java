package com.geocoord.lucene;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import junit.framework.TestCase;

public class HHCodeTokenStreamTestCase extends TestCase {
  
  public void testIncrementToken_SingleHHCode_MultiResolution() throws IOException {
    StringReader reader = new StringReader("0123456789abcdeF");
    HHCodeTokenStream hhcts = new HHCodeTokenStream(new WhitespaceAnalyzer().tokenStream(GeoCoordIndex.GEO_FIELD, reader));
    
    StringBuilder sb = new StringBuilder();
    
    while(hhcts.incrementToken()) {
      TermAttribute term = hhcts.getAttribute(TermAttribute.class);
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(term.term());
    }
    
    assertEquals("0123456789abcdef 0123456789abcde 0123456789abcd 0123456789abc 0123456789ab 0123456789a 0123456789 012345678 01234567 0123456 012345 01234 0123 012 01 0", sb.toString());
  }

  public void testIncrementToken_MultipleHHCode_MultiResolution() throws IOException {
    StringReader reader = new StringReader("0123456789abcdef FEDCBA9876543210");
    HHCodeTokenStream hhcts = new HHCodeTokenStream(new WhitespaceAnalyzer().tokenStream(GeoCoordIndex.GEO_FIELD, reader));
    
    StringBuilder sb = new StringBuilder();
    
    while(hhcts.incrementToken()) {
      TermAttribute term = hhcts.getAttribute(TermAttribute.class);
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(term.term());
    }
    
    assertEquals("0123456789abcdef 0123456789abcde 0123456789abcd 0123456789abc 0123456789ab 0123456789a 0123456789 012345678 01234567 0123456 012345 01234 0123 012 01 0 fedcba9876543210 fedcba987654321 fedcba98765432 fedcba9876543 fedcba987654 fedcba98765 fedcba9876 fedcba987 fedcba98 fedcba9 fedcba fedcb fedc fed fe f", sb.toString());
  }
  
  public void testIncrementToken_SingleHHCode_MonoResolution() throws IOException {
    StringReader reader = new StringReader("#0123456789abcdeF");
    HHCodeTokenStream hhcts = new HHCodeTokenStream(new WhitespaceAnalyzer().tokenStream(GeoCoordIndex.GEO_FIELD, reader));
    
    StringBuilder sb = new StringBuilder();
    
    while(hhcts.incrementToken()) {
      TermAttribute term = hhcts.getAttribute(TermAttribute.class);
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(term.term());
    }
    
    System.out.println(sb.toString());
    assertEquals("0123456789abcdef", sb.toString());
  }

  public void testIncrementToken_MultipleHHCode_MonoResolution() throws IOException {
    StringReader reader = new StringReader("#0123456789abcdef #FEDCBA9876543210");
    HHCodeTokenStream hhcts = new HHCodeTokenStream(new WhitespaceAnalyzer().tokenStream(GeoCoordIndex.GEO_FIELD, reader));
    
    StringBuilder sb = new StringBuilder();
    
    while(hhcts.incrementToken()) {
      TermAttribute term = hhcts.getAttribute(TermAttribute.class);
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(term.term());
    }
    
    System.out.println(sb.toString());
    assertEquals("0123456789abcdef fedcba9876543210", sb.toString());
  }

}
