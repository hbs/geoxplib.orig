package com.geoxp.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geoxp.lucene.AttributeTokenStream;
import com.geoxp.lucene.GeoCoordIndex;
import com.geoxp.lucene.TagAttrAnalyzer;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TagAttrAnalyzerTestCase extends TestCase {
  
  public void testForbiddenFields() throws Exception {
    TagAttrAnalyzer gca = new TagAttrAnalyzer();

    String query = "foo:(bar) AND tags:(abc OR def) OR attr:(\\" + AttributeTokenStream.INDEXED_ATTRIBUTE_PREFIX + "com.geoxp.test)";
    
    QueryParser parser = new QueryParser(Version.LUCENE_30, GeoCoordIndex.TAGS_FIELD, gca);
    
    Throwable t = null;
    
    try {
      Query q = parser.parse(query);
      System.out.println(q);
    } catch (RuntimeException re) {
      if (re.getCause() instanceof GeoCoordException) {
        t = re.getCause();
      }
    }
    
    Assert.assertTrue(null != t);
    Assert.assertEquals(GeoCoordExceptionCode.SEARCH_INVALID_QUERY_FIELD, ((GeoCoordException) t).getCode());
  }
  
  public void testTokenStream_TAGSField() throws IOException {
    TagAttrAnalyzer gca = new TagAttrAnalyzer();
    StandardAnalyzer sa = new StandardAnalyzer(Version.LUCENE_30);
    
    String WSATestString = "the quick brown fox jumped over the lazy dog jumping étoile des neiges";

    StringReader reader1 = new StringReader(WSATestString);
    StringReader reader2 = new StringReader(WSATestString);
    
    TokenStream ts1 = gca.tokenStream(GeoCoordIndex.TAGS_FIELD, reader1);
    // We swap ASCIIFoldingFilter and PorterStemFilter to detect weirdnesses
    TokenStream ts2 = new ASCIIFoldingFilter(new PorterStemFilter(sa.tokenStream(GeoCoordIndex.TAGS_FIELD, reader2)));
    
    while(ts1.incrementToken()) {
      assertTrue(ts2.incrementToken());
      // Compare term length
      assertEquals(ts1.getAttribute(TermAttribute.class).termLength(), ts2.getAttribute(TermAttribute.class).termLength());
      assertEquals(ts1.getAttribute(TermAttribute.class).termLength(), ts2.getAttribute(TermAttribute.class).termLength());
      // Compare term value
      assertEquals(ts1.getAttribute(TermAttribute.class).term(), ts2.getAttribute(TermAttribute.class).term());
      assertEquals(ts1.getAttribute(TermAttribute.class).term(), ts2.getAttribute(TermAttribute.class).term());
    }
    
    assertFalse(ts2.incrementToken());          
  }

  public void testTokenStream_ATTRField() throws IOException {
    TagAttrAnalyzer gca = new TagAttrAnalyzer();
    WhitespaceAnalyzer wsa = new WhitespaceAnalyzer();
    
    String WSATestString = "0 1 2 3";
    String AttrTestString = "~0 ~1 ~2 ~3";
        
    StringReader reader1 = new StringReader(AttrTestString);
    StringReader reader2 = new StringReader(WSATestString);
    
    TokenStream ts1 = gca.tokenStream(GeoCoordIndex.ATTR_FIELD, reader1);
    // Use same constructor as in TagAttrAnalyzer
    TokenStream ts2 = new AttributeTokenStream(wsa.tokenStream(GeoCoordIndex.ATTR_FIELD, reader2), false, false, true);
    
    while(ts1.incrementToken()) {
      assertTrue(ts2.incrementToken());
      // Compare term length
      assertEquals(ts1.getAttribute(TermAttribute.class).termLength(), ts2.getAttribute(TermAttribute.class).termLength());
      assertEquals(ts1.getAttribute(TermAttribute.class).termLength(), ts2.getAttribute(TermAttribute.class).termLength());
      // Compare term value
      assertEquals(ts1.getAttribute(TermAttribute.class).term(), ts2.getAttribute(TermAttribute.class).term());
      assertEquals(ts1.getAttribute(TermAttribute.class).term(), ts2.getAttribute(TermAttribute.class).term());
    }
    
    assertFalse(ts2.incrementToken());                  
  }
  
  public void testTokenStream_TagAttr() throws Exception {
    String q = "attr:(((type=parking) OR (type=camping) OR (type=wc) OR (type=euro) OR (type=food) OR (type=drink) OR (type=redcross)))";

    QueryParser qp = new QueryParser(Version.LUCENE_30, GeoCoordIndex.TAGS_FIELD, new TagAttrAnalyzer());
    
    Query query = qp.parse(q);
    
    System.out.println(query);
  }
}
