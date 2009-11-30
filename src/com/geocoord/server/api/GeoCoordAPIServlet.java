package com.geocoord.server.api;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;
import com.geocoord.util.CryptoUtil;

public class GeoCoordAPIServlet extends HttpServlet {
  
  protected boolean checkParams(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    //
    // Check for mandatory parameters
    //
    
    if (null == req.getParameter(Constants.API_PARAM_TS)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_TIMESTAMP");
      return false;
    }
    
    if (null == req.getParameter(Constants.API_PARAM_ID)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_ID");
      return false;
    }

    if (null == req.getParameter(Constants.API_PARAM_SIG)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_MISSING_SIGNATURE");
      return false;
    }

    //
    // Extract timestamp, check that it's still valid
    //
    
    try {     
      long delay = System.currentTimeMillis() - Long.valueOf(req.getParameter(Constants.API_PARAM_TS));
      
      if (delay > Constants.API_SIGNATURE_TTL) {
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_EXPIRED_SIGNATURE");
        return false;
      }
    } catch (NumberFormatException nfe) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_TIMESTAMP");
      return false;
    }

    return true;
  }
  
  protected boolean checkSignature(HttpServletRequest req, HttpServletResponse resp, byte[] key) throws IOException {
    //
    // Check request signature
    //

    try { 
      String sig = CryptoUtil.signRequest(req, key);
      
      if (!sig.equals(req.getParameter(Constants.API_PARAM_SIG))) {
        throw new GeoCoordException(GeoCoordExceptionCode.API_EXPIRED_SIGNATURE);
      }
    } catch (GeoCoordException gce) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "API_INVALID_SIGNATURE");
      return false;
    }
    
    return true;
  }
}
