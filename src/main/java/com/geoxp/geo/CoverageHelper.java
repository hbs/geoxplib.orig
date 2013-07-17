package com.geoxp.geo;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class CoverageHelper {
  private static final int MIN_LOD = 256;
  private static final int MAX_LOD = -1;

  public static Coverage fromGeoCells(long[] geocells) {
    Coverage c = new Coverage();
    
    for (long geocell: geocells) {
      int resolution = ((int) (((geocell & 0xf000000000000000L) >> 60) & 0xf)) << 1;
      long hhcode = geocell << 4;
      c.addCell(resolution, hhcode);
    }
    
    c.optimize(0L);
    return c;
  }
  
  public static String toKML(Coverage coverage) throws IOException {
    StringWriter sw = new StringWriter();
    toKML(coverage, sw, true);
    return sw.toString();
  }
  
  public static void toKML(Coverage coverage, Writer writer, boolean outline) throws IOException {
    
    //
    // Extract cells to render
    //
    
    // FIXME(hbs): obfuscate cells so the use of HHCodes is not obvious
    //String[] cells = coverage.toString("#").split("#");

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

    boolean showpins = false;
    
    for (int res: coverage.getResolutions()) {
      for (long cell: coverage.getCells(res)) {
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
        if (outline) {
          writer.append("      <outline>1</outline>\n");
        } else {
          writer.append("      <outline>0</outline>\n");
        }
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
        
        if (showpins) {
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
}
