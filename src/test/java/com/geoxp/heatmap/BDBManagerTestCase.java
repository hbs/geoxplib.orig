package com.geoxp.heatmap;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;

public class BDBManagerTestCase {
  
  @BeforeClass
  public static void setUp() throws Exception {
    System.setProperty(BDBManager.GEOXP_DB_HOME, "/var/tmp/BDBManagerTestCase.temp" + Thread.currentThread().getId());   
  }
  
  @Test
  public void testMultipleDB() {
    
    Database db1 = BDBManager.getDB("db1");
    Database db2 = BDBManager.getDB("db2");
    
    db1.put(null, new DatabaseEntry("db1".getBytes()), new DatabaseEntry("db1".getBytes()));
    db2.put(null, new DatabaseEntry("db2".getBytes()), new DatabaseEntry("db2".getBytes()));
    
    BDBManager.closeDB("db1");
    BDBManager.closeDB("db2");
    
    db1 = BDBManager.getDB("db1");
    DatabaseEntry key = new DatabaseEntry("db1".getBytes()); 
    DatabaseEntry data = new DatabaseEntry();
    db1.get(null, key, data, LockMode.READ_COMMITTED);
    Assert.assertArrayEquals(key.getData(), data.getData());

    db2 = BDBManager.getDB("db2");
    key = new DatabaseEntry("db2".getBytes()); 
    data = new DatabaseEntry();
    db2.get(null, key, data, LockMode.READ_COMMITTED);
    Assert.assertArrayEquals(key.getData(), data.getData());

    BDBManager.closeDB("db1");
    BDBManager.closeDB("db2");    
  }
  
  @AfterClass
  public static void cleanUp() throws Exception {
    BDBManager.closeAll();
    File tmp = new File(System.getProperty(BDBManager.GEOXP_DB_HOME));
    tmp.delete();
  }
}
