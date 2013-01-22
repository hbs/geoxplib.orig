package com.geoxp.heatmap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.geoxp.heatmap.TileBuilder.Scaler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TileServlet extends HttpServlet {
  
  private static final String SENDFILE_DIR = null != System.getProperty("geoxp.sendfile.dir") ? System.getProperty("geoxp.sendfile.dir") : "/var/tmp/geoxp.sendfile.dir";
  
  private static final byte[] EMPTY_TILE_256x256 = new BigInteger("89504e470d0a1a0a0000000d49484452000001000000010008060000005c72a866000000017352474200aece1ce900000006624b474400ff00ff00ffa0bda793000000097048597300000b1300000b1301009a9c180000000774494d4507db090b0c3635b8890f0e000001154944415478daedc13101000000c2a0f54fed6b08a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000007803013c0001d82943040000000049454e44ae426082",16).toByteArray();
  
  private final HeatMapRegistry registry;
  
  /**
   * Default scaler, simply caps values to 10.0
   */
  private static Scaler DEFAULT_SCALER = new Scaler() {        
    @Override
    public double scale(double value) {
      return Math.min(10.0, value);
    }
  };
  
  /**
   * Maximum value we accept for the intensity
   */
  private static double MAX_CAP = 255.0;
  
  @Inject
  public TileServlet(HeatMapRegistry registry) {
    this.registry = registry;
  }
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    long nano = System.nanoTime();
    
    //
    // Attempt to retrieve the builder
    //
    
    HeatMapManager manager = this.registry.getHeatMap(req.getParameter("hm"));
    
    if (null == manager) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Heat Map.");
      return;
    }
    
    TileBuilder tb = manager.getTileBuilder();
    
    if (null == tb) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Heat Map.");
      return;      
    }
    
    int x = Integer.valueOf(req.getParameter("x"));
    int y = Integer.valueOf(req.getParameter("y"));
    int z = Integer.valueOf(req.getParameter("z"));
    
    String scaling = req.getParameter("s");
    
    Scaler scaler;

    //
    // If no scaler was defined, scaler is the default one, i.e. one that
    // simply caps values at 10.0
    //
    
    if (null == scaling) {
      scaler = DEFAULT_SCALER;
    } else if (scaling.startsWith("log")) {
      //
      // Scaling is logXX:MIN:MAX
      //
      
      String tokens[] = scaling.substring(3).split(":");
      
      if (3 != tokens.length) {
        scaler = DEFAULT_SCALER;
      } else {
        final double logscale = Double.valueOf(tokens[0]);
        final double minvalue = Math.max(0.0, Double.valueOf(tokens[1]));
        final double maxvalue = Math.min(Double.valueOf(tokens[2]), MAX_CAP);
        
        if (logscale > 1.0) {
          scaler = new Scaler() {
            
            @Override
            public double scale(double value) {
              return Math.max(minvalue, Math.min(Math.log(value)/Math.log(logscale), maxvalue));
            }
          };        
        } else {
          scaler = DEFAULT_SCALER;
        }        
      }
    } else if (scaling.startsWith("mul")) {
      //
      // Scaling is mulXX:MIN:MAX
      //
      
      String tokens[] = scaling.substring(3).split(":");
      
      if (3 != tokens.length) {
        scaler = DEFAULT_SCALER;
      } else {
        final double mulscale = Double.valueOf(tokens[0]);
        final double minvalue = Math.max(0.0, Double.valueOf(tokens[1]));
        final double maxvalue = Math.min(Double.valueOf(tokens[2]), MAX_CAP);
        
        if (mulscale >= 0) {
          scaler = new Scaler() {
            
            @Override
            public double scale(double value) {
              return Math.max(minvalue, Math.min(value * mulscale, maxvalue));
            }
          };        
        } else {
          scaler = DEFAULT_SCALER;
        }              
      }
    } else {
      scaler = DEFAULT_SCALER;
    }
    
    int[] palette = null;
    
    if (null != req.getParameter("p")) {
      palette = ColorMap.get(req.getParameter("p"));
    }
    
    if (null == palette) {
      palette = ColorMap.FIRE;
    }
    
    long timestamp = 0;
    
    if (null != req.getParameter("t")) {
      timestamp = Long.valueOf(req.getParameter("t"));
    }
    
    if (0 == timestamp) {
      timestamp = System.currentTimeMillis();
    }
    
    long bucketspan = 0;
    
    if (null != req.getParameter("bs")) {
      bucketspan = Long.valueOf(req.getParameter("bs"));
    }
    
    int bucketcount = 1;
    
    if (null != req.getParameter("bc")) {
      bucketcount = Integer.valueOf(req.getParameter("bc"));
    }
    
    //palette = ColorMap.generate(0xff0000);
    double opacity = null != req.getParameter("o") ? Double.valueOf(req.getParameter("o")) : 1.0;
    
    if (opacity < 0 || opacity > 1) {
      opacity = 1.0;
    }
    
    Radiator radiator;
    
    if (null != req.getParameter("r")) {
      radiator = Radiator.get(req.getParameter("r"));
    } else {
      radiator = Radiator.get("default");
    }
    
    double timedecay = 1.0;
    
    if (null != req.getParameter("td")) {
      timedecay = Double.valueOf(req.getParameter("td"));
    }
    
    String thr = "[" + Thread.currentThread().getId() + "] ";
    
    BufferedImage bi = tb.getTile(timestamp, bucketspan, bucketcount, timedecay, scaler, x, y, z, radiator, palette, opacity);
    
    if (null == bi) {
      resp.setContentLength(EMPTY_TILE_256x256.length - 1);
      resp.setContentType("image/png");
      resp.getOutputStream().write(EMPTY_TILE_256x256,1,EMPTY_TILE_256x256.length - 1);
      return;
    }
    
    boolean sendfile = false;
    
    long now = System.currentTimeMillis();
    now = now - (now % 120000);
    
    if (Boolean.TRUE.equals(req.getAttribute("org.apache.tomcat.sendfile.support"))) {
      sendfile = true;
      File f = new File(SENDFILE_DIR, Long.toString(now));
      
      f.mkdirs();
      
      f = new File(f, UUID.randomUUID().toString());
      
      ImageIO.write(bi, "PNG", f);
      
      long len = f.length();
      req.setAttribute("org.apache.tomcat.sendfile.filename", f.getCanonicalPath());
      req.setAttribute("org.apache.tomcat.sendfile.start", 0L);
      req.setAttribute("org.apache.tomcat.sendfile.end", len); 
      resp.setHeader("Content-Length", Long.toString(len));
      resp.flushBuffer();
    } else {
      
      resp.addHeader("X-GeoXP-HeatMap", Double.toString((System.nanoTime() - nano)/1000000.0));
      resp.addHeader("Cache-Control", "max-age=10000");
      
      ImageIO.write(bi, "PNG", resp.getOutputStream());      
    }
  }
}
