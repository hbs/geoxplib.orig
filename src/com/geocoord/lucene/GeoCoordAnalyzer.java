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

public class GeoCoordAnalyzer extends Analyzer {
  
  private static WhitespaceAnalyzer wsa = new WhitespaceAnalyzer();
  private static StandardAnalyzer sa = new StandardAnalyzer(Version.LUCENE_30);
  
  @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    //
    // Handle all known fields
    //
    
    if (GeoCoordIndex.ID_FIELD.equals(fieldName)
        || GeoCoordIndex.LAYER_FIELD.equals(fieldName)
        || GeoCoordIndex.USER_FIELD.equals(fieldName)
        || GeoCoordIndex.ATTR_FIELD.equals(fieldName)
        || GeoCoordIndex.TYPE_FIELD.equals(fieldName)
        || GeoCoordIndex.TSHIGH_FIELD.equals(fieldName)
        || GeoCoordIndex.TSMID_FIELD.equals(fieldName)
        || GeoCoordIndex.TSLOW_FIELD.equals(fieldName)) {
      try {
        return wsa.reusableTokenStream(fieldName, reader);
      } catch (IOException ioe) {
        return null;
      }
    } else if (GeoCoordIndex.TAGS_FIELD.equals(fieldName)) {
      try {
        return new PorterStemFilter(new ASCIIFoldingFilter(sa.reusableTokenStream(fieldName, reader)));
      } catch (IOException ioe) {
        return null;
      }
    } else if (GeoCoordIndex.GEO_FIELD.equals(fieldName)) {
      return new HHCodeTokenStream(wsa.tokenStream(fieldName, reader));
    } else {
      return null;
    }
  }
}
