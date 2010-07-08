package com.geocoord.lucene;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;

/**
 * Analyzer that only accepts TAGS/ATTR fields.
 * 
 * To be used when parsing queries from the API which should not be
 * allowed to access other fields.
 * 
 */
public class TagAttrAnalyzer extends Analyzer {

  private static WhitespaceAnalyzer wsa = new WhitespaceAnalyzer();
  private static StandardAnalyzer sa = new StandardAnalyzer(Version.LUCENE_30);

  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    if (GeoCoordIndex.ATTR_FIELD.equals(fieldName)) {
      return new AttributeTokenStream(wsa.tokenStream(fieldName, reader), false);
    } else if (GeoCoordIndex.TAGS_FIELD.equals(fieldName)) {
      try {
        return new PorterStemFilter(new ASCIIFoldingFilter(sa.reusableTokenStream(fieldName, reader)));
      } catch (IOException ioe) {
        return null;
      }
    } else {
      throw new RuntimeException(new GeoCoordException(GeoCoordExceptionCode.SEARCH_INVALID_QUERY_FIELD));
    }
  }
}
