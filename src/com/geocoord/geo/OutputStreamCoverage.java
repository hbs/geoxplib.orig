package com.geocoord.geo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PrintStream;
import java.io.SequenceInputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.internal.matchers.SubstringMatcher;

import com.fasterxml.sort.DataReader;
import com.fasterxml.sort.SortConfig;
import com.fasterxml.sort.std.TextFileSorter;

public class OutputStreamCoverage extends Coverage {
  
  private final OutputStream os;
  private final byte[] suffix;
  
  public OutputStreamCoverage(OutputStream os) {
    this.os = os;
    this.suffix = null;
  }
  
  public OutputStreamCoverage(OutputStream os, String suffix) {
    this.os = os;
    this.suffix = suffix.getBytes();
  }
  
  @Override
  public void addCell(int resolution, long lat, long lon) {
    //
    // Make sure lat/lon are in the 0->2**32-1 range
    //
    
    lat = ((lat % (1L << HHCodeHelper.MAX_RESOLUTION)) + (1L << HHCodeHelper.MAX_RESOLUTION)) % (1L << HHCodeHelper.MAX_RESOLUTION);
    lon = ((lon % (1L << HHCodeHelper.MAX_RESOLUTION)) + (1L << HHCodeHelper.MAX_RESOLUTION)) % (1L << HHCodeHelper.MAX_RESOLUTION);
    
    addCell(resolution, HHCodeHelper.buildHHCode(lat, lon, HHCodeHelper.MAX_RESOLUTION));
  }
  
  @Override
  public void addCell(int resolution, long hhcode) {
    int r = (resolution >> 1) - 1;
    
    // Do nothing if resolution out of range
    if (0 != (r & 0xfffffff0)) {
      return;
    }

    try {
      os.write(HHCodeHelper.toString(hhcode, resolution).getBytes());
      if (null != suffix) {
        os.write(suffix);
      }
      os.write('\n');
    } catch (IOException ioe) {      
    }    
  }
  
  @Override
  public void merge(Coverage other) {
    Map<Integer, Set<Long>> cells = other.getAllCells();
    
    for (int r: cells.keySet()) {
      for (long hhcode: cells.get(r)) {
        try {
          os.write(HHCodeHelper.toString(hhcode, r).getBytes());
          os.write('\n');
        } catch (IOException ioe) {      
        }            
      }
    }
  }
  
  public static void merge(InputStream in, InputStream in2, OutputStream out) throws IOException {
    SequenceInputStream seq = new SequenceInputStream(in, in2);
    
    BufferedReader br = new BufferedReader(new InputStreamReader(seq));
    PrintStream ps = new PrintStream(out);
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        break;
      }
      ps.println(line);
    }
    
    br.close();
    ps.close();
  }
  
  /**
   * Optimize a coverage streamed from an InputStream.
   * 
   * @param in
   * @param thresholds
   * @param minresolution
   * @throws IOException
   */
  public static void optimize(InputStream in, OutputStream out, long thresholds, int minresolution) throws IOException {
    
    //
    // Sort input
    //
    
    File tmpfile = File.createTempFile("OutputStreamCoverage.optimize", "");
    tmpfile.deleteOnExit();
    
    TextFileSorter sorter = new TextFileSorter(new SortConfig().withMaxMemoryUsage(2 * 1000 * 1000));
    OutputStream tmpos = new FileOutputStream(tmpfile);
    sorter.sort(in, tmpos);
    tmpos.close();
    
    in = new FileInputStream(tmpfile);
    
    //
    // Split the thresholds
    //
    
    String hexdigit = "0123456789abcdef";
    
    int[] resthresholds = new int[16];
    
    for (int i = 0; i < 16; i++) {
      resthresholds[i] = (int) ((thresholds >> (60 - 4 * i)) & 0xf);
    }
    
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    
    int threshold = 17;
    String lastprefix = null;
    
    short subcells = 0;
    
    while(true) {
      String line = br.readLine();

      if (null == line) {
        break;
      }

      if (line.length() * 2 <= minresolution) {
        out.write(line.getBytes());
        out.write('\n');
        continue;
      }
      
      String prefix = line.substring(0, line.length() - 1);
      String suffix = line.substring(prefix.length());
      
      if (null == lastprefix) {
        subcells = (short) (1 << Integer.valueOf(suffix, 16));
        threshold = resthresholds[line.length() - 1];
        if (0 == threshold) {
          threshold = 16;
        }
        lastprefix = prefix;
      } else if (prefix.equals(lastprefix)) {
        subcells |= (short) (1 << Integer.valueOf(suffix, 16));
      } else {
        // prefix has changed, count the number of set bits
        int v = subcells & 0xffff;
        
        int set = 0;
        
        while (v > 0) {
          v &= (v - 1);
          set++;
        }
        
        if (set >= threshold) {
          out.write(lastprefix.getBytes());
          out.write('\n');
        } else {
          for (int i = 0; i < 16; i++) {
            if (0 != (subcells & (short) (1 << i))) {
              out.write(lastprefix.getBytes());
              out.write(hexdigit.charAt(i));
              out.write('\n');
            }
          }
        }
        
        subcells = (short) (1 << Integer.valueOf(suffix, 16));
        threshold = resthresholds[line.length() - 1];
        if (0 == threshold) {
          threshold = 16;
        }
        lastprefix = prefix;
      }      
    }
    
    br.close();
    
    // prefix has changed, count the number of set bits
    int v = subcells & 0xffff;
    
    int set = 0;
    
    while (v > 0) {
      v &= (v - 1);
      set++;
    }

    if (set >= threshold) {
      out.write(lastprefix.getBytes());
      out.write('\n');
    } else {
      for (int i = 0; i < 16; i++) {
        if (0 != (subcells & (short) (1 << i))) {
          out.write(lastprefix.getBytes());
          out.write(hexdigit.charAt(i));
          out.write('\n');
        }
      }
    }
    
    out.close();
  }
  
  public static void normalize(InputStream in, OutputStream out, int resolution) throws IOException {

    BufferedReader br = new BufferedReader(new InputStreamReader(in));

    PrintStream ps = new PrintStream(out);
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        break;
      }
      
      if (2 * line.length() >= resolution) {
        ps.println(line.substring(0, resolution >> 1));        
      } else {
        for (int i = 0; i < (1 << (2 * (resolution - 2 * line.length()))); i++) {
          ps.print(line);
          ps.println(Long.toHexString(0x8000000000000000L|i).substring(16 - ((resolution >> 1) - line.length())));
        }
      }
    }
    
    br.close();
    ps.close();
  }
  
  public static void minus(InputStream in, final InputStream minus, OutputStream out) throws IOException {
    //
    // Combine coverage and 'minus' coverage
    //
    InputStream wrappedminus = new InputStream() {

      private boolean lf = false;
      
      @Override
      public int read() throws IOException {
        if (lf) {
          lf = false;
          return '\n';
        } else {
          int c = minus.read();
          
          if ('\n' == c) {
            lf = true;
            return '-';
          } else {
            return c;
          }
        }
      }
      
      @Override
      public void close() throws IOException {
        super.close();
        minus.close();
      }
    };
    
    SequenceInputStream seq = new SequenceInputStream(in, wrappedminus);
    
    //
    // Sort into a temp file
    //
    
    TextFileSorter sorter = new TextFileSorter(new SortConfig().withMaxMemoryUsage(2 * 1000 * 1000));
    File tmpfile = File.createTempFile("OuputStreamCoverage.minus", "");
    tmpfile.deleteOnExit();
    FileOutputStream tmp = new FileOutputStream(tmpfile);
    sorter.sort(seq, tmp);
    tmp.close();
    
    //
    // Now read sorted temp file and remove cells which have a '-' equivalent
    //
    
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tmpfile)));
    
    String last = null;
    
    PrintStream ps = new PrintStream(out);
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        if (null != last) {
          ps.println(last);
        }
        break;
      }
      
      if (null == last && !line.endsWith("-")) {
        last = line;
      } else if (null == last) {
        // Ignore orphaned '-' cells
        continue;
      } else if (line.startsWith(last) && line.endsWith("-") && line.length() == last.length() + 1) {
        last = null; 
        continue;
      } else if (line.endsWith("-")) {
        ps.println(last);
        last = null;
      } else if (!line.equals(last)){
        ps.println(last);
        last = line;
      }
    }
    
    br.close();
    ps.close();
  }
  
  public static void intersection(InputStream in, final InputStream intersect, OutputStream out) throws IOException {
    //
    // Combine coverage and 'minus' coverage
    //
    InputStream wrappedminus = new InputStream() {

      private boolean lf = false;
      
      @Override
      public int read() throws IOException {
        if (lf) {
          lf = false;
          return '\n';
        } else {
          int c = intersect.read();
          
          if ('\n' == c) {
            lf = true;
            return '+';
          } else {
            return c;
          }
        }
      }
      
      @Override
      public void close() throws IOException {
        super.close();
        intersect.close();
      }
    };
    
    SequenceInputStream seq = new SequenceInputStream(in, wrappedminus);
    
    //
    // Sort into a temp file
    //
    
    TextFileSorter sorter = new TextFileSorter(new SortConfig().withMaxMemoryUsage(2 * 1000 * 1000));
    File tmpfile = File.createTempFile("OuputStreamCoverage.intersection", "");
    tmpfile.deleteOnExit();
    FileOutputStream tmp = new FileOutputStream(tmpfile);
    sorter.sort(seq, tmp);
    tmp.close();
    
    //
    // Now read sorted temp file and keep only cells which have a '+' equivalent
    //
    
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tmpfile)));
    
    String last = null;
    
    PrintStream ps = new PrintStream(out);
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        break;
      }
      
      if (null == last && !line.endsWith("+")) {
        last = line;
      } else if (null == last) {
        // Ignore orphaned '+' cells
        continue;
      } else if (line.startsWith(last) && line.endsWith("+") && line.length() == last.length() + 1) {
        ps.println(last);
        last = null;
        continue;
      } else if (line.endsWith("+")) {
        last = null;
      } else {
        last = line;
      }
    }

    br.close();
    ps.close();
    tmpfile.delete();    
  }
  
  public static void parse(String def, OutputStream out, int resolution) throws IOException {
    //
    // Split def on ' '
    //
    
    String[] defs = def.split(" ");
    
    //
    // Generate one file per area
    //

    List<File> files = new ArrayList<File>();
    
    for (int i = 0; i < defs.length; i++) {
      File file = File.createTempFile("OutputStreamCoverage.parse", "");
      file.deleteOnExit();
      files.add(file);
      
      //
      // Strip mode
      //
      
      String areadef = defs[i].substring(1);
      
      if (areadef.startsWith("circle:")) {
        OutputStreamCoverage c = new OutputStreamCoverage(new FileOutputStream(file));
        GeoParser.parseCircle(areadef.substring(7), resolution, c);
        c.close();
      } else if (def.startsWith("polygon:")) {
        OutputStreamCoverage c = new OutputStreamCoverage(new FileOutputStream(file));
        GeoParser.parsePolygon(areadef.substring(8), resolution, c);
        c.close();
      } else if (def.startsWith("rect:")) {
        OutputStreamCoverage c = new OutputStreamCoverage(new FileOutputStream(file));
        GeoParser.parseViewport(areadef.substring(5), resolution, c);
        c.close();
      } else if (def.startsWith("path:")) {
        OutputStreamCoverage c = new OutputStreamCoverage(new FileOutputStream(file));
        GeoParser.parsePath(areadef.substring(5), resolution, c);
        c.close();
      } else if (def.startsWith("polyline:")) {
        // Extract distance
        int idx = areadef.substring(9).indexOf(":");
        
        if (-1 == idx) {
          continue;
        }
        
        try {
          double dist = Double.valueOf(areadef.substring(9,idx));
          List<Long>[] hhcoords = GeoParser.parseEncodedPolyline(areadef.substring(9 + idx + 1));
          
          OutputStreamCoverage c = new OutputStreamCoverage(new FileOutputStream(file));
          for (int k = 0; k < hhcoords[0].size() - 1; k++) {
            HHCodeHelper.coverSegment(hhcoords[0].get(k), hhcoords[1].get(k), hhcoords[0].get(k+1), hhcoords[1].get(k + 1), dist, resolution, c);
          }
          c.close();
        } catch (NumberFormatException nfe) {
          continue;
        }
      }
    }
    
    //
    // Now combine coverages
    //
    
    // Find the first '+' coverage
    
    int i = 0;
    
    while (i < defs.length && !defs[i].startsWith("+")) {
      i++;
    }
    
    File first = files.get(i);
    i++;
    
    for (; i < defs.length; i++) {
      if (defs[i].startsWith("+")) {
        // Merge coverages
        File dest = File.createTempFile("OutputStreamCoverage.parse", "");
        dest.deleteOnExit();
        files.add(dest);
        merge(new FileInputStream(first), new FileInputStream(files.get(i)), new FileOutputStream(dest));
        first = dest;
      } else if (defs[i].startsWith("-")) {
        // Proceed with substraction
        File dest = File.createTempFile("OutputStreamCoverage.parse", "");
        dest.deleteOnExit();
        files.add(dest);
        minus(new FileInputStream(first), new FileInputStream(files.get(i)), new FileOutputStream(dest));
        first = dest;
      } else if (defs[i].startsWith("&")) {
        // Proceed with intersection
        File dest = File.createTempFile("OutputStreamCoverage.parse", "");
        dest.deleteOnExit();
        files.add(dest);
        intersection(new FileInputStream(first), new FileInputStream(files.get(i)), new FileOutputStream(dest));
        first = dest;
      }   
    }
    
    //
    // Now optimize result
    //

    for (int j = 0; j < 16; j++) {
      File second = File.createTempFile("OutputStreamCoverage.parse", "");
      second.deleteOnExit();
      files.add(second);
      optimize(new FileInputStream(first), new FileOutputStream(second), 0L, 0);
      if (first.length() == second.length()) {
        break;
      }
      first = second;
    }
    
    //
    // Copy 'second' to dest
    //
    
    byte[] buf = new byte[1024];
    
    InputStream in = new FileInputStream(first);
    
    while(true) {
      int len = in.read(buf);
      
      if (len < 0) {
        break;
      }
      
      out.write(buf, 0, len);
    }
    
    out.close();
    
    //
    // Delete intermediate files
    //
    
    for (File file: files) {
      file.delete();      
    }
  }
  
  public static void toKML(InputStream in, Writer writer) throws IOException {
    
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
    
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    
    while(true) {
      String line = br.readLine();
      
      if (null == line) {
        break;
      }

      int res = line.length() * 2;
      
      long cell = new BigInteger((line + "000000000000000").substring(0,16), 16).longValue();
      
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

    br.close();
    
    writer.append("</Document>\n");
    writer.append("</kml>\n");    
  }

  public void close() throws IOException {
    this.os.close();
  }
}
