//
//  GeoXP Lib, library for efficient geo data manipulation
//
//  Copyright (C) 1999-2016  Mathias Herberts
//
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Affero General Public License as
//  published by the Free Software Foundation, either version 3 of the
//  License, or (at your option) any later version and under the terms
//  of the GeoXP License Exception.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

package com.geoxp.densitweet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
  
  private static String HEATMAP_NAME = "";
  private static String HEATMAP_SECRET = "";
  
  /**
   * When to add 'EXPIRE' to call to GeoXP, by default every 10 minutes
   */
  private static long EXPIRE_EVERY = 600000;
  
  /**
   * File to store tweets in
   */
  private static OutputStream FILE_STORE = null;
  
  @Override
  public void run() {
    
    StringBuilder sb = new StringBuilder();
    int count = 0;
    
    int BATCH_SIZE = 1000;
    long BATCH_DELAY = 5000;
    
    long lastbatch = System.currentTimeMillis();
    
    HttpClient client = new DefaultHttpClient();

    long lastexpire = 0;
    
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
          // Add expire
          //
          
          if (System.currentTimeMillis() - lastexpire > EXPIRE_EVERY) {
            sb.insert(0,"EXPIRE 0\n");
            lastexpire = System.currentTimeMillis();
          }
          
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
  public static void main(String[] args) throws Exception {
    
    DensiTweet t = new DensiTweet();
    t.setDaemon(true);
    t.start();
    
    String oauthConsumerKey = System.getProperty("oauth.consumer.key"); // Hg2aze4FXQ5zWyXoz6CQ
    String oauthConsumerSecret = System.getProperty("oauth.consumer.secret"); // O8XsLLjHtNPgv1I1JtZeTxrKeiUPk8pI03hIsGuKDo
    String oauthAccessToken = System.getProperty("oauth.access.token"); // 126988112-imW1brqjQHX5g1OSVHv7qGwVrM8nvFINinU2ufED
    String oauthAccessTokenSecret = System.getProperty("oauth.access.token.secret"); // UXE1oJf8d5BJZ4dRlrwERee4kG9cdSsBXoliBtsuSnQ
    
    ConfigurationBuilder cb = new ConfigurationBuilder();
    cb.setDebugEnabled(false)
      .setOAuthConsumerKey(oauthConsumerKey)
      .setOAuthConsumerSecret(oauthConsumerSecret)
      .setOAuthAccessToken(oauthAccessToken)
      .setOAuthAccessTokenSecret(oauthAccessTokenSecret)
      .setJSONStoreEnabled(true);
    
    FilterQuery query = new FilterQuery();
    
    boolean intrack = false;
    boolean inloc = false;
    boolean infollow = false;
    boolean inheatmap = false;
    boolean inheatmapsecret = false;
    boolean log = false;
    boolean inexpire = false;
    boolean instore = false;
    
    Set<String> track = new HashSet<String>();
    Set<Long> follow = new HashSet<Long>();
    List<Double> locations = new ArrayList<Double>();
    
    int i = 0;
    
    while (i < args.length) {      
      if ("--track".equals(args[i])) {
        intrack = true;
      } else if ("--expire".equals(args[i])) {
        inexpire = true;
      } else if ("--store".equals(args[i])) {
        instore = true;
      } else if ("--follow".equals(args[i])) {
        infollow = true;
      } else if ("--locations".equals(args[i])) {
        inloc = true;
      } else if ("--log".equals(args[i])) {
        log = true;
      } else if ("--heatmap".equals(args[i])) {
        inheatmap = true;
      } else if ("--heatmapsecret".equals(args[i])) {
        inheatmapsecret = true;
      } else if (instore) {
        instore = false;
        FILE_STORE = new FileOutputStream(args[i], true);
      } else if (inexpire) {
        inexpire = false;
        EXPIRE_EVERY = Long.valueOf(args[i]);
      } else if (inheatmap) {
        inheatmap = false;
        HEATMAP_NAME = args[i];
      } else if (inheatmapsecret) {
        inheatmapsecret = false;
        HEATMAP_SECRET = args[i];
      } else if (intrack) {
        intrack = false;
        
        // Split args on commas
        String[] tokens = args[i].split(",");
        
        for (String token: tokens) {
          track.add(token);
        }
      } else if (infollow) {
        infollow = false;
        // Split args on commas
        String[] tokens = args[i].split(",");
        
        for (String token: tokens) {
          follow.add(Long.valueOf(token));
        }        
      } else if (inloc) {
        inloc = false;
        // Split on ','
        String[] tokens = args[i].split(",");
        
        for (String bbox: tokens) {
          // Split on ':'
          String[] coords = bbox.split(":");
          
          for (String coord: coords) {
            locations.add(Double.valueOf(coord));
          }          
        }
      }
      
      i++;
    }

    if (track.size() > 0) {
      query.track(track.toArray(new String[0]));
    }
    
    if (follow.size() > 0) {
      long[] f = new long[follow.size()];
      int k = 0;
      for (long l: follow) {
        f[k++] = l;
      }
      query.follow(f);
    }
    
    if (locations.size() > 0) {
      double[][] coords = new double[locations.size() >> 1][2];
      
      for (int j = 0; j < coords.length; j++) {
        coords[j][0] = locations.get(j * 2);
        coords[j][1] = locations.get(j * 2  + 1);
      }
      
      query.locations(coords);
    }

    final boolean LOG = log;
    
    TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
    StatusListener listener = new StatusListener() {
      public void onStatus(Status status) {
        
        JsonObject json = new JsonParser().parse(DataObjectFactory.getRawJSON(status)).getAsJsonObject();
    
        if (LOG) {
          System.out.println(json);
        }
        
        if (null != FILE_STORE) {
          try {
            FILE_STORE.write(json.toString().getBytes("UTF-8"));
            FILE_STORE.write('\n');
          } catch (IOException ioe) {            
          }
        }
        
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
          if (null != place && place.isJsonObject()) {
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

    System.out.println(query);
    twitterStream.filter(query);    
  }

}
