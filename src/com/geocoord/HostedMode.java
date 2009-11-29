package com.geocoord;

import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSourceFactory;

import org.springframework.mock.jndi.SimpleNamingContextBuilder;

public class HostedMode {
  public static void main(String[] args) {
    //
    // Create Mock JNDI context
    //
    
    try {
      System.setProperty("java.naming.factory.initial", SimpleNamingContextBuilder.class.getName());
      SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
      Properties props = new Properties();
      props.put("url", "jdbc:mysql://127.0.0.1:3306/geocoord?autoReconnect=false&amp;gatherPerfMetrics=true&amp;logSlowQueries=true&amp;slowQueryThresholdMillis=100&amp;useNanosForElapsedTime=true&amp;characterEncoding=utf8&amp;characterSetResults=utf8");
      props.put("username", "geocoord");
      props.put("password", "geocoord");
      props.put("driverClassName", "com.mysql.jdbc.Driver"); 
      builder.bind("java:comp/env/jdbc/geocoord", BasicDataSourceFactory.createDataSource(props));
      builder.activate();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }

    // Call original HostedMode
    com.google.gwt.dev.HostedMode.main(args);
  }
}
