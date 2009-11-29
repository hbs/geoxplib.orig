package com.geocoord.server.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.apache.thrift.TException;
import org.bouncycastle.util.encoders.Hex;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.User;
import com.geocoord.thrift.services.UserService;
import com.geocoord.util.CryptoUtil;
import com.geocoord.util.ThriftUtil;

public class UserDAOImpl implements UserService.Iface {
  
  private User loadByTwitterId(String key) throws GeoCoordException {

    if (!key.startsWith("twitter:")) {
      throw new GeoCoordException(GeoCoordExceptionCode.USER_INVALID_TWITTER_ID);
    }

    Connection dbconn = null;
    Statement stmt = null;
    ResultSet rs = null;
    boolean commitEnabled = DB.isCommitEnabled();
    boolean needRollback = false;
    
    try {  
      //
      // Look for the user by issueing a JOIN (beurk!) with the 'userrefs' table
      //
      
      StringBuilder sql = new StringBuilder();
      sql.append("/*@ SQL-003 */ SELECT users.gcuid,users.thrift FROM users,userrefs WHERE users.gcuid = userrefs.gcuid AND userrefs.gcuref = ");
      sql.append(CryptoUtil.FNV1a64(key));
      sql.append("/* SQL-003 */");
      
      dbconn = DB.hold();

      stmt = dbconn.createStatement();
            
      rs = stmt.executeQuery(sql.toString());
      
      User user = null;
      
      if (rs.next()) {
        user = (User) ThriftUtil.deserialize(User.class, rs.getBytes("users.thrift"));
      }
      
      rs.close();
      rs = null;
      
      stmt.close();
      stmt = null;
      
      DB.release();
      dbconn = null;
      
      return user;
    } catch (SQLException sqle) {
      throw new GeoCoordException(GeoCoordExceptionCode.SQL_ERROR);
    } finally {
      if (needRollback) try { DB.rollback(); } catch (GeoCoordException gce) {}
      if (null != rs) try { rs.close(); } catch (SQLException sqle) {}
      if (null != stmt) try { stmt.close(); } catch (SQLException sqle) {}
      if (null != dbconn) try { DB.release(); } catch (GeoCoordException gce) {}
      DB.enableCommit(commitEnabled);
    }
    
  }
  
  private User loadByGcuid(String key) throws GeoCoordException {
    
    if (!key.startsWith("gcuid:")) {
      throw new GeoCoordException(GeoCoordExceptionCode.USER_INVALID_GCUID);
    }
    
    Connection dbconn = null;
    Statement stmt = null;
    ResultSet rs = null;
    boolean commitEnabled = DB.isCommitEnabled();
    boolean needRollback = false;
    
    try {  
      //
      // Look for the user by issueing a JOIN (beurk!) with the 'userrefs' table
      //
      
      StringBuilder sql = new StringBuilder();
      sql.append("/*@ SQL-004 */ SELECT users.gcuid,users.thrift FROM users WHERE users.gcuid = ");
      sql.append(CryptoUtil.FNV1a64(key.substring(6)));
      sql.append("/* SQL-004 */");
      
      dbconn = DB.hold();

      stmt = dbconn.createStatement();
            
      rs = stmt.executeQuery(sql.toString());
      
      User user = null;
      
      if (rs.next()) {
        user = (User) ThriftUtil.deserialize(User.class, rs.getBytes("users.thrift"));
      }
      
      rs.close();
      rs = null;
      
      stmt.close();
      stmt = null;
      
      DB.release();
      dbconn = null;
      
      return user;
    } catch (SQLException sqle) {
      throw new GeoCoordException(GeoCoordExceptionCode.SQL_ERROR);
    } finally {
      if (needRollback) try { DB.rollback(); } catch (GeoCoordException gce) {}
      if (null != rs) try { rs.close(); } catch (SQLException sqle) {}
      if (null != stmt) try { stmt.close(); } catch (SQLException sqle) {}
      if (null != dbconn) try { DB.release(); } catch (GeoCoordException gce) {}
      DB.enableCommit(commitEnabled);
    }

  }
  public User load(String key) throws GeoCoordException, TException {  
    if (key.startsWith("gcuid:")) {
      return loadByGcuid(key);
    } else if (key.startsWith("twitter:")) {
      return loadByTwitterId(key);
    } else {
      throw new GeoCoordException(GeoCoordExceptionCode.USER_INVALID_ID_TYPE);
    }    
  }

  public User store(User user) throws GeoCoordException, TException {
    
    Connection dbconn = null;
    Statement stmt = null;
    boolean commitEnabled = DB.isCommitEnabled();
    boolean needRollback = false;
  
    try {
      //
      // If the user has no gcuid, generate one
      //
    
      boolean newuser = false;
    
      if (!user.isSetGcuid()) {
        newuser = true;
        user.setGcuid(UUID.randomUUID().toString());
        
        // Generate a HMAC Key
        user.setHmacKey(new byte[32]);
        CryptoUtil.getSecureRandom().nextBytes(user.getHmacKey());
      }
    
      StringBuilder sql = new StringBuilder();
      
      if (newuser) {
        sql.append("/*@ SQL-001 */ INSERT INTO users(gcuid,thrift) VALUES(");
        sql.append(CryptoUtil.FNV1a64(user.getGcuid()));
        sql.append(",x'");
        sql.append(ThriftUtil.serializeHex(user));
        sql.append("')");
      } else {
        sql.append("/*@ SQL-002 */ UPDATE users SET thrift=x'");
        sql.append(ThriftUtil.serializeHex(user));
        sql.append("'");
        sql.append(" WHERE gcuid=");
        sql.append(CryptoUtil.FNV1a64(user.getGcuid()));
      }
    
      dbconn = DB.hold();

      stmt = dbconn.createStatement();
      
      stmt.executeUpdate(sql.toString());
      
      //
      // Insert references (Twitter, Google, fb, ...)
      //
      
      if (newuser) {
        sql.setLength(0);
        sql.append("/*@ SQL-002 */ INSERT INTO userrefs(gcuref,gcuid) VALUES(");
        if (user.isSetTwitterId()) {
          sql.append(CryptoUtil.FNV1a64("twitter:" + user.getTwitterId()));
        }
        sql.append(",");
        sql.append(CryptoUtil.FNV1a64(user.getGcuid()));
        sql.append(")");
        
        stmt.executeUpdate(sql.toString());
      }
      
      stmt.close();
      stmt = null;
      
      DB.release();
      dbconn = null;
      
      DB.commit();
      
      return user;
    } catch (SQLException sqle) {
      sqle.printStackTrace();
      needRollback = true;
      throw new GeoCoordException(GeoCoordExceptionCode.SQL_ERROR);
    } finally {
      if (needRollback) try { DB.rollback(); } catch (GeoCoordException gce) {}
      if (null != stmt) try { stmt.close(); } catch (SQLException sqle) {}
      if (null != dbconn) try { DB.release(); } catch (GeoCoordException gce) {}
      DB.enableCommit(commitEnabled);
    }
  }
}
