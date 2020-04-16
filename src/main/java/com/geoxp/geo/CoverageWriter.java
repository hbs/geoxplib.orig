//
//   GeoXP Lib, library for efficient geo data manipulation
//
//   Copyright 2020-      SenX S.A.S.
//   Copyright 2019-2020  iroise.net S.A.S.
//   Copyright 1999-2019  Mathias Herberts
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package com.geoxp.geo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;


public class CoverageWriter {

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
