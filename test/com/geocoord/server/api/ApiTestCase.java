package com.geocoord.server.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import com.geocoord.server.dao.DB;
import com.geocoord.server.servlet.GuiceBootstrap;

import junit.framework.TestCase;

abstract public class ApiTestCase extends TestCase {

	private static boolean done;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		if (!done) {
			init();
			done = true;
		}
	}
	
	/**
	 * Initialize datasource, empty table and inject services.
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception { 
		new GuiceBootstrap().init();
			
		System.setProperty("java.naming.factory.initial", SimpleNamingContextBuilder.class.getName());
		SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
		Properties props = new Properties();
		props.put("url", "jdbc:mysql://127.0.0.1:3306/geocoordtests?autoReconnect=false&amp;gatherPerfMetrics=true&amp;logSlowQueries=true&amp;slowQueryThresholdMillis=100&amp;useNanosForElapsedTime=true&amp;characterEncoding=utf8&amp;characterSetResults=utf8");
		props.put("username", "geocoord");
		props.put("password", "geocoord");
		props.put("driverClassName", "com.mysql.jdbc.Driver"); 
		builder.bind("java:comp/env/jdbc/geocoord", BasicDataSourceFactory.createDataSource(props));
		builder.activate();

//	       	Context ctx = new InitialContext ();
//	        DataSource ds = (DataSource) ctx.lookup ("java:comp/env/jdbc/geocoord");
//	        Connection conn = ds.getConnection();
//
//	        int errors = ij.runScript(
//				conn,
//				new FileInputStream("etc/GeoCoord-derby.sql"), "utf-8", System.out, "utf-8");
//	        
//	        conn.close();
			

		//
		// empty all tables
		//

		Connection conn = null;
		Statement stmt = null;
	
		try {
			conn = DB.hold();
			stmt = conn.createStatement();
			stmt.execute("TRUNCATE TABLE users");
			stmt.execute("TRUNCATE TABLE userrefs");
			stmt.execute("TRUNCATE TABLE layers");
		} finally {
			if (null != stmt) { try {stmt.close(); } catch(SQLException sqle) {} }
			if (null != conn) { DB.release(); }
		}
	}

}
