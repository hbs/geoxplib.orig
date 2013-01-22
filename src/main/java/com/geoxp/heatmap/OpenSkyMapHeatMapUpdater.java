//package com.geoxp.heatmap;
//
//import java.io.IOException;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Map;
//
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.util.EntityUtils;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParseException;
//import com.google.gson.JsonParser;
//
//public class OpenSkyMapHeatMapUpdater implements Runnable {
//  
//  private static final long DELAY = 15000L;
//
//  private final HeatMapManager managerLow;
//  private final HeatMapManager managerHigh;
//  
//  private static final Collection<Integer> LOW_ALT_RESOLUTIONS = new HashSet<Integer>() {{ add(14); add(16); add(18); add(20); }};
//  private static final Collection<Integer> HIGH_ALT_RESOLUTIONS = new HashSet<Integer>() {{ add(4); add(6); add(8); add(10); add(12); add(14); }};
//
//  public OpenSkyMapHeatMapUpdater() {
//    //
//    // Create configuration
//    //
//    
//    //
//    // Create HeatMaps
//    //
//    
//    Map<Long,Integer> buckets = new HashMap<Long, Integer>();
//    // 5 buckets of 1'
//    buckets.put(60000L, 5);
//    // 6 buckets of 4 hours
//    buckets.put(4*3600*1000L, 6);
//    
//    
//    // Limit # of buckets to 100M
//    // When used buckets go over 98M, fast expire cells not updated within the last 12 hours
//    // When used buckets fall below 97M, stop fast expire.
//
//    /*
//    managerLow = new BDBHeatMapManager("com.openskymap.low", buckets, 4, 16);
//    ((BDBHeatMapManager) managerLow).configureLimits(100000000L, 98000000, 97000000, 12 * 3600000L);
//
//    managerHigh = new BDBHeatMapManager("com.openskymap.high", buckets, 4, 16);
//    ((BDBHeatMapManager) managerHigh).configureLimits(100000000L, 98000000, 97000000, 12 * 3600000L);
//    */
//
//    managerLow = new MemoryHeatMapManager(buckets, 4, 20);
//    ((MemoryHeatMapManager) managerLow).configureLimits(100000000L, 98000000, 97000000, 12 * 3600000L);
//
//    managerHigh = null;
//    
//    TileBuilderRegistry.register("com.openskymap", new HeatMapManagerTileBuilder(managerLow, 4));
//    //TileBuilderRegistry.register("com.openskymap.high", new HeatMapManagerTileBuilder(managerHigh, 4));
//    
//    Thread t = new Thread(this);
//    t.setDaemon(true);
//    t.start();
//  }
//  
//  @Override
//  public void run() {
//    //
//    // Periodically retrieve OpenSkyMap data
//    //
//        
//    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//    HttpClient httpclient = new DefaultHttpClient();
//
//    while (true) {
//      
//      long now = System.currentTimeMillis();
//      
//      try {
//        HttpGet httpget = new HttpGet("http://aurora.openskymap.com/services/srvjsonp.php");
//
//        HttpResponse response = httpclient.execute(httpget);
//        HttpEntity entity = response.getEntity();
//
//        if (null != entity) {
//          String body = EntityUtils.toString(entity);
//          
//          int count = 0;
//          
//          try {
//            JsonObject json = new JsonParser().parse(body).getAsJsonObject();
//            
//            JsonArray flights = json.get("Vols").getAsJsonArray();
//                      
//            Iterator<JsonElement> iter = flights.iterator();
//            
//            while (iter.hasNext()) {
//              JsonElement flight = iter.next().getAsJsonObject().get("unVol").getAsJsonArray().get(0);
//
//              double lat = flight.getAsJsonObject().get("Vlt").getAsDouble();
//              double lon = flight.getAsJsonObject().get("Vlg").getAsDouble();
//              double alt = flight.getAsJsonObject().get("Val").getAsDouble();
//              String time = flight.getAsJsonObject().get("Vdt").getAsString();
//
//              try {
//                long timestamp = sdf.parse(time).getTime();
//                
//                if (alt < 1000) {
//                  managerLow.store(lat, lon, timestamp, 1, true, LOW_ALT_RESOLUTIONS);              
//                } else {
//                  managerLow.store(lat, lon, timestamp, 1, true, HIGH_ALT_RESOLUTIONS);
//                }
//                count++;
//              } catch (ParseException pe) {              
//              }
//            }            
//          } catch (JsonParseException jpe) {            
//          } finally {
//            System.out.println("Stored " + count + " ACFT positions, current bucket count: " + managerLow.getBucketCount());
//          }
//        }        
//      } catch (IOException ioe) {
//      }
//      
//      while (System.currentTimeMillis() < now + DELAY) {
//        try {
//          Thread.sleep(Math.min(now + DELAY - System.currentTimeMillis(), 1000));
//        } catch (InterruptedException ie) {        
//        }
//      }
//    }
//  }
//  
//  public static void main(String[] args) throws Exception {
//    OpenSkyMapHeatMapUpdater osmhmu = new OpenSkyMapHeatMapUpdater();
//    
//    while (true) {
//      Thread.sleep(1000);
//    }
//  }
//
//}
