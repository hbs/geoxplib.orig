package com.geoxp.geo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.geocoord.geo.OutputStreamCoverage;

public class Coverage {

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    int i = 0;
    
    boolean kml = false;
    String outfile = null;
    int res = 16;
    StringBuilder sb = new StringBuilder();
    
    while (i < args.length) {
      if ("--kml".equals(args[i]) || "-k".equals(args[i])) {
        kml = true;
      } else if ("--out".equals(args[i]) || "-o".equals(args[i])) {
        i++;
        outfile = args[i];
      } else if ("--res".equals(args[i]) || "-r".equals(args[i])) {
        i++;
        res = Integer.valueOf(args[i]);
      } else {
        if (sb.length() > 0) {
          sb.append(" ");
        }
        if (args[i].startsWith("@")) {
          FileInputStream in = new FileInputStream(new File(args[i].substring(1)));
          byte[] buf = new byte[1024];
          while(true) {
            int len = in.read(buf);
            if (len < 0) {
              in.close();
              break;
            }
            sb.append(new String(buf, 0, len));
          }
        } else {
          sb.append(args[i]);
        }
      }
      i++;
    }
    
    OutputStream out;
    File tmp = null;
    
    if (kml) {
      tmp = File.createTempFile("com.geoxp.geo.Coverage", "");
      tmp.deleteOnExit();
      out = new FileOutputStream(tmp);
    } else if (null != outfile) {
      out = new FileOutputStream(outfile);
    } else {
      out = System.out;
    }
    
    OutputStreamCoverage.parse(sb.toString() + " ", out, res);
    
    if (kml) {
      Writer writer;
      
      if (null == outfile) {
        writer = new OutputStreamWriter(System.out);
      } else {
        writer = new OutputStreamWriter(new FileOutputStream(outfile));
      }
      
      InputStream in = new FileInputStream(tmp);
      OutputStreamCoverage.toKML(in, writer);
      writer.close();
    }
  }
}
