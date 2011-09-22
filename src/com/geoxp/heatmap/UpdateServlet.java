package com.geoxp.heatmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    
    HeatMapManager manager = registry.getHeatMap(name);
    
    if (null == manager) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown HeatMap.");
      return;
    }

    resp.setContentType("text/plain");

    BufferedReader br = req.getReader();
    
    int count = 0;
    int valid = 0;
    
    long now = System.currentTimeMillis();
    
    boolean secretOk = false;
    boolean update = true;
    
    long tsoffset = 0;
    
    long beforeBucketCount = manager.getBucketCount();
    
    while (true) {
      String line = br.readLine();
   
      if (null == line) {
        break;
      }

      if ("CLEAR".equals(line) && secretOk) {
        manager.clear();
        continue;
      }

      //
      // Should we STORE the values instead of UPDATING them?
      // This is useful when reloading a SNAPSHOT.
      //
      
      if ("STORE".equals(line) && secretOk) {
        update = false;
        continue;
      }

      if ("TIMESTAMP".equals(line) && secretOk) {
        tsoffset = System.currentTimeMillis() - Long.valueOf(line.substring(9).trim());
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
        
        manager.snapshot(resp.getOutputStream(), resolutions);
        continue;
      }

      if (!secretOk) {
        if (line.startsWith("SECRET")) {
          secret = line.substring(7).trim();
          secretOk = manager.getConfiguration().getSecret().equals(secret);
        }
        continue;
      }

      count++;
      
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
          
          manager.store(lat, lon, ts + tsoffset, value, update, r);          
        } else {
          manager.store(lat, lon, ts + tsoffset, value, update);          
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
