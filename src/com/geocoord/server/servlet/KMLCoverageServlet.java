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
import com.geocoord.geo.HHCodeHelper;

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
    
    long thresholds = 0x0L;
    
    try {
      thresholds = new BigInteger(req.getParameter("thresholds"), 16).longValue();
    } catch (NumberFormatException nfe) {      
    }
    
    Coverage globalCoverage = new Coverage();
    
    int resolution = 0;
    
    try {
      resolution = Integer.parseInt(req.getParameter("res"));
    } catch (NumberFormatException nfe) {      
    }
    
    String[] paths = req.getParameterValues("path");
    
    if (null != paths) {

      writer.append("  <Placemark><MultiGeometry>\n");

      for (String path: paths) {
        List<Long> nodes = outputPath(writer,path);

        //
        // Compute Coverage
        //
        
        Coverage coverage = HHCodeHelper.coverPolyline(nodes, resolution, true);
        
        //
        // Merge coverage with current one.
        //
    
        globalCoverage.merge(coverage);
      }
      
      writer.append("  </MultiGeometry></Placemark>\n");
    }
    
    String[] polygons = req.getParameterValues("poly");
    
    if (null != polygons) {

        writer.append("  <Placemark><MultiGeometry>\n");

        for (String path: polygons) {
          List<Long> nodes = outputPath(writer,path);

          //
          // Compute Coverage
          //
          
          Coverage coverage = HHCodeHelper.coverPolygon(nodes, resolution);
          
          //
          // Merge coverage with current one.
          //
      
          globalCoverage.merge(coverage);
        }
        
        writer.append("  </MultiGeometry></Placemark>\n");
    }
    
    //
    // Optimize coverage.
    //
    
    globalCoverage.optimize(thresholds);
    
    System.out.println(globalCoverage.toString());
    //
    // Display all cells in the KML file
    //
    
    
   
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
        writer.append("      <Point>\n");
        writer.append("        <coordinates>\n");
        writer.append(Double.toString((bbox[3]+bbox[1])/2.0));
        writer.append(",");
        writer.append(Double.toString((bbox[2]+bbox[0])/2.0));
        writer.append(",0");
        writer.append("        </coordinates>\n");      
        writer.append("      </Point>\n");     
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
      
      try {
        double lat = Double.parseDouble(latlon[0]);
        double lon = Double.parseDouble(latlon[1]);
        
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