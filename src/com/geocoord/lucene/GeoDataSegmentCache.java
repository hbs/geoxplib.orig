package com.geocoord.lucene;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.bouncycastle.util.encoders.Hex;
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
  
  /**
   * List of segment infos, ordered by increasing size of segments.
   * This is to speed up deleteByUUID by checking segments in increasing order
   */
  private static List<SegmentInfo> segmentInfos = new ArrayList<SegmentInfo>();
  
  /**
   * Map of segment infos to array of docids, sorted by increasing UUIDs. This is
   * used to find the docid from the UUID when deleting a doc. This allows to find
   * the docid in logarithmic time using a binary search.
   */
  private static final Map<SegmentInfo,int[]> docids = new HashMap<SegmentInfo, int[]>();
  
  /**
   * Map of segment info to number of deleted docs when the segment is intially loaded.
   */
  private static final Map<SegmentInfo,Integer> deleteddocs = new HashMap<SegmentInfo, Integer>();
  
  /**
   * Value used to mark docids that are deleted when the segment is loaded.
   */
  private static final int DELETED_DOC_MARKER = -2;
  
  private static final Map<SegmentInfo,AtomicInteger> segmentInfoReferences = new HashMap<SegmentInfo, AtomicInteger>();
  
  private static final Map<IndexReader,SegmentInfo[]> readerSegmentKeys = new HashMap<IndexReader, SegmentInfo[]>();
  private static final Map<IndexReader,int[]> readerSegmentStarts = new HashMap<IndexReader, int[]>();
  
  
  public static synchronized final void removeSegmentInfoReference(SegmentInfo key) {

    if (!segmentInfoReferences.containsKey(key)) {
      logger.error("Attempt to remove a reference to an unknown SegmentInfo " + key);
      return;
    }

    // Decrement number of references
    int refs = segmentInfoReferences.get(key).decrementAndGet();
    
    if (refs != 0) {
      logger.info("Reference count to segment " + key + ": " + refs);
      return;
    }
    
    int ndocs = 0;
        
    if (uuidMSB.containsKey(key)) {
      ndocs = uuidMSB.get(key).length;
      uuidMSB.remove(key);
      uuidLSB.remove(key);
      hhcodes.remove(key);
      timestamps.remove(key);
      docids.remove(key);
      deleteddocs.remove(key);
      segmentInfos.remove(key);
    }
    
    logger.info("Removed all references to segment " + key + " (" + ndocs + ")");
  }
  
  public static synchronized final void allocateSegment(SegmentInfo si, int size) {
    
    uuidMSB.put(si, new long[size]);
    uuidLSB.put(si, new long[size]);
    hhcodes.put(si, new long[size]);
    timestamps.put(si, new int[size]);
    segmentInfos.add(si);
    
    if (!segmentInfoReferences.containsKey(si)) {
      segmentInfoReferences.put(si, new AtomicInteger());
    }
    
    int refs = segmentInfoReferences.get(si).incrementAndGet();
    
    if (1 != refs) {
      logger.warn("Weird.... we're allocating a segment (" + si + ") which already has references...");
    }

    //
    // Sort segmentInfos by doc count.
    //
    
    Collections.sort(segmentInfos, new Comparator<SegmentInfo>() {
      @Override
      public int compare(SegmentInfo o1, SegmentInfo o2) {
        return o1.docCount-o2.docCount;
      }
    });
    
    //
    // Allocate space for docids.
    //
    
    docids.put(si, new int[size]);
    
    // Fill docids array with -1 as a marker of deleted docs
    Arrays.fill(docids.get(si), DELETED_DOC_MARKER);
    
    logger.info("Allocated segment " + si + " (" + size + ")");
  }
  
  /**
   * Reflect a merge in the segment cache.
   * 
   * @param merged Merged segments
   * @param created Newly created segment
   */
  public static void commitMerge(IndexWriter writer, SegmentInfos merged, SegmentInfo created) throws IOException {
    //
    // Remove merged segments
    //
    
    logger.info("Committing merge ...");

    long doccount = 0;
    long deleted = 0;
    
    for (SegmentInfo info: merged) {
      removeSegmentInfoReference(info);
      doccount += info.docCount;
      deleted += info.getDelCount();
    }

    //
    // Add created segment. Retrieving a SegmentReader will trigger the loading of the cache
    //    

    SegmentReader reader = writer.getSegmentReaderFromReadersPool(created);
    writer.releaseSegmentReader(reader);
    
    assert (doccount - deleted) == created.docCount;
    
    logger.info("... merge committed");
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

    logger.info("About to load segment (" + si + ") of " + reader.maxDoc() + " docs (" + reader.numDeletedDocs() + " deleted)");

    boolean success = false;
    
    long nano = System.nanoTime();
    
    //
    // Allocate space for this key
    //
    
    allocateSegment(si, reader.maxDoc());

    final long[] uuidmsb = getUuidMSB(si);
    final long[] uuidlsb = getUuidLSB(si);
    long[] hhcodes = getHhcodes(si);
    int[] timestamps = getTimestamps(si);
    int[] sortedDocids = docids.get(si);
    
    //
    // Loop on term positions for UUID
    //
    
    TermPositions tp = null;
    
    int sanitized = 0;

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
          sortedDocids[docid] = docid;
        }
      }

      //
      // We move deleted docs to the end of the array, otherwise the sort fails
      //

      int firstdeleted = 0;

      if (si.getDelCount() > 0) {
        int i = 0;
        
        while (i < sortedDocids.length) {
          if (DELETED_DOC_MARKER != sortedDocids[i]) {
            int tmp = sortedDocids[firstdeleted];
            sortedDocids[firstdeleted] = sortedDocids[i];
            sortedDocids[i] = tmp;
            firstdeleted++;
          }
          i++;
        }
      } else {
        firstdeleted = sortedDocids.length;
      }

      deleteddocs.put(si, sortedDocids.length - firstdeleted);

      //
      // Now sort docids by UUID, sort only non deleted docs, otherwise sorting takes forever as UUID is
      // not set and all values are the same (which should NEVER happen)
      //

      QuickSorter.quicksort(sortedDocids, 0, firstdeleted - 1, new Comparator<Integer>() {        
        @Override
        public int compare(Integer docid1, Integer docid2) {
          
          long a = uuidmsb[docid1];
          long b = uuidmsb[docid2];
          
          if (a == b) { // MSBs are equal, compare LSBs.
            a = uuidlsb[docid1];
            b = uuidlsb[docid2];
          }
          
          int r = 0;
          
          if (a == b) {
            r = 0;
          } else if (((a & b) & 0x8000000000000000L) != 0) { // bit 63 is set in both
            r = Long.signum((a & 0x7fffffffffffffffL) - (b & 0x7fffffffffffffffL));
          } else if (a < 0) { // <0 therefore bigger (we consider unsigned longs...)
            r = 1;
          } else if (b < 0) { 
            r = -1;
          } else { // Both >= 0
            r = Long.signum(a - b);
          }
          
          //System.out.printf("%016x%016x   %016x%016x  ", uuidmsb[docid1], uuidlsb[docid1], uuidmsb[docid2], uuidlsb[docid2]);          
          //System.out.println(r);
          
          return r;
        }
      });
      
      //
      // Ok, we're almost done....
      //
      // We need to sanitize the segment data now, because if an atom was indexed several (N) times between two index
      // commits/merges, we have not been able to remove the N-1 previous values as the data was in memory and not
      // yet available (we can't delete non committed docs!).
      // Therefore we will scan the segment and remove all but one of each set of docs with the same UUID.
      // To remove it we will simply set its UUID msb/lsb to 0L/0L as this is a value that has a very tiny probability
      // of appearing...
      //
      // At the end of this stage, the segment will contain a single doc for a given UUID. This is sort of an
      // eventual consistency semantic, the more we merge, the more we'll become consistent.
      // 
      
      long lastmsb = 0L;
      long lastlsb = 0L;
            
      boolean first = true;
      
      for (int i = 0; i < firstdeleted; i++) {
        if (first) {
          lastmsb = uuidmsb[sortedDocids[i]];
          lastlsb = uuidlsb[sortedDocids[i]];
          first = false;
        } else {
          // this doc has the same UUID as the previous one, remove the previous one
          // as this doc has a greater docid (set its UUID to 0/0, DON'T SET sortedDocids to DELETED_DOC_MARKER,
          // because we would then need to sort the segment again.)
          if (uuidmsb[sortedDocids[i]] == lastmsb && uuidlsb[sortedDocids[i]] == lastlsb) {
            uuidmsb[sortedDocids[i-1]] = 0L;
            uuidlsb[sortedDocids[i-1]] = 0L;
            reader.deleteDocument(sortedDocids[i - 1]);
            sanitized++;
          } else {
            // Update MSB/LSB
            lastmsb = uuidmsb[sortedDocids[i]];
            lastlsb = uuidlsb[sortedDocids[i]];            
          }
        }
      }      
     
      // FIXME(hbs): we need to sanitize across segments too, keeping only the highest docid in the youngest segment.3
      success = true;
    } catch (IllegalStateException ise) {
      // Thrown when no terms index was loaded. Consider the load a success anyway
      logger.error("loadCache", ise);
      assert false;
      //success = true;
    } catch (IOException ioe) {
      success = false;
      removeSegmentInfoReference(si);      
    } finally {
      if (null != tp) {
        try { tp.close(); } catch (IOException ioe) {}
      }
    }
    
    nano = System.nanoTime() - nano;

    logger.info("Loaded " + reader.numDocs() + " payloads (sanitized " + sanitized + "/" + reader.maxDoc() + ") in " + (nano / 1000000.0) + " ms from " + reader + " => success = " + success);
    
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

    SegmentInfo segkey = null;
    int segdocid = docid;

    //
    // Sometimes (when in the middle of a merge) we're called with a SegmentReader
    //
    
    if (reader instanceof SegmentReader) {
      segkey = ((SegmentReader) reader).getPublicSegmentInfo();  
    } else {
      int idx = getSegmentIndex(reader, docid);
    
      segkey = readerSegmentKeys.get(reader)[idx];
    
      // Compute docid relative to the segment
      segdocid = docid - readerSegmentStarts.get(reader)[idx];
    }
    
    //
    // If no data is known for the given segment, return false.
    // This can happen in the middle of a merge when a segment with deletions
    // has been removed.
    //
    
    if (!uuidMSB.containsKey(segkey)) {
      return false;
    }
    
    // docid is too big
    if (segdocid > uuidMSB.get(segkey).length) {
      return false;
    }

    //
    // Fill the GeoData
    //
    
    gdata.uuidMSB = uuidMSB.get(segkey)[segdocid];
    gdata.uuidLSB = uuidLSB.get(segkey)[segdocid];
    gdata.hhcode = hhcodes.get(segkey)[segdocid];
    gdata.timestamp = timestamps.get(segkey)[segdocid];
    
    return true;
  }

  public static SegmentInfo getSegmentKey(SegmentReader sr) {
    return sr.getPublicSegmentInfo();
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
  public static void stats() throws IOException {
    System.out.println("Cached data for " + hhcodes.size() + " segments.");
    
    long deleted = 0L;
    
    for (SegmentInfo seg: hhcodes.keySet()) {
      System.out.println("  " + seg + " -> " + hhcodes.get(seg).length);
      deleted += seg.getDelCount();
    }
    
    long total = 0;
    
    for (long[] a: hhcodes.values()) {
      total += a.length;
    }
        
    System.out.println("Total cached  items " + total + " (" + deleted + " deleted)");
    
    System.out.println("Segment infos for " + readerSegmentKeys.size() + " readers.");
    
    for (IndexReader reader: readerSegmentKeys.keySet()) {
      System.out.println("   " + reader.toString() + " -> " + readerSegmentKeys.get(reader).length);
    }
  }
  
  public static boolean deleteByUUID(IndexWriter writer, final long msb, final long lsb) throws IOException {

    // Wait for possible pending merges
    // This is not an absolute guarantee that we will not hit a dead segment, but it should be
    // pretty close

    writer.waitForMerges();

    //
    // Find the SegmentInfo/docId of the point to delete
    //

    int docid = 0;
    SegmentInfo si = null;
    
    boolean diddeletes = false;
    
    //for (SegmentInfo info: uuidMSB.keySet()) {
    for (SegmentInfo info: segmentInfos) {

      // If the segmentInfo is no longer live, skip it.
      // Normally between the waitForMerges and now, no segment
      // should disappear. Unless we have really fast merges!
      if (!writer.infoIsLive(info)) {
        logger.info("Skipping dead segment info " + info);
        continue;
      }
      
      final long[] msbs = uuidMSB.get(info);
      final long[] lsbs = uuidLSB.get(info);
      
      if (null != msbs) {
        
        //
        // Do a binary search
        //
        
        int index = BinarySearcher.binarySearch(docids.get(info), deleteddocs.get(info), new Comparator<Integer>() {
          @Override
          public int compare(Integer docid1, Integer docid2) {
            long a = (-1 == docid1 ? msb : msbs[docid1]);
            long b = (-1 == docid2 ? msb : msbs[docid2]);
            
            if (a == b) { // MSBs are equal, compare LSBs.
              a = (-1 == docid1 ? lsb : lsbs[docid1]);
              b = (-1 == docid2 ? lsb : lsbs[docid2]);
            }
            
            int r = 0;
            
            if (a == b) {
              r = 0;
            } else if (((a & b) & 0x8000000000000000L) != 0) { // bit 63 is set in both
              r = Long.signum((a & 0x7fffffffffffffffL) - (b & 0x7fffffffffffffffL));
            } else if (a < 0) { // <0 therefore bigger (we consider unsigned longs...)
              r = 1;
            } else if (b < 0) { 
              r = -1;
            } else { // Both >= 0
              r = Long.signum(a - b);
            }

            return r;
          }
        });
        
        if (index >= 0) {
          //
          // Clear UUID MSB/LSB so if we later add the point again and it ends up in a new segment,
          // we don't attempt to delete the already deleted version...
          //
          msbs[index] = 0;
          lsbs[index] = 0;
          docid = docids.get(info)[index];
          si = info;          
        }
      }
      
      // INFO(hbs):  we scan ALL segments and delete all docs with the given UUID,
      //             this speeds up the eventual consistency of the index, as a doc with the same UUID being
      //             added several times per second will lead to several copies in the index.
      //             This induces a longer time to delete points, but this is for index's sanity so I guess this is good.
      if (null != si) {
        SegmentReader sr = writer.getSegmentReaderFromReadersPool(si);
        
        if (null != sr) {
          sr.deleteDocument(docid);
          writer.releaseSegmentReader(sr);
        } else {
          logger.info("Null SegmentReader when attempting to delete " + msb + ":" + lsb + " (docid=" + docid + ")");
        }
        si = null;
        diddeletes = true;
      }
    }

    //
    // If the point was not found, return false
    //
    
    if (!diddeletes) {
      return false;
    }
    
    //
    // Attempt to delete the point
    //
    
    //logger.info("About to delete uuid=" + msb + ":" + lsb + "   doc #" + docid + " in segment " + si);
    
    /*
    SegmentReader sr = writer.getSegmentReaderFromReadersPool(si);
    
    if (null != sr) {
      sr.deleteDocument(docid);
      writer.releaseSegmentReader(sr);
    } else {
      logger.info("Null SegmentReader when attempting to delete " + msb + ":" + lsb + " (docid=" + docid + ")");
    }
    */
    
    //logger.info("Deleted doc #" + docid + " in segment " + si);

    return true;
  }
  
  /**
   * Implementation of QuickSort
   * adapted from @see http://www.geekpedia.com/tutorial290_Quicksort-in-Java.html
   */
  public static class QuickSorter {
    
    public static void sort(int[] array, Comparator<Integer> c) {
      quicksort(array, 0, array.length-1, c);
    }
  
    private static void quicksort(int[] array, int lo, int hi, Comparator<Integer> c) {
      //System.out.println("LO=" + lo + " HI=" + hi);
      if (hi > lo) {
        int partitionPivotIndex = (int)(Math.random()*(hi-lo) + lo); // (lo + hi) / 2; //
        //System.out.println("LO=" + lo + " HI=" + hi + " PIVOT=" + partitionPivotIndex);
        int newPivotIndex = partition(array, lo, hi, partitionPivotIndex, c);
        quicksort(array, lo, newPivotIndex-1, c);
        quicksort(array, newPivotIndex+1, hi, c);
      }
    }
  
    private static int partition(int[] array, int lo, int hi, int pivotIndex, Comparator<Integer> c) {
      int pivotValue = array[pivotIndex];
  
      swap(array, pivotIndex, hi); //send to the back
  
      int index = lo;
  
      for (int i = lo; i < hi; i++) {
        if (c.compare(array[i], pivotValue) <= 0 ) {
          swap(array, i, index);
          index++;
        }
      }
  
      swap(array, hi, index);
      
      return index;
    }
  
    private static void swap(int[] array, int i, int j) {
      if (i == j) {
        return;
      }
      int temp = array[i];
      array[i] = array[j];
      array[j] = temp;
    }
  }
  
  public static class BinarySearcher {
    public static int binarySearch(int[] a, int deleted, Comparator<Integer> c) {
      int low = 0;
      int high = a.length - 1 - deleted;
      int mid;

      while( low <= high ) {
        // Skip initialy deleted docs
        mid = ( low + high ) / 2;

        int res = c.compare(a[mid], -1);
        if(res < 0) {
          low = mid + 1;
        } else if(res > 0 ) {
          high = mid - 1;
        } else {
          return mid;
        }
      }

      return -1;
    }
  }
  
  public static synchronized void addSegmentInfoReference(SegmentInfo si) {
    
    // DO NOTHING, as cloning is done only for R/O SRs.
    return;
    
    /*
    if (!segmentInfoReferences.containsKey(si)) {
      logger.error("Adding a reference to an unknown segment " + si);
      Exception e = new Exception();
      e.fillInStackTrace();
      e.printStackTrace();
      return;
    }
    
    int refs = 1;//segmentInfoReferences.get(si).incrementAndGet();
    logger.info("Segment " + si + " now has " + refs + " references.");
    
    if (refs > 1) {
      Exception e = new Exception();
      e.fillInStackTrace();
      e.printStackTrace();
    }
    */
  }
}
