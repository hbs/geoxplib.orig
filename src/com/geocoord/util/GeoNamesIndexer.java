package com.geocoord.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.geo.HHCodeHelper;
import com.geocoord.lucene.GeoCoordAnalyzer;
import com.geocoord.lucene.GeoCoordIndex;
import com.geocoord.lucene.UUIDTokenStream;

public class GeoNamesIndexer {

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    
    IndexWriter writer = new IndexWriter(FSDirectory.open(new File(args[0])), new GeoCoordAnalyzer(24), true, MaxFieldLength.UNLIMITED);
    
    int count = 0;
    
    UUIDTokenStream uuidTokenStream = new UUIDTokenStream();
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        break;
      }

      if (line.startsWith("RC")) {
        continue;
      }
      
      String[] tokens = line.split("\\t");

      long hhcode = HHCodeHelper.getHHCodeValue(Double.valueOf(tokens[3]), Double.valueOf(tokens[4]));

      Document doc = new Document();
      
      //
      // Reset UUIDTokenStream
      //
      
      uuidTokenStream.reset(UUID.randomUUID(),hhcode,System.currentTimeMillis());
      Field field = new Field(GeoCoordIndex.ID_FIELD, uuidTokenStream);      
      doc.add(field);
                        
      StringBuilder sb = new StringBuilder();
      
      sb.append(HHCodeHelper.toString(hhcode));
      
      field = new Field(GeoCoordIndex.GEO_FIELD, sb.toString(), Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);      
      doc.add(field);

      if (!"".equals(tokens[10])) {
        field = new Field(GeoCoordIndex.ATTR_FIELD, "dsg:" + tokens[10], Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
        doc.add(field);
      }

      field = new Field(GeoCoordIndex.TAGS_FIELD, tokens[23], Store.NO, Index.ANALYZED_NO_NORMS, TermVector.NO);
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
