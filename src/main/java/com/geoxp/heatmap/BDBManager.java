package com.geoxp.heatmap;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class BDBManager {
  
  private static ConcurrentHashMap<String, Database> dbs;
  
  private static ConcurrentHashMap<String, Environment> envs;

  private static EnvironmentConfig envconfig;
  
  public static final String GEOXP_DB_HOME = "geoxp.db.home";
  
  private static final String DB_HOME = null != System.getProperty(GEOXP_DB_HOME) ? System.getProperty(GEOXP_DB_HOME) : "/var/tmp/GEOXP_DB_HOME";
  
  static {
    
    envconfig = new EnvironmentConfig();
    envconfig.setTransactional(false);
    envconfig.setCachePercent(60);
    envconfig.setSharedCache(true);
    envconfig.setAllowCreate(true);
    envconfig.setConfigParam(EnvironmentConfig.LOG_FILE_MAX, "40000000");

    envs = new ConcurrentHashMap<String, Environment>();
    dbs = new ConcurrentHashMap<String, Database>();
    
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        System.out.println("Closing BDB environment.");
        BDBManager.closeAll();
      }
    }));        
  }
  
  // FIXME(hbs): we need to check if we can have that synchronized or if it blocks for
  //             too long when opening DBs.
  public static synchronized Database getDB(String dbname) {
    
    //
    // If db is opened, return it
    //
    
    Database db = dbs.get(dbname);
    
    if (null != db) {
      return db;
    }

    //
    // Attempt to retrieve environment
    //
    
    Environment env = envs.get(dbname);

    if (null == env) {
      File home = new File(DB_HOME, dbname);
      
      if (!home.exists()) {
        home.mkdirs();
      }

      env = new Environment(home, envconfig);

      envs.put(dbname, env);
    }
    
    //
    // Attempt to open db
    //

    
    DatabaseConfig dbconfig = new DatabaseConfig();
    dbconfig.setAllowCreate(true);
    dbconfig.setDeferredWrite(true);
    dbconfig.setCacheMode(CacheMode.DEFAULT);
    dbconfig.setTransactional(false);
               
    db = env.openDatabase(null, dbname, dbconfig);
    
    System.out.println("Opened Database " + dbname + " with " + db.count() + " records.");
    
    dbs.put(dbname, db);
    
    return db;
  }
  
  public static synchronized void closeAll() {
    System.out.println(envs);
    System.out.println(dbs);
    
    for (String dbname: envs.keySet()) {
      Database db = dbs.get(dbname);
      
      if (null != db) {
        System.out.println("Closing Database " + dbname + " with " + db.count() + " records.");
        db.close();
      }
      
      Environment env = envs.get(dbname);
      
      if (null != env) {
        System.out.println("Cleaning logs for Environment " + dbname);
        env.cleanLog();
        System.out.println("Closing Environment " + dbname);
        env.close();
      }
    }
  }
}
