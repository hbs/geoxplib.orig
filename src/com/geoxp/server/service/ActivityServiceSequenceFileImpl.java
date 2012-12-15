package com.geoxp.server.service;

import java.io.IOException;
import java.net.URI;

import net.iroise.commons.thrift.ThriftHelper;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.thrift.data.ActivityEvent;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.services.ActivityService;

public class ActivityServiceSequenceFileImpl extends Thread implements ActivityService.Iface {
  
  private static final Logger logger = LoggerFactory.getLogger(ActivityServiceSequenceFileImpl.class);
  
  private SequenceFile.Writer writer = null;
  
  private static final byte[] EMPTY_KEY = new byte[0];
  
  private static final String LOGDIR="/var/tmp/GeoXP/logdir";
  
  private long lastWriter = 0L;

  private long FLUSH_INTERVAL = 5000L;
  
  //
  // Rotate logs every 900s (15')
  //
  
  private long LOG_ROTATE_INTERVAL = 900000L;
  
  @Override
  public void run() {
    //
    // Loop forever, rotating log files
    //
    
    while(true) {
      try {
        Thread.sleep(FLUSH_INTERVAL);

        rotateLog();
      } catch (InterruptedException ie) {
        continue;
      } catch (IOException ioe) {
        logger.error("run", ioe);
      }
    }
  }
  
  /**
   * Create a new logFile.
   */
  private synchronized void rotateLog() throws IOException {
  
    if (System.currentTimeMillis() - lastWriter < LOG_ROTATE_INTERVAL) {
      return;
    }
    
    // Close existing writer
    if (null != writer) {
      writer.sync();
      writer.close();
    }
    
    StringBuilder uri = new StringBuilder();
    uri.append(LOGDIR);
    uri.append("/");
    uri.append(System.currentTimeMillis());
    uri.append(".sq");
    
    Configuration conf = new Configuration();
    conf.set("io.serializations", "org.apache.hadoop.io.serializer.JavaSerialization");
    FileSystem fs = FileSystem.get(URI.create(uri.toString()), conf);
    Path path = new Path(uri.toString());
    
    writer = SequenceFile.createWriter(fs, conf, path, byte[].class, byte[].class);
    
    lastWriter = System.currentTimeMillis();
  }
  
  private synchronized void writeEvent(ActivityEvent event) throws TException {        
    try {
      if (null == writer) {
        rotateLog();
      }

      writer.append(EMPTY_KEY, ThriftHelper.serialize(event));
    } catch (IOException ioe) {
      throw new TException(ioe);
    }
  }
  
  private synchronized void close() {
    if (null != writer) {
      try {
        writer.close();
      } catch (IOException ioe) {
        logger.error("close", ioe);
      }
    }
  }
  
  /**
   * Return a shutdown hook that will close the currently open file.
   * @return
   */
  private Thread getShutdownHook() {    
    final ActivityServiceSequenceFileImpl self = this;
    
    return new Thread() {
      @Override
      public void run() {
        self.close();
      }
    };
  }
  

  public ActivityServiceSequenceFileImpl() {
    
    // Register shutdown hook
    Runtime.getRuntime().addShutdownHook(this.getShutdownHook());
    
    this.setName("[" + ActivityServiceSequenceFileImpl.class.getName() + "]");
    this.setDaemon(true);
    this.start();
  }
  
  @Override
  public void record(ActivityEvent event) throws GeoCoordException, TException {
    writeEvent(event);
  }
}
