package com.geocoord.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.FSDirectory;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.thrift.data.Constants;

public class GeoNamesIndexer {

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    
    IndexWriter writer = new IndexWriter(FSDirectory.open(new File(args[0])), new WhitespaceAnalyzer(), true, MaxFieldLength.UNLIMITED);
    
    int count = 0;
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        break;
      }

      if (line.startsWith("RC")) {
        continue;
      }
      
      String[] tokens = line.split("\\t");
    
      Document doc = new Document();
      
      Field field = new Field(Constants.LUCENE_ID_FIELD, tokens[7], Store.YES, Index.NOT_ANALYZED_NO_NORMS);      
      doc.add(field);
      
      long hhcode = HHCodeHelper.getHHCodeValue(Double.valueOf(tokens[3]), Double.valueOf(tokens[4]));
                  
      StringBuilder sb = new StringBuilder();
      
      sb.append(HHCodeHelper.toIndexableString(hhcode));
      
      field = new Field(Constants.LUCENE_HHCODE_FIELD, sb.substring(0,16), Store.YES, Index.NOT_ANALYZED_NO_NORMS, TermVector.NO);      
      doc.add(field);

      field = new Field(Constants.LUCENE_CELLS_FIELD, sb.substring(17), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.YES);      
      doc.add(field);

      writer.addDocument(doc);

      count++;
      if (count % 10000 == 0) {
        writer.commit();
        System.out.print("*");
      }
    }
    
    writer.close();
  }
}
