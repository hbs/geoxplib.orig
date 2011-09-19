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
              // Check if buckets changed or if centroid enabling did, if so we need to reset the manager
              //
              
              if ((!conf.getBuckets().equals(mgr.getConfiguration().getBuckets()))
            || ! (conf.isCentroidEnabled() == mgr.getConfiguration().isCentroidEnabled())) {
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
}
