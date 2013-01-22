package com.geoxp.heatmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.geocoord.thrift.data.HeatMapAggregationType;
import com.geocoord.thrift.data.HeatMapConfiguration;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UpdateServlet extends HttpServlet {
    
  private HeatMapRegistry registry;

  @Inject
  public UpdateServlet(HeatMapRegistry registry) {
    this.registry = registry;
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    //
    // Extract heatmap name
    //
    
    String path = req.getPathInfo();
    
    String name = null;
    String secret = null;
    
    if (null != path) {
      name = path.substring(1);
    }
    
    //
    // Extract sub map and strip name
    //
    
    String submap = name.replaceAll("^[^/]+/", "");
    name = name.replaceAll("/.*", "");
    
    HeatMapManager manager = registry.getHeatMap(name + "." + submap);
    
    boolean issubmap = true;
    
    if (null == manager) {
      
      manager = registry.getHeatMap(name);
      issubmap = false;
      
      if (null == manager) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown HeatMap.");
        return;
      }
    }

    resp.setContentType("text/plain");

    BufferedReader br = req.getReader();
    
    int count = 0;
    int valid = 0;
    
    long now = System.currentTimeMillis();
    
    boolean secretOk = false;
    boolean update = true;
    
    boolean restore = false;
    
    long beforeBucketCount = manager.getBucketCount();
    
    while (true) {
      String line = br.readLine();
     
      if (null == line) {
        break;
      }

      if ("CLEAR".equals(line) && secretOk) {
        synchronized(manager) {
          manager.clear();
        }
        continue;
      }

      if ("EXPIRE".equals(line) && secretOk) {
        long threshold = Long.valueOf(line.substring(6).trim());
        synchronized(manager) {
          manager.expire(threshold);
        }
        continue;
      }

      //
      // Should we STORE the values instead of UPDATING them?
      //
        
      if ("STORE".equals(line) && secretOk) {
        update = false;
        continue;
      }

      if (line.startsWith("SNAPSHOT") && secretOk) {
        String[] res = line.substring(8).split(",");
          
        Collection<Integer> resolutions = new HashSet<Integer>();
          
        for (String r: res) {
          if (!"".equals(r.trim())) {
            resolutions.add(Integer.valueOf(r.trim()));
          }
        }
      
        synchronized(manager) {
          manager.snapshot(resp.getOutputStream(), resolutions);
        }
        continue;
      }

      if (line.equals("RESTORE") && secretOk) {
        restore = true;
        continue;
      }
      
      //
      // Check secret
      //
            
      if (!secretOk) {
        if (line.startsWith("SECRET")) {
          secret = line.substring(7).trim();
          synchronized(manager) {
            secretOk = manager.getConfiguration().getSecret().equals(secret);
          }
        }
        continue;
      }

      //
      // Create submap if it does not exist
      //
      
      if (line.startsWith("SUBMAP") && !issubmap && secretOk) {
        //
        // Split params
        //
        
        try {
          HeatMapConfiguration conf = HeatMapRegistry.parseHeatMapConfiguration(line.replaceAll("^SUBMAP\\s+",""));
                    
          //
          // Add prefix to conf name
          //
          
          conf.setName(manager.getConfiguration().getName() + "." + conf.getName());
          
          //
          // Check if such a manager already exists, if so check that the old def is compatible
          // with the new one
          //
          
          HeatMapManager submgr = registry.getHeatMap(conf.getName());
                    
          if (null != submgr) { 
            if (HeatMapRegistry.checkConfigurationCompatibility(conf, submgr.getConfiguration())) {
              submgr.setConfiguration(conf);
            } else {
              HeatMapManager m = new MemoryHeatMapManager(conf); 
              this.registry.deregisterHeatMap(conf.getName());
              this.registry.registerHeatMap(conf.getName(), m);
            }
          } else {
            HeatMapManager m = new MemoryHeatMapManager(conf); 
            manager.addChild(m);
            manager = m;
            registry.registerHeatMap(conf.getName(), m); 
            issubmap = true;
          }
          
        } catch (Exception e) {
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid SUBMAP spec " + e.getMessage());
          return;          
        }           
        continue;
      }
      
      //
      // Show maps
      //
      
      if (line.startsWith("MAPS") && !issubmap && secretOk) {
        for (HeatMapManager mgr: manager.getChildren()) {
          resp.getOutputStream().print("SUBMAP ");
          resp.getOutputStream().print(mgr.getConfiguration().getName().substring(manager.getConfiguration().getName().length() + 1));
          resp.getOutputStream().print(" ");
          resp.getOutputStream().print(mgr.getConfiguration().getMaxBuckets());
          boolean first = true;
          resp.getOutputStream().print(" ");
          for (Entry<Long,Integer> entry: mgr.getConfiguration().getBuckets().entrySet()) {
            if (!first) {
              resp.getOutputStream().print(",");
            }
            resp.getOutputStream().print(entry.getKey() + ":" + entry.getValue());
            first=false;
          }
          resp.getOutputStream().print(" ");
          resp.getOutputStream().print(mgr.getConfiguration().getAggregationType().name());
          resp.getOutputStream().print(" ");
          resp.getOutputStream().print(mgr.getConfiguration().isCentroidEnabled());
          resp.getOutputStream().print(" ");
          resp.getOutputStream().print(mgr.getConfiguration().getMinResolution());
          resp.getOutputStream().print(" ");
          resp.getOutputStream().print(mgr.getConfiguration().getMaxResolution());
          resp.getOutputStream().print(" ");
          resp.getOutputStream().print(mgr.getConfiguration().getSecret());
          resp.getOutputStream().println();
        }
        continue;
      }
      
      //
      // Destroy submap
      //
      
      if (line.startsWith("DELMAP") && !issubmap && secretOk) {
        String subname = line.replaceAll("^DELMAP\\s*","");
        this.registry.deregisterHeatMap(manager.getConfiguration().getName() + "." + subname);
        continue;
      }
      
      count++;
        
      if (restore) {
        synchronized(manager) {
          manager.restore(line);
        }
        continue;
      }
        
      String[] tokens = line.split(":");

      if (null == tokens || tokens.length < 4) {
        continue;
      }
        
      try {
        long ts = "".equals(tokens[0].trim()) ? now : Long.valueOf(tokens[0].trim());
        double lat = Double.valueOf(tokens[1].trim());
        double lon = Double.valueOf(tokens[2].trim());
        int value = Integer.valueOf(tokens[3].trim());
    
        if (tokens.length > 4) {
          String[] resolutions = tokens[4].split(",");
          
          Collection<Integer> r = new HashSet<Integer>();
          
          for (String res: resolutions) {
            r.add(Integer.valueOf(res.trim()));
          }
          
          synchronized(manager) {
            manager.store(lat, lon, ts, value, update, r);
          }
        } else {
          synchronized(manager) {
            manager.store(lat, lon, ts, value, update);
          }
        }
    
        valid++;        
      } catch (NumberFormatException nfe) {
        nfe.printStackTrace();
      }                
    }

    if (count > 0) {
      resp.addHeader("X-GeoXP-Buckets", beforeBucketCount + ":" + manager.getBucketCount());
      resp.addHeader("X-GeoXP-DataPoints", count + ":" + valid);
    }
  }
}
