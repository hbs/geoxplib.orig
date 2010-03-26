package com.geocoord.server.servlet;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.geocoord.geo.HHCodeHelper;

public class KMLCellServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    //
    // Extract cells to render
    //
    
    // FIXME(hbs): obfuscate cells so the use of HHCodes is not obvious
    String[] cells = req.getParameterValues("cell");
    
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("application/vnd.google-earth.kml+xml");
    
    Writer writer = resp.getWriter();
    
    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    writer.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
    writer.append("<Document>\n");
    writer.append("<name>GeoXPGgrid</name>\n");
    
    String subcells = "0123456789abcdef";
    
    for (String cell: cells) {
      //
      // Output Region for bbox of the cell
      //
      int resolution = 2 * cell.length();
      long hhcode = HHCodeHelper.fromString(cell);
      double[] bbox = HHCodeHelper.getHHCodeBBox(hhcode, resolution);
      writer.append("  <Region>\n");
      writer.append("    <LatLonAltBox id=\"");
      writer.append(cell);
      writer.append("\">\n");
      writer.append("      <north>");
      writer.append(Double.toString(bbox[2]));
      writer.append("</north>\n");
      writer.append("      <south>");
      writer.append(Double.toString(bbox[0]));
      writer.append("</south>\n");
      writer.append("      <east>");
      writer.append(Double.toString(bbox[3]));
      writer.append("</east>\n");
      writer.append("      <west>");
      writer.append(Double.toString(bbox[1]));
      writer.append("</west>\n");
      writer.append("    </LatLonAltBox>\n");
      writer.append("    <Lod>\n");
      writer.append("      <minLodPixels>128</minLodPixels>\n");
      writer.append("      <maxLodPixels>1024</maxLodPixels>\n");
      writer.append("    </Lod>\n");
      writer.append("  </Region>\n");
      writer.append("  <Placemark>\n");
      writer.append("    <name>");
      writer.append(cell);
      writer.append("</name>\n");
      writer.append("    <MultiGeometry>\n");
      writer.append("      <LineString>\n");
      writer.append("        <tessellate>1</tessellate>\n");
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
      writer.append("      </LineString>\n");   
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
      
      //
      // Now output a network link per subcell
      //
      
      if (32 != resolution) {
        for (int i = 0; i < 16; i++) {
          hhcode = HHCodeHelper.fromString(cell + subcells.charAt(i));
          bbox = HHCodeHelper.getHHCodeBBox(hhcode, resolution + 1);
          writer.append("  <NetworkLink>\n");
          writer.append("    <name>");
          writer.append(cell + subcells.charAt(i));
          writer.append("</name>\n");
          writer.append("    <Region>\n");
          writer.append("      <LatLonAltBox>\n");
          writer.append("        <north>");
          writer.append(Double.toString(bbox[2]));
          writer.append("</north>\n");
          writer.append("        <south>");
          writer.append(Double.toString(bbox[0]));
          writer.append("</south>\n");
          writer.append("        <east>");
          writer.append(Double.toString(bbox[3]));
          writer.append("</east>\n");
          writer.append("        <west>");
          writer.append(Double.toString(bbox[1]));
          writer.append("</west>\n");
          writer.append("      </LatLonAltBox>\n");
          writer.append("      <Lod>\n");
          writer.append("        <minLodPixels>128</minLodPixels>\n");
          writer.append("        <maxLodPixels>1024</maxLodPixels>\n");
          writer.append("      </Lod>\n");
          writer.append("    </Region>\n");
          writer.append("    <Link>\n");
          writer.append("      <href>http://localhost:8888/kml/cell");
          writer.append("?cell=");
          writer.append(cell + subcells.charAt(i));
          writer.append("\n");
          writer.append("</href>\n");
          writer.append("      <viewRefreshMode>onRegion</viewRefreshMode>\n");
          writer.append("    </Link>");
          writer.append("  </NetworkLink>\n");          
        }
      }
    }
       
    writer.append("</Document>\n");
    writer.append("</kml>\n");
    
  }
}
