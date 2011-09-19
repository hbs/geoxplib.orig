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
    
    BufferedReader br = req.getReader();
    
    int count = 0;
    int valid = 0;
    
    long now = System.currentTimeMillis();
    
    boolean secretOk = false;
    
    while (true) {
      String line = br.readLine();
   
      if (null == line) {
        break;
      }

      if ("CLEAR".equals(line)) {
        manager.clear();
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
          
          manager.store(lat, lon, ts, value, true, r);          
        } else {
          manager.store(lat, lon, ts, value, true);          
        }
    
        valid++;        
      } catch (NumberFormatException nfe) {
        nfe.printStackTrace();
      }
      
      
    }
    
    resp.setContentType("text/plain");
    resp.getWriter().append("DataPoints=");
    resp.getWriter().append("" + count);
    resp.getWriter().append("\n");
    resp.getWriter().append("ValidDataPoints=");
    resp.getWriter().append("" + valid);
    resp.getWriter().append("\n");
    resp.getWriter().append("CurrentBucketCount=");
    resp.getWriter().append("" + manager.getBucketCount());
    resp.getWriter().append("\n");
  }
}
