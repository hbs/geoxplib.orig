package com.geocoord.server.dao;

import java.sql.Connection;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;

/*
 * DataSource MUST be defined in the container. In the Context element defining
 * the application add the following:
 * 
 *         <Resource name="jdbc/GeoCoord"
 *                         auth="Container"
 *                         type="javax.sql.DataSource"
 *                         factory="org.apache.tomcat.dbcp.dbcp.BasicDataSourceFactory"
 *                         maxActive="100"
 *                         maxIdle="8"
 *                         maxWait="10000"
 *                         username="XXXX"
 *                         password="YYYY"
 *                         driverClassName="com.mysql.jdbc.Driver"
 *                         url="jdbc:mysql://127.0.0.1:3306/geocoord?autoReconnect=true" />
 *
 * 
 * In the webapp web.xml add:
 * 
 * 		 <resource-ref>
 *       <description>GeoCoord DB Connection</description>
 *       <res-ref-name>jdbc/geocoord</res-ref-name>
 *       <res-type>javax.sql.DataSource</res-type>
 *       <res-auth>Container</res-auth>
 *     </resource-ref>
 */

//TODO(hbs): implement sharding so we can query get("UNIVERSE")
//and retrieve a Connection for the given universe
//Both Mutex and dbconn must hold maps.

public class DB {

  private static final String JNDI_NAME = "java:comp/env/jdbc/geocoord";

  private static final Log logger = LogFactory.getLog(DB.class);

  /**
   * Thread local DB connection 
   */

  private static ThreadLocal<Connection> dbconn = new ThreadLocal<Connection> () {
    protected synchronized Connection initialValue () {
      return null;
    }
  };


  /**
   * Thread local connection mutex 
   */

  private static ThreadLocal<Boolean> dbmutex = new ThreadLocal<Boolean> () {protected synchronized Boolean initialValue () { return Boolean.FALSE; }};

  /**
   * Thread local flag enabling or disabling commit.
   */

  private static ThreadLocal<Boolean> dbcommit = new ThreadLocal<Boolean> () { protected synchronized Boolean initialValue() { return Boolean.TRUE; }};
  

  public static Connection hold () throws GeoCoordException {
    return hold(false);
  }
  
  /**
   * Get a database connection
   */

  private static Connection hold (boolean force) throws GeoCoordException {

    //
    // If dbmutex is true then bail out
    //

    if (!force && ((Boolean) dbmutex.get()).equals(Boolean.TRUE)) {
      throw new GeoCoordException (GeoCoordExceptionCode.DB_CONNECTION_ALREADY_HELD);
    }

    //
    // ThreadLocal value is null, retrieve a connection from the pool
    //

    if (dbconn.get() == null) {
      //
      // Get DB connection
      //

      try {
        Context ctx = new InitialContext ();

        if (ctx == null) {
          throw new GeoCoordException (GeoCoordExceptionCode.DB_CONTEXT);
        }

        DataSource ds = (DataSource) ctx.lookup (JNDI_NAME);

        if (ds == null) {
          throw new GeoCoordException (GeoCoordExceptionCode.DB_DATASOURCE);
        }

        Connection conn = ds.getConnection();

        if (conn == null) {
          throw new GeoCoordException (GeoCoordExceptionCode.DB_CONNECTION);
        }

        conn.setAutoCommit (false);
        // Change MySQL transaction isolation level, this defaults to REPEATABLE_READ
        // for InnoDB which is not what we want.
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        conn.rollback ();

        //
        // Set it as value for the ThreadLocal.
        //

        dbconn.set(conn);
      } catch (Exception e) {
        logger.error("hold", e);
        e.printStackTrace();
        throw new GeoCoordException (GeoCoordExceptionCode.DB_CONNECTION);
      }                       
    }

    //
    // Set dbmutex value to true
    //

    dbmutex.set(Boolean.TRUE);

    //
    // Return the DB Connection
    //

    return dbconn.get();               
  }

  /**
   * Release a database connection
   */

  public static void release () throws GeoCoordException {
    //
    // Set dbmutex to false
    //

    if (dbconn.get() == null) {
      throw new GeoCoordException (GeoCoordExceptionCode.DB_NO_CONNECTION_HELD);
    }

    if (((Boolean)dbmutex.get()).equals(Boolean.FALSE)) {
      throw new GeoCoordException (GeoCoordExceptionCode.DB_CONNECTION_ALREADY_RELEASED);
    }

    dbmutex.set(Boolean.FALSE);
  }

  /**
   * Recycle a database connection (when it is no longer needed)
   */

  public static void recycle () throws GeoCoordException {

    if (((Boolean) dbmutex.get()).equals(Boolean.TRUE)) {
      throw new GeoCoordException (GeoCoordExceptionCode.DB_CONNECTION_HELD);              
    }

    if (dbconn.get() != null) {
      Connection conn = (Connection) dbconn.get();

      try {
        if (conn != null) conn.rollback();
        if (conn != null) conn.close();                         
      } catch (Exception e) {
        logger.error("recycle", e);
      }

      //
      // Set the ThreadLocal value to null
      //
      dbconn.set(null);
    }

    //
    // Set dbmutex to false
    //

    dbmutex.set(Boolean.FALSE);
  }

  public static void enableCommit(boolean flag) {
    dbcommit.set(flag); // autoboxing...
  }

  public static boolean isCommitEnabled() {
    return dbcommit.get(); // autoboxing...
  }
  
  public static void commit () throws GeoCoordException {
    
    // Check if dbcommit is false, if so return immediately
    if (!isCommitEnabled()) { // autoboxing...
      // Log the event as i might be done by mistake.
      /*
      Throwable t = new Throwable();
      t.fillInStackTrace();
      logger.info("Commit called when enableCommit is false.", t);
      */
      return;
    }
    
    Connection dbconn = DB.hold();

    try {
      dbconn.commit ();
    } catch (Exception e) {
      logger.error("commit", e);
      throw new GeoCoordException (GeoCoordExceptionCode.DB_COMMIT);
    } finally {
      DB.release ();
    }
  }

  public static void rollback () throws GeoCoordException {
    // Rollbacking is important, so ignore the case when the connection is already held
    // as we might arrive here after an exception was thrown and we MUST issue the rollback.
    Connection dbconn = DB.hold(true);

    try {
      dbconn.rollback ();
    } catch (Exception e) {
      logger.error("rollback", e);
      throw new GeoCoordException (GeoCoordExceptionCode.DB_ROLLBACK);
    } finally {
      DB.release ();
    }
  }
}
