package com.geocoord.lucene;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.lucene.UUIDTokenStream.PayloadAttr;

/**
 * Class that holds Lucene Segment payload data.
 * 
 */
public class GeoDataSegmentCache {
  
  private static final Logger logger = LoggerFactory.getLogger(GeoDataSegmentCache.class);
  
  public static final class GeoData {
    long uuidMSB;
    long uuidLSB;
    long hhcode;
    int timestamp;
  }
  
  /**
   * Map of segment -> most significant bit of UUID
   * Segment key is segment dir + segment name, should be unique per host.
   */
  private static final Map<SegmentInfo,long[]> uuidMSB = new HashMap<SegmentInfo, long[]>();
  /**
   * Map of segment -> least significant bit of UUID
   */
  private static final Map<SegmentInfo,long[]> uuidLSB = new HashMap<SegmentInfo, long[]>();
  /**
   * Map of segment -> HHCode
   */
  private static final Map<SegmentInfo,long[]> hhcodes = new HashMap<SegmentInfo, long[]>();
  /**
   * Map of segment -> timestamp (in seconds since the epoch)
   */
  private static final Map<SegmentInfo,int[]> timestamps = new HashMap<SegmentInfo, int[]>();
  
  private static final Map<IndexReader,SegmentInfo[]> readerSegmentKeys = new HashMap<IndexReader, SegmentInfo[]>();
  private static final Map<IndexReader,int[]> readerSegmentStarts = new HashMap<IndexReader, int[]>();
  
  
  public static final void removeSegment(SegmentInfo key) {
    
    int ndocs = 0;
        
    if (uuidMSB.containsKey(key)) {
      ndocs = uuidMSB.get(key).length;
      uuidMSB.remove(key);
      uuidLSB.remove(key);
      hhcodes.remove(key);
      timestamps.remove(key);
    }
    
    logger.info("Removed segment " + key + " (" + ndocs + ")");
  }
  
  public static final void allocateSegment(SegmentInfo si, int size) {
    
    uuidMSB.put(si, new long[size]);
    uuidLSB.put(si, new long[size]);
    hhcodes.put(si, new long[size]);
    timestamps.put(si, new int[size]);
    
    logger.info("Allocated segment " + si + " (" + size + ")");
  }
  
  public static final long[] getUuidMSB(SegmentInfo key) {
    return uuidMSB.get(key);
  }

  public static final long[] getUuidLSB(SegmentInfo key) {
    return uuidLSB.get(key);
  }
  
  public static final long[] getHhcodes(SegmentInfo key) {
    return hhcodes.get(key);
  }

  public static final int[] getTimestamps(SegmentInfo key) {
    return timestamps.get(key);
  }
  
  /**
   * Populate the cache for 'key' from 'reader'
   * @param key
   * @param reader
   */
  public static final boolean loadCache(SegmentInfo si, IndexReader reader) {
    
    boolean success = false;
    
    long nano = System.nanoTime();
    
    //
    // Allocate space for this key
    //
    
    allocateSegment(si, reader.maxDoc());
    
    long[] uuidmsb = getUuidMSB(si);
    long[] uuidlsb = getUuidLSB(si);
    long[] hhcodes = getHhcodes(si);
    int[] timestamps = getTimestamps(si);
    
    //
    // Loop on term positions for UUID
    //
    
    TermPositions tp = null;
    
    try {
      tp = reader.termPositions(new Term(GeoCoordIndex.ID_FIELD, UUIDTokenStream.UUIDTermValue));
      
      ByteBuffer payload = null;
          
      while(tp.next()) {
        
        int docid = tp.doc();
        tp.nextPosition();
        
        //
        // Retrieve payload
        //
            
        if (tp.isPayloadAvailable()) {
          int len = tp.getPayloadLength();
          
          // Allocate the ByteBuffer if the payload size changed or
          // if it's the first allocation
          if (null == payload || payload.limit() != len) {
            payload = ByteBuffer.allocate(len);
          }
          
          tp.getPayload(payload.array(), 0);

          hhcodes[docid] = UUIDTokenStream.getPayloadAttribute(payload, PayloadAttr.HHCODE);
          uuidmsb[docid] = UUIDTokenStream.getPayloadAttribute(payload, PayloadAttr.UUID_MSB);
          uuidlsb[docid] = UUIDTokenStream.getPayloadAttribute(payload, PayloadAttr.UUID_LSB);
          timestamps[docid] = (int) (UUIDTokenStream.getPayloadAttribute(payload, PayloadAttr.TIMESTAMP) / 1000);
        }
      }
      
      success = true;
    } catch (IllegalStateException ise) {
      // Thrown when no terms index was loaded. Consider the load a success anyway
      logger.error("loadCache", ise);
      assert false;
      //success = true;
    } catch (IOException ioe) {
      success = false;
      removeSegment(si);      
    } finally {
      if (null != tp) {
        try { tp.close(); } catch (IOException ioe) {}
      }
    }
    
    nano = System.nanoTime() - nano;

    logger.info("Loaded " + reader.maxDoc() + " payloads in " + (nano / 1000000.0) + " ms => success = " + success);
    
    return success;
  }
  /*
  final static int readerIndex(IndexReader reader, int docid) {
    int lo = 0;                                      // search starts array
    int hi = numSubReaders - 1;                  // for first element less

    while (hi >= lo) {
      int mid = (lo + hi) >>> 1;
      int midValue = starts[mid];
      if (n < midValue)
        hi = mid - 1;
      else if (n > midValue)
        lo = mid + 1;
      else {                                      // found a match
        while (mid+1 < numSubReaders && starts[mid+1] == midValue) {
          mid++;                                  // scan to last match
        }
        return mid;
      }
    }
    return hi;
  }
*/
  
  /**
   * Record document starts per segment for a given reader.
   * 
   * @param reader
   * @param infos
   */
  public static final void addReaderDocStarts(IndexReader reader, SegmentReader[] sreaders) {
    
    //
    // Allocate a LinkedHashMap for the doc starts per segment.
    // This MUST be a LinkedHashMap so we can do a dichotomy on the key
    //
    
    readerSegmentKeys.put(reader, new SegmentInfo[sreaders.length]);
    readerSegmentStarts.put(reader, new int[sreaders.length]);
    
    int maxDoc = 0;
    
    for (int i = 0; i < sreaders.length; i++) {
      readerSegmentKeys.get(reader)[i] = sreaders[i].getPublicSegmentInfo();
      readerSegmentStarts.get(reader)[i] = maxDoc;
      maxDoc += sreaders[i].maxDoc();
    }
    
    logger.info("Added " + sreaders.length + " per segment infos for reader " + reader);

  }
  
  public static final void removeReaderDocStarts(IndexReader reader) {
    readerSegmentKeys.remove(reader);
    readerSegmentStarts.remove(reader);
    logger.info("Removed per segment infos for reader " + reader);
  }

  private static final int getSegmentIndex(IndexReader reader, int docid) {
    //
    // Do a dichotomy to find the correct segment
    //
    
    int size = readerSegmentStarts.get(reader).length;
    int[] starts = readerSegmentStarts.get(reader);
    
    int lo = 0;                         // search starts array
    int hi = size - 1;                  // for first element less

    int idx = -1;
    
    while (hi >= lo) {
      int mid = (lo + hi) >>> 1;
      int midValue = starts[mid];
      if (docid < midValue)
        hi = mid - 1;
      else if (docid > midValue)
        lo = mid + 1;
      else {                                      // found a match
        while (mid+1 < size && starts[mid+1] == midValue) {
          mid++;                                  // scan to last match
        }
        return mid;
      }
    }
    
    return hi;
  }
  
  public static final boolean getGeoData(IndexReader reader, int docid, GeoData gdata) {

    int idx = getSegmentIndex(reader, docid);
    
    //
    // Fill the GeoData
    //
    
    SegmentInfo segkey = readerSegmentKeys.get(reader)[idx];

    // Compute docid relative to the segment
    int segdocid = docid - readerSegmentStarts.get(reader)[idx];
                                                           
    // docid is too big
    if (segdocid > uuidMSB.get(segkey).length) {
      return false;
    }

    gdata.uuidMSB = uuidMSB.get(segkey)[segdocid];
    gdata.uuidLSB = uuidLSB.get(segkey)[segdocid];
    gdata.hhcode = hhcodes.get(segkey)[segdocid];
    gdata.timestamp = timestamps.get(segkey)[segdocid];
    
    return true;
  }

  public static String getSegmentKey(SegmentReader sr) {
    return sr.directory().toString() + sr.getSegmentName();
  }
  
  public static final boolean getSegmentGeoData(String segkey, int segdocid, GeoData gdata) {
    
    if (!uuidMSB.containsKey(segkey) || segdocid > uuidMSB.get(segkey).length) {
      return false;
    }
    
    gdata.uuidMSB = uuidMSB.get(segkey)[segdocid];
    gdata.uuidLSB = uuidLSB.get(segkey)[segdocid];
    gdata.hhcode = hhcodes.get(segkey)[segdocid];
    gdata.timestamp = timestamps.get(segkey)[segdocid];
    
    return true;
  }
  
  public static final long[] getSegmentUUIDMSB(String segkey) {
    return uuidMSB.get(segkey);
  }
  public static final long[] getSegmentUUIDLSB(String segkey) {
    return uuidLSB.get(segkey);
  }
  public static final long[] getSegmentHHCodes(String segkey) {
    return hhcodes.get(segkey);
  }
  public static final int[] getSegmentTimestamps(String segkey) {
    return timestamps.get(segkey);
  }

  public static final long getHHCode(IndexReader reader, int docid) {
    int idx = getSegmentIndex(reader, docid);

    //
    // Fill the GeoData
    //
    
    SegmentInfo segkey = readerSegmentKeys.get(reader)[idx];

    // Compute docid relative to the segment
    int segdocid = docid - readerSegmentStarts.get(reader)[idx];
                                                           
    // docid is too big
    
    long[] a = hhcodes.get(segkey);
    
    if (segdocid > a.length) {
      return 0;
    }

    return a[segdocid];
  }
  
  public static final int getTimestamp(IndexReader reader, int docid) {
    int idx = getSegmentIndex(reader, docid);

    //
    // Fill the GeoData
    //
    
    SegmentInfo segkey = readerSegmentKeys.get(reader)[idx];

    // Compute docid relative to the segment
    int segdocid = docid - readerSegmentStarts.get(reader)[idx];

    int[] a = timestamps.get(segkey);
    
    // docid is too big
    if (segdocid > a.length) {
      return 0;
    }

    return a[segdocid];    
  }
  
  /**
   * Dump statistics to stdout
   */
  public static void stats() {
    System.out.println("Cached data for " + hhcodes.size() + " segments.");
    
    for (SegmentInfo seg: hhcodes.keySet()) {
      System.out.println("  " + seg + " -> " + hhcodes.get(seg).length);
    }
    
    long total = 0;
    
    for (long[] a: hhcodes.values()) {
      total += a.length;
    }
        
    System.out.println("Total cached items " + total);
    
    System.out.println("Segment infos for " + readerSegmentKeys.size() + " segments.");
    
    for (IndexReader reader: readerSegmentKeys.keySet()) {
      System.out.println("   " + reader.toString() + " -> " + readerSegmentKeys.get(reader).length);
    }
  }
  
  public static boolean deleteByUUID(IndexWriter writer, long msb, long lsb) throws IOException {
    //
    // Find the SegmentInfo/docId of the point to delete
    //

    int docid = 0;
    SegmentInfo si = null;
    
    for (SegmentInfo info: uuidMSB.keySet()) {
      long[] msbs = uuidMSB.get(info);
      long[] lsbs = uuidLSB.get(info);
      if (null != msbs) {
        for (int i = 0; i < msbs.length; i++) {          
          if (msb == msbs[i] && lsb == lsbs[i]) {
            si = info;
            docid = i;
            break;
          }
        }
      }
      if (null != si) {
        break;
      }
    }
    
    //
    // If the point was not found, return false
    //
    
    if (null == si) {
      return false;
    }
    
    //
    // Attempt to delete the point
    //
    
    logger.info("About to delete doc #" + docid + " in segment " + si);
    
    SegmentReader sr = writer.getSegmentReaderFromReadersPool(si);
    
    if (null != sr) {
      sr.deleteDocument(docid);
    }
    
    writer.releaseSegmentReader(sr);

    logger.info("Deleted doc #" + docid + " in segment " + si);

    return true;
  }
}
