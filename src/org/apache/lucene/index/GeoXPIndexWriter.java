package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DocumentsWriter.IndexingChain;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

public class GeoXPIndexWriter extends IndexWriter {

  public GeoXPIndexWriter(Directory d, Analyzer a, boolean create, MaxFieldLength mfl) throws CorruptIndexException, LockObtainFailedException, IOException {
    super(d,a,create,mfl);
    setMergeScheduler(new GeoXPConcurrentMergeScheduler(this));
  }

  public GeoXPIndexWriter(Directory d, Analyzer a, MaxFieldLength mfl) throws CorruptIndexException, LockObtainFailedException, IOException {
    super(d, a, mfl);
    setMergeScheduler(new GeoXPConcurrentMergeScheduler(this));
  }

  public GeoXPIndexWriter(Directory d, Analyzer a, IndexDeletionPolicy deletionPolicy, MaxFieldLength mfl) throws CorruptIndexException, LockObtainFailedException, IOException {
    super(d, a, deletionPolicy, mfl);
    setMergeScheduler(new GeoXPConcurrentMergeScheduler(this));
  }

  public GeoXPIndexWriter(Directory d, Analyzer a, boolean create, IndexDeletionPolicy deletionPolicy, MaxFieldLength mfl) throws CorruptIndexException, LockObtainFailedException, IOException {
    super(d, a, create, deletionPolicy, mfl);
    setMergeScheduler(new GeoXPConcurrentMergeScheduler(this));
  }

  GeoXPIndexWriter(Directory d, Analyzer a, boolean create, IndexDeletionPolicy deletionPolicy, MaxFieldLength mfl, IndexingChain indexingChain, IndexCommit commit) throws CorruptIndexException, LockObtainFailedException, IOException {
    super(d, a, create, deletionPolicy, mfl, indexingChain, commit);
    setMergeScheduler(new GeoXPConcurrentMergeScheduler(this));
  }

  public GeoXPIndexWriter(Directory d, Analyzer a, IndexDeletionPolicy deletionPolicy, MaxFieldLength mfl, IndexCommit commit) throws CorruptIndexException, LockObtainFailedException, IOException {
    super(d, a, deletionPolicy, mfl, commit);
    setMergeScheduler(new GeoXPConcurrentMergeScheduler(this));
  }

  class GeoXPReaderPool extends ReaderPool {
    @Override
    public synchronized SegmentReader get(SegmentInfo info, boolean doOpenStores, int readBufferSize, int termsIndexDivisor) throws IOException {
      //
      // Create a GeoXPSegmentReader if one does not yet exist for 'info'
      //
     
      //info.
      return null;
    }
  }
  
  
  
  // 
  // Return SegmentReader associated with a given SegmenInfo
  //
  public SegmentReader getSegmentReaderFromReadersPool(SegmentInfo si) throws IOException {
    return this.readerPool.get(si, false);
  }
  
  /**
   * Release a SegmentReader.
   * @param sr
   */
  public void releaseSegmentReader(SegmentReader sr) throws IOException {
    this.readerPool.release(sr);
  }
  
  public boolean infoIsLive(SegmentInfo info) {
    return this.readerPool.mapToLive(info) == info;
  }
  ////////////////////////////////  
}
