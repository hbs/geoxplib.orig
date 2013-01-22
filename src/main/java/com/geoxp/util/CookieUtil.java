package com.geoxp.util;

import javax.servlet.http.Cookie;

import org.bouncycastle.util.encoders.Base64;

import com.geocoord.thrift.data.Constants;
import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.User;

public class CookieUtil {
  public static Cookie getAuthCookie(User user) throws GeoCoordException {
    
    com.geocoord.thrift.data.Cookie cookie = new com.geocoord.thrift.data.Cookie();
    cookie.setUserId(user.getUserId());

    Cookie gca = new Cookie(Constants.GEOCOORD_AUTH_COOKIE_NAME, new String(Base64.encode(CryptoUtil.wrap(CryptoUtil.pad(8, ThriftUtil.serialize(cookie)))))); 
    gca.setMaxAge(Constants.GEOCOORD_AUTH_COOKIE_TTL);
    return gca;
  }
}
