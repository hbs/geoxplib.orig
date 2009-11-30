package com.geocoord.server.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.apache.thrift.TException;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.thrift.data.Layer;
import com.geocoord.thrift.data.LayerAdminRequest;
import com.geocoord.thrift.data.LayerAdminRequestType;
import com.geocoord.thrift.data.LayerAdminResponse;
import com.geocoord.thrift.services.LayerService;
import com.geocoord.util.CryptoUtil;
import com.geocoord.util.ThriftUtil;

public class LayerServiceImpl implements LayerService.Iface {
  
  public LayerAdminResponse admin(LayerAdminRequest request) throws GeoCoordException, TException {
    if (LayerAdminRequestType.CREATE.equals(request.getType())) {
      return doLayerCreate(request);
    } else if (LayerAdminRequestType.COUNT.equals(request.getType())) {
      return doCount(request);
    }
    return null;
  }
  
  /**
   * Perform a layer creation.
   * 
   * @param request
   * @return
   * @throws GeoCoordException
   */
  private LayerAdminResponse doLayerCreate(LayerAdminRequest request) throws GeoCoordException {
    Connection dbconn = null;
    Statement stmt = null;
    boolean commitEnabled = DB.isCommitEnabled();
    boolean needRollback = false;
    
    try {  
      //
      // Generate gclid for layer
      //
      
      String gclid = UUID.randomUUID().toString();
      
      //
      // Create layer
      //
      
      Layer layer = new Layer();
      layer.setGclid(gclid);
      layer.setHmacKey(new byte[32]);
      CryptoUtil.getSecureRandom().nextBytes(layer.getHmacKey());
      layer.setName(request.getName());
      layer.setGcuid(request.getGcuid());
      
      StringBuilder sql = new StringBuilder();
      sql.append("/*@ SQL-005 */ INSERT INTO layers(gclid,gcuid,fnvname,timestamp,thrift) VALUES (");
      sql.append(CryptoUtil.FNV1a64(gclid));
      sql.append(",");
      sql.append(CryptoUtil.FNV1a64(request.getGcuid()));
      sql.append(",");
      sql.append(CryptoUtil.FNV1a64(layer.getName()));
      sql.append(",");
      sql.append(System.currentTimeMillis());
      sql.append(",x'");
      sql.append(ThriftUtil.serializeHex(layer));
      sql.append("')");
      sql.append("/* SQL-005 */");
      
      dbconn = DB.hold();

      stmt = dbconn.createStatement();

      stmt.executeUpdate(sql.toString());
      
      stmt.close();
      stmt = null;
      
      DB.release();
      dbconn = null;

      // Commit changes
      DB.commit();
      
      LayerAdminResponse resp = new LayerAdminResponse();
      resp.setGclid(gclid);
      
      return resp;
    } catch (SQLException sqle) {
      throw new GeoCoordException(GeoCoordExceptionCode.SQL_ERROR);
    } finally {
      if (needRollback) try { DB.rollback(); } catch (GeoCoordException gce) {}
      if (null != stmt) try { stmt.close(); } catch (SQLException sqle) {}
      if (null != dbconn) try { DB.release(); } catch (GeoCoordException gce) {}
      DB.enableCommit(commitEnabled);
    }
  }
  
  /**
   * Count the number of layers created by a given gcuid.
   * 
   * @param request
   * @return
   * @throws GeoCoordException
   */
  private LayerAdminResponse doCount(LayerAdminRequest request) throws GeoCoordException {
    Connection dbconn = null;
    ResultSet rs = null;
    Statement stmt = null;
    boolean commitEnabled = DB.isCommitEnabled();
    boolean needRollback = false;
    
    try {
      StringBuilder sql = new StringBuilder();
      sql.append("/*@ SQL-006 */ SELECT COUNT(*) FROM layers WHERE gcuid=");
      sql.append(CryptoUtil.FNV1a64(request.getGcuid()));
      
      dbconn = DB.hold();
      stmt = dbconn.createStatement();
      rs = stmt.executeQuery(sql.toString());
      
      long count = 0L;
      
      if (rs.next()) {
        count = rs.getLong(1);
      }
      
      rs.close();
      rs = null;
      
      stmt.close();
      stmt = null;
      
      DB.release();
      dbconn = null;
      
      LayerAdminResponse resp = new LayerAdminResponse();
      resp.setCount(count);
      
      return resp;
    } catch (SQLException sqle) {
      throw new GeoCoordException(GeoCoordExceptionCode.DB_ERROR);
    } finally {
      if (needRollback) try { DB.rollback(); } catch (GeoCoordException gce) {}
      if (null != rs) try { rs.close(); } catch (SQLException sqle) {}
      if (null != stmt) try { stmt.close(); } catch (SQLException sqle) {}
      if (null != dbconn) try { DB.release(); } catch (GeoCoordException gce) {}
      DB.enableCommit(commitEnabled);     
    }
  }
}
