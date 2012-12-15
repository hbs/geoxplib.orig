package com.geoxp.geo;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

/**
 * Servlet which generates a KML file from a coverage specification.
 * 
 */
@Singleton
public class KMLServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    //
    // Allocate global coverage
    //
    
    com.geoxp.geo.Coverage coverage = new com.geoxp.geo.Coverage();
    coverage.setAutoThresholds(0L);
    coverage.setAutoDedup(true);
    
    //
    // Loop over specs
    //
    
    for (String spec: req.getParameterValues("spec")) {
      int resolution;
      
      System.out.println("SPEC=" + spec);
      
      boolean add = true;
      
      if (spec.startsWith("+")) {
        spec = spec.substring(1);
        add = true;
      } else if (spec.startsWith("-")) {
        spec = spec.substring(1);
        add = false;
      }
      
      
      if (spec.startsWith("@")) {
        resolution = Integer.valueOf(spec.replaceAll(":.*","").substring(1));
        spec = spec.replaceAll("^@[^:]*:","");
      } else {       
        resolution = 0;
      }

      if (resolution > 0 || resolution < -6) {
        resolution = -6;
      }
      
      com.geoxp.geo.Coverage c;
      
      if (add) {
        coverage = GeoParser.parseArea(spec, resolution, coverage);
      } else {
        c = new Coverage();
        c.setAutoThresholds(0L);
        c.setAutoDedup(true);
        c = GeoParser.parseArea(spec, resolution, c);
        
        coverage = coverage.minus(coverage, c);
      }
    }
    
    //
    // Output KML
    //
    
    resp.setContentType("application/vnd.google-earth.kml+xml");
    resp.addHeader("Content-Disposition", "attachment; filename=\"GeoXP.kml\"");
    CoverageHelper.toKML(coverage, resp.getWriter(), null != req.getParameter("outline"));
  }
}
