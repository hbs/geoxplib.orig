package com.geoxp.heatmap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class OpenSkyMapUpdater {

  private static long DELAY = 15000;
  
  private static int LOW_HIGH_LIMIT = 3000;
  
  private static final String LOW_ALT_RESOLUTIONS = "14,16,18,20";
  private static final String HIGH_ALT_RESOLUTIONS = "4,6,8,10,12,14";

  private static final String HEATMAP_NAME = "com.openskymap";
  private static final String HEATMAP_SECRET = "com.geoxp.openskymap";
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    
    long lastexpire = 0;
    
    while(true) {
      //
      // Periodically retrieve OpenSkyMap data
      //
          
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  
      StringBuilder sb = new StringBuilder();

      HttpClient client = new DefaultHttpClient();
      
      while (true) {
        
        long now = System.currentTimeMillis();
        
        sb.setLength(0);
        sb.append("SECRET ");
        sb.append(HEATMAP_SECRET);
        sb.append("\n");
    
        if (System.currentTimeMillis() - lastexpire > 3600000) {
          sb.append("EXPIRE 0\n");
          lastexpire = System.currentTimeMillis();
        }
        
        try {
          HttpGet httpget = new HttpGet("http://aurora.openskymap.com/services/srvjsonp.php");

          HttpResponse response = client.execute(httpget);
          HttpEntity entity = response.getEntity();
          
          if (null != entity) {
            String body = EntityUtils.toString(entity);
            
            int count = 0;
            
            try {
              JsonObject json = new JsonParser().parse(body).getAsJsonObject();
              
              JsonArray flights = json.get("Vols").getAsJsonArray();
                        
              Iterator<JsonElement> iter = flights.iterator();
                                 
              while (iter.hasNext()) {
                JsonElement flight = iter.next().getAsJsonObject().get("unVol").getAsJsonArray().get(0);

                double lat = flight.getAsJsonObject().get("Vlt").getAsDouble();
                double lon = flight.getAsJsonObject().get("Vlg").getAsDouble();
                double alt = flight.getAsJsonObject().get("Val").getAsDouble();
                String time = flight.getAsJsonObject().get("Vdt").getAsString();
                
                try {
                  long timestamp = sdf.parse(time).getTime();
                  
                  sb.append(timestamp);
                  sb.append(":");
                  sb.append(lat);
                  sb.append(":");
                  sb.append(lon);
                  sb.append(":");
                  sb.append("1");
                  sb.append(":");
                  
                  if (alt < LOW_HIGH_LIMIT) {
                    sb.append(LOW_ALT_RESOLUTIONS);              
                  } else {
                    sb.append(HIGH_ALT_RESOLUTIONS);
                  }
                  
                  sb.append("\n");
                  
                  count++;
                } catch (ParseException pe) {              
                }
              }
              
              //entity.getContent().close();
              
              try {
                HttpPost post = new HttpPost("http://localhost:8080/geoxp/update/" + HEATMAP_NAME);

                post.setEntity(new StringEntity(sb.toString()));
                
                HttpResponse resp = client.execute(post);
                resp.getEntity().getContent().close();
              } catch (MalformedURLException mue) {          
              } catch (ProtocolException pe) {          
              } catch (IOException ioe) {          
              }

            } catch (JsonParseException jpe) {            
            } finally {
            }
          }        
        } catch (IOException ioe) {
        }
        
        while (System.currentTimeMillis() < now + DELAY) {
          try {
            Thread.sleep(Math.min(now + DELAY - System.currentTimeMillis(), 1000));
          } catch (InterruptedException ie) {        
          }
        }
      }      
    }
  }

}
