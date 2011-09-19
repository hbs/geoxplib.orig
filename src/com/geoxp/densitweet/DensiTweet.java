package com.geoxp.densitweet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gwt.json.client.JSONObject;

import twitter4j.FilterQuery;
import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

public class DensiTweet extends Thread {

  private static LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>(10240);
  
  private static String HEATMAP_NAME = "com.twitter";
  private static String HEATMAP_SECRET = "com.geoxp.twitter";
  
  @Override
  public void run() {
    
    StringBuilder sb = new StringBuilder();
    int count = 0;
    
    int BATCH_SIZE = 1000;
    long BATCH_DELAY = 5000;
    
    long lastbatch = System.currentTimeMillis();
    
    HttpClient client = new DefaultHttpClient();

    while(true) {
      
      String point = null;
      
      try {
        point = queue.poll(5000L, TimeUnit.MILLISECONDS);
      } catch (InterruptedException ie) {        
      }
      
      if (null != point) {
        sb.append(point);
        sb.append("\n");
        count++;
      }
      
      if (null == point || count >= BATCH_SIZE || (System.currentTimeMillis() - lastbatch) > BATCH_DELAY) {
        
        try {
          
          HttpPost post = new HttpPost("http://localhost:8080/geoxp/update/" + HEATMAP_NAME);

          //
          // Add SECRET
          //
          
          sb.insert(0, "\n");
          sb.insert(0, HEATMAP_SECRET);
          sb.insert(0, "SECRET ");
          
          post.setEntity(new StringEntity(sb.toString()));
          
          HttpResponse resp = client.execute(post); 
          resp.getEntity().getContent().close();
        } catch (MalformedURLException mue) {          
        } catch (ProtocolException pe) {          
        } catch (IOException ioe) {          
        }
                
        //
        // Reset StringBuilder
        //
        
        count = 0;
        sb.setLength(0);
        lastbatch = System.currentTimeMillis();
      }
    }
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    
    DensiTweet t = new DensiTweet();
    t.setDaemon(true);
    t.start();
    
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(false)
      .setOAuthConsumerKey("Hg2aze4FXQ5zWyXoz6CQ")
      .setOAuthConsumerSecret("O8XsLLjHtNPgv1I1JtZeTxrKeiUPk8pI03hIsGuKDo")
      .setOAuthAccessToken("126988112-imW1brqjQHX5g1OSVHv7qGwVrM8nvFINinU2ufED")
      .setOAuthAccessTokenSecret("UXE1oJf8d5BJZ4dRlrwERee4kG9cdSsBXoliBtsuSnQ")
      .setJSONStoreEnabled(true);

    TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
    StatusListener listener = new StatusListener() {
      public void onStatus(Status status) {
        
        JsonObject json = new JsonParser().parse(DataObjectFactory.getRawJSON(status)).getAsJsonObject();
        
        GeoLocation geoloc = status.getGeoLocation();
        
        StringBuilder sb = new StringBuilder();
        
        if (null != geoloc) {
          sb.append(status.getCreatedAt().getTime());
          sb.append(":");
          sb.append(geoloc.getLatitude());
          sb.append(":");
          sb.append(geoloc.getLongitude());
          sb.append(":1:");
        } else {
          JsonElement place = json.get("place");
          if (null != place) {
            JsonElement bbox = place.getAsJsonObject().get("bounding_box");
            
            if (null != bbox) {
              JsonArray coordinates = bbox.getAsJsonObject().get("coordinates").getAsJsonArray().get(0).getAsJsonArray();
              
              // Compute centroid
              
              int count = coordinates.size();

              double lon = 0.0;
              double lat = 0.0;
              for (JsonElement elt: coordinates) {
                lon += elt.getAsJsonArray().get(0).getAsDouble();
                lat += elt.getAsJsonArray().get(1).getAsDouble();
              }
              lat /= count;
              lon /= count;
              
              sb.append(status.getCreatedAt().getTime());
              sb.append(":");
              sb.append(lat);
              sb.append(":");
              sb.append(lon);
              sb.append(":1:");
            }
          }
        }
          
        if (sb.length() > 0) { 
          queue.offer(sb.toString());          
        }
      }

      public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {} 
      public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}
      public void onScrubGeo(long userId, long upToStatusId) {}
      public void onException(Exception ex) { ex.printStackTrace(); }
    };
        
    twitterStream.addListener(listener);
    
    FilterQuery query = new FilterQuery();
    
    double[][] coords = new double[2][2];
    coords[0][0] = -180.0;
    coords[0][1] = -90.0;
    coords[1][0] = 180.0;
    coords[1][1] = 90.0;
    
    query.locations(coords);
    
    twitterStream.filter(query);
  }

}
