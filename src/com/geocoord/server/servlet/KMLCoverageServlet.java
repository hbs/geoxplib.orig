package com.geocoord.server.servlet;

import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.geocoord.geo.Coverage;
import com.geocoord.geo.CoverageHelper;
import com.geocoord.geo.GeoParser;
import com.geocoord.geo.HHCodeHelper;

/**
 * Servlet to return a KML file given a coverage specification.
 * 
 * Test case for coverPolygon: http://127.0.0.1:8888/kml/coverage?resolution=-4&path=10000:48:-4.5,49:-3.5,48:0
 * Test case for circle: 
 * @author hbs
 *
 */
public class KMLCoverageServlet extends HttpServlet {
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("application/vnd.google-earth.kml+xml");
    
    Writer writer = resp.getWriter();
    
    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    writer.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
    writer.append("<Document>\n");
    writer.append("  <name>GeoXP Coverage</name>\n");
    writer.append("  <ScreenOverlay>\n");
    writer.append("    <overlayXY x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
    writer.append("    <screenXY x=\"20\" y=\"50\" xunits=\"pixels\" yunits=\"pixels\"/>\n");
    writer.append("    <size>-1</size>\n");
    writer.append("    <Icon>\n");
    // FIXME(hbs): replace image URL
    writer.append("      <href>http://farm5.static.flickr.com/4056/4477523262_bfd831c564_o.png</href>\n");
    writer.append("    </Icon>\n");
    writer.append("  </ScreenOverlay>\n");
    
   
    // Extract thresholds.
    long thresholds = 0x0L;

    if (null != req.getParameter("thresholds")) {
      try {
        thresholds = new BigInteger(req.getParameter("thresholds"), 16).longValue();
      } catch (NumberFormatException nfe) {      
      }      
    }
    
    // Extract max cells
    
    int maxcells = 0;
    
    if (null != req.getParameter("maxcells")) {
      try {
        maxcells = Integer.valueOf(req.getParameter("maxcells"));
      } catch (NumberFormatException nfe) {
        
      }      
    }
    
    Coverage globalCoverage = new Coverage();
    
    int resolution = 0;
    
    if (null != req.getParameter("resolution")) {
      try {
        resolution = Integer.parseInt(req.getParameter("resolution"));
      } catch (NumberFormatException nfe) {      
      }      
    }
    
    String[] pathparams = new String[] { "path", "pathhole" };
    
    for (String pathparam: pathparams) {
      String[] paths = req.getParameterValues(pathparam);
      
      if (null != paths) {

        writer.append("  <Placemark><MultiGeometry>\n");

        for (String path: paths) {
          List<Long> nodes = outputPath(writer,path);

          //
          // Compute Coverage
          //
          
          Coverage coverage = GeoParser.parsePath(path, resolution); //HHCodeHelper.coverPolyline(nodes, resolution, true);
          
          //
          // Merge or remove coverage
          //
      
          if ("pathhole".equals(pathparam)) {
            globalCoverage = Coverage.minus(globalCoverage, coverage);
          } else {
            globalCoverage.merge(coverage);
          }
        }
        
        writer.append("  </MultiGeometry></Placemark>\n");
      }
      
    }
    
    String[] polyparams = new String[] { "polygon", "polygonhole" };
    
    for (String polyparam: polyparams) {
      String[] polygons = req.getParameterValues(polyparam);
      
      if (null != polygons) {

          writer.append("  <Placemark><MultiGeometry>\n");

          for (String path: polygons) {
            List<Long> nodes = outputPath(writer,path);

            //
            // Compute Coverage
            //
            
            Coverage coverage = GeoParser.parsePolygon(path, resolution); // rHHCodeHelper.coverPolygon(nodes, resolution);
            
            //
            // Merge or remove coverage
            //
        
            if ("polygonhole".equals(polyparam)) {
              globalCoverage = Coverage.minus(globalCoverage, coverage);
            } else {
              globalCoverage.merge(coverage);
            }
          }
          
          writer.append("  </MultiGeometry></Placemark>\n");
      }      
    }

    String[] circleparams = new String[] { "circle", "circlehole" };
    
    for (String circleparam: circleparams) {
      String[] circles = req.getParameterValues(circleparam);
      
      if (null != circles) {

          for (String circle: circles) {
            //
            // Compute Coverage
            //
            
            Coverage coverage = GeoParser.parseCircle(circle, resolution);
        
            System.out.println("CIRCLE COVERAGE=" + coverage);
            //
            // Merge or remove coverage
            //
        
            if ("circlehole".equals(circleparam)) {
              globalCoverage = Coverage.minus(globalCoverage, coverage);
            } else {
              globalCoverage.merge(coverage);
            }
          }
      }      
    }

    //
    // Intersect with possible viewport
    //
    
    if (null != req.getParameter("viewport")) {
      String[] latlon = req.getParameter("viewport").split(",");
      
      String[] sw = latlon[0].split(":");
      String[] ne = latlon[1].split(":");

      try {
        double swlat = Double.valueOf(sw[0]);
        double swlon = Double.valueOf(sw[1]);
        
        double nelat = Double.valueOf(ne[0]);
        double nelon = Double.valueOf(ne[1]);
       
        if (swlat > nelat) {
          double tmp = swlat;
          swlat = nelat;
          nelat = tmp;
        }
        
        if (swlon > nelon) {
          double tmp = swlon;
          swlon = nelon;
          nelon = tmp;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(swlat);
        sb.append(":");
        sb.append(swlon);
        sb.append(",");
        sb.append(swlat);
        sb.append(":");
        sb.append(nelon);
        sb.append(",");
        sb.append(nelat);
        sb.append(":");
        sb.append(nelon);
        sb.append(",");
        sb.append(nelat);
        sb.append(":");
        sb.append(swlon);
        sb.append(",");
        sb.append(swlat);
        sb.append(":");
        sb.append(swlon);

        outputPath(writer, sb.toString());

        System.out.println(sb.toString());
        
        Coverage coverage = GeoParser.parseViewport(req.getParameter("viewport"), resolution);

        System.out.println("VIEWPORT = " + coverage.toString());
        
        //
        // Intersect global coverage with viewport
        //
        
        globalCoverage = Coverage.intersection(globalCoverage, coverage);
        
      } catch (NumberFormatException nfe) {
        
      }
    }
    
    //
    // Optimize coverage.
    //
    
    globalCoverage.optimize(thresholds, HHCodeHelper.MIN_RESOLUTION);
    
    if (maxcells > 0) {
      globalCoverage.reduce(maxcells);
    }
    
    System.out.println(globalCoverage.toString());
    
    //
    // Display all cells in the KML file
    //
           
    boolean showpins = null != req.getParameter("nopins");
    
    for (int res: globalCoverage.getResolutions()) {
      for (long cell: globalCoverage.getCells(res)) {
        double[] bbox = HHCodeHelper.getHHCodeBBox(cell, res);
        
        writer.append("  <Placemark>\n");
        writer.append("  <Style>\n");
        writer.append("    <LineStyle>\n");
        writer.append("      <color>c0008000</color>\n");        
        writer.append("      <width>1</width>\n");
        writer.append("    </LineStyle>\n");
        writer.append("    <PolyStyle>\n");
        writer.append("      <color>c0f0f0f0</color>\n");
        writer.append("      <fill>1</fill>\n");
        writer.append("      <outline>1</outline>\n");    
        writer.append("    </PolyStyle>\n");    
        writer.append("  </Style>\n");        
        writer.append("    <name>");
        writer.append(HHCodeHelper.toString(cell, res));
        writer.append("</name>\n");
        writer.append("    <MultiGeometry>\n");

        writer.append("      <tessellate>1</tessellate>\n");
        writer.append("      <Polygon><outerBoundaryIs><LinearRing>\n");
        writer.append("        <coordinates>\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[1]));
        writer.append(",");
        writer.append(Double.toString(bbox[0]));
        writer.append(",0\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[1]));
        writer.append(",");
        writer.append(Double.toString(bbox[2]));
        writer.append(",0\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[3]));
        writer.append(",");
        writer.append(Double.toString(bbox[2]));
        writer.append(",0\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[3]));
        writer.append(",");
        writer.append(Double.toString(bbox[0]));
        writer.append(",0\n");
        writer.append("          ");
        writer.append(Double.toString(bbox[1]));
        writer.append(",");
        writer.append(Double.toString(bbox[0]));
        writer.append(",0\n");
        
        writer.append("        </coordinates>\n");      
        writer.append("      </LinearRing></outerBoundaryIs></Polygon>\n");   
        
        if (!showpins) {
          writer.append("      <Point>\n");
          writer.append("        <coordinates>\n");
          writer.append(Double.toString((bbox[3]+bbox[1])/2.0));
          writer.append(",");
          writer.append(Double.toString((bbox[2]+bbox[0])/2.0));
          writer.append(",0");
          writer.append("        </coordinates>\n");      
          writer.append("      </Point>\n");
        }
        writer.append("    </MultiGeometry>\n");
        writer.append("  </Placemark>\n");
      }
    }
   
    writer.append("</Document>\n");
    writer.append("</kml>\n");
  }

  private List<Long> outputPath(Writer writer, String path) throws IOException {
    // Split on ','
    String[] coords = path.split(",");
    
    List<Long> vertices = new ArrayList<Long>();
    
    writer.append("    <LineString>\n");
    writer.append("      <tessellate>1</tessellate>\n");
    writer.append("      <coordinates>\n");

    for (String coord: coords) {
      // Split on ':'
      String[] latlon = coord.split(":");
      
      // Index of latitude.
      int latidx;
      
      if (latlon.length == 3) {
        // There is a distance specified...
        latidx = 1;
      } else {
        latidx = 0;
      }
      
      try {
        double lat = Double.parseDouble(latlon[latidx]);
        double lon = Double.parseDouble(latlon[latidx+1]);
        
        long hhcode = HHCodeHelper.getHHCodeValue(lat, lon);

        writer.append("        ");
        writer.append(Double.toString(lon));
        writer.append(",");
        writer.append(Double.toString(lat));
        writer.append(",0\n");
        
        vertices.add(hhcode);
      } catch (NumberFormatException nfe) {
        
      }
    }

    writer.append("      </coordinates>\n");
    writer.append("    </LineString>\n");

    return vertices;
  }
}