package com.geoxp.heatmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geocoord.thrift.data.HeatMapAggregationType;
import com.geocoord.thrift.data.HeatMapConfiguration;
import com.google.inject.Singleton;

@Singleton
public class HeatMapRegistry extends Thread {
  
  private Map<String,HeatMapManager> managers = new ConcurrentHashMap<String, HeatMapManager>();
  
  private static Logger logger = LoggerFactory.getLogger(HeatMapRegistry.class);
  
  public HeatMapRegistry() {
    this.setDaemon(true);
    this.start();
  }
  
  public HeatMapManager getHeatMap(String name) {
    if (null == name) {
      return null;
    } else {
      return managers.get(name);
    }
  }
  
  public void registerHeatMap(String name, HeatMapManager manager) {
    this.managers.put(name, manager);
  }
  
  public void deregisterHeatMap(String name) {
    HeatMapManager manager = this.managers.get(name);    
    if (null != manager) {
      HeatMapManager parent = manager.getParent();
      
      if (null != parent) {
        parent.removeChild(manager);
      }
      
      this.managers.remove(name);
      
      //
      // Deregister child managers
      //
      for (HeatMapManager mgr: manager.getChildren()) {
        deregisterHeatMap(mgr.getConfiguration().getName());
      }
    }
  }
  
  @Override
  public void run() {
    //
    // Periodically read the heatmaps conf directory
    //

    File confdir = new File("/var/tmp/heatmaps.conf");

    Set<String> knownHeatmaps = new HashSet<String>();
    
    while(true) {
      
      String[] files = confdir.list(new FilenameFilter() {
        
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(".conf");
        }
      });
      
      if (null != files) {
        for (String file: files) {
          try {
            BufferedReader br = new BufferedReader(new FileReader(new File(confdir, file)));
            
            HeatMapConfiguration conf = new HeatMapConfiguration();
            
            conf.setName(file.replaceAll("\\.conf$", ""));
      
            knownHeatmaps.add(conf.getName());
            
            while (true) {
              String line = br.readLine();
              if (null == line) {
                break;
              }
            
              String name = line.replaceAll("=.*", "").toLowerCase();
              String value = line.replaceAll(".*=", "");
              
              if ("minresolution".equals(name)) {
                conf.setMinResolution(Integer.valueOf(value));
              } else if ("maxresolution".equals(name)) {
                conf.setMaxResolution(Integer.valueOf(value));
              } else if ("maxbuckets".equals(name)) {
                conf.setMaxBuckets(Long.valueOf(value));
              } else if ("secret".equals(name)) {
                conf.setSecret(value);
              } else if ("lowwatermark".equals(name)) {
                conf.setLowWaterMark(Long.valueOf(value));
              } else if ("highwatermark".equals(name)) {
                conf.setHighWaterMark(Long.valueOf(value));
              } else if ("fastexpirettl".equals(name)) {
                conf.setFastExpireTTL(Long.valueOf(value));
              } else if ("buckets".equals(name)) {
                String span = value.replaceAll(":.*","");
                String count = value.replaceAll(".*:", "");
                
                conf.putToBuckets(Math.abs(Long.valueOf(span)), Math.abs(Integer.valueOf(count)));
              } else if ("resolutionoffset".equals(name)) {
                conf.setResolutionOffset(Integer.valueOf(value));
              } else if ("centroidenabled".equals(name)) {
                conf.setCentroidEnabled("true".equals(value));
              } else if ("aggregationtype".equals(name)) {
                try {
                  conf.setAggregationType(HeatMapAggregationType.valueOf(value));
                } catch (IllegalArgumentException iae) {
                  logger.error("Illegal aggregation type " + value + ", using SUM.");
                }
              }
            }
            
            br.close();

            //
            // Do some sanity checks
            //
            
            assert (conf.getMaxResolution() > 0) : "MaxResolution <= 0";
            assert (conf.getMinResolution() > 0) : "MinResolution <= 0";
            assert (conf.getBucketsSize() > 0) : "Missing buckets";
            assert (conf.getLowWaterMark() < conf.getHighWaterMark()) : "LWM >= HWM";
            assert (null != conf.getSecret()) : "Missing secret";
            
            //
            // Retrieve current config
            //
            
            HeatMapManager mgr = managers.get(conf.getName());
      
            //
            // No known manager by this name, create a new one
            //
            
            if (null == mgr) {
              // FIXME(hbs): add a way to choose the type of manager (memory, bdb, memcached, ...).                
              managers.put(conf.getName(), new MemoryHeatMapManager(conf));
            } else {
              //
              // Check if configuration has changed, if not do nothing
              //
              
              if (conf.equals(mgr.getConfiguration())) {
                continue;
              }
              
              //
              // Check if buckets, centroid enabling or aggregation type changed.
              // If so we need to reset the manager.
              //
    
              if (!checkConfigurationCompatibility(conf, mgr.getConfiguration())) {
                deregisterHeatMap(conf.getName());
                this.managers.put(conf.getName(), new MemoryHeatMapManager(conf));
              } else {
                // Simply replace the configuration
                mgr.setConfiguration(conf);
              }
            }
            
          } catch (IOException ioe) {
            logger.error("run", ioe);
          } catch (NumberFormatException nfe) {
            logger.error("run", nfe);
          } catch (AssertionError ae) {
            logger.error("run", ae);
          }
        }          
      }

      //
      // Now remove heatmaps we don't have a config file for
      //
      
      for (String name: managers.keySet()) {
        if (!knownHeatmaps.contains(name)) {
          managers.remove(name);
        }
      }
      
      try {
        Thread.sleep(60000L);
      } catch (InterruptedException ie) {        
      }
    }
  }
  
  public static HeatMapConfiguration parseHeatMapConfiguration(String str) throws IOException {
    HeatMapConfiguration conf = new HeatMapConfiguration();
    
    String[] tokens = str.split("\\s+");
    
    //
    // Format is
    //
    // NAME MAXBUCKETS BUCKETSPEC AGGREGATION CENTROID MINRES MAXRES SECRET
    //
    
    if (tokens.length != 8) {
      throw new IOException("Invalid map spec, should be NAME MAXBUCKETS BUCKETSPEC AGGREGATION CENTROID MINRES MAXRES SECRET");
    }
    
    try {
      conf.setName(tokens[0]);
      conf.setMaxBuckets(Long.valueOf(tokens[1]));
      
      String[] bucketspecs = tokens[2].split(",");
      
      for (String bucketspec: bucketspecs) {
        String span = bucketspec.replaceAll(":.*","");
        String count = bucketspec.replaceAll(".*:", "");
        
        conf.putToBuckets(Math.abs(Long.valueOf(span)), Math.abs(Integer.valueOf(count)));
      }
      
      if (conf.getBucketsSize() == 0) {
        throw new IOException("Invalid bucket spect, should be span:count[,span:count]");
      }
      
      conf.setAggregationType(HeatMapAggregationType.valueOf(tokens[3]));
      conf.setCentroidEnabled(Boolean.valueOf(tokens[4]));
      conf.setMinResolution(Integer.valueOf(tokens[5]));
      conf.setMaxResolution(Integer.valueOf(tokens[6]));
      conf.setSecret(tokens[7]);
      
      //
      // Force the following parameters
      //
      
      conf.setResolutionOffset(4);
      
      return conf;
    } catch (NumberFormatException nfe) {
      throw new IOException("Invalid number");
    } catch (IllegalArgumentException iae) {
      throw new IOException("Invalid aggregation type");
    }
  }
  
  public static boolean checkConfigurationCompatibility(HeatMapConfiguration conf1, HeatMapConfiguration conf2) {
    //
    // Configurations are compatible iff they have the same buckets, the same aggregation type
    // and the same value of centroidEnable
    //
    
    if ((!conf1.getBuckets().equals(conf2.getBuckets()))
        || ! (conf1.isCentroidEnabled() == conf2.isCentroidEnabled())
        || ! (conf1.getAggregationType().equals(conf2.getAggregationType()))) {
      return false;
    } else {
      return true;
    }
  }
}
