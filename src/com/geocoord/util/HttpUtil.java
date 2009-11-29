package com.geocoord.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class HttpUtil {
  /**
   * Return the values associated with a Cookie
   * 
   * @param request
   * @param name
   * @return
   */
  public static List<String> getCookieValues(HttpServletRequest request, String name) {
    
    List<String> values = new ArrayList<String>();
    
    for (Cookie cookie: request.getCookies()) {
      if (cookie.getName().equals(name)) {
        values.add(cookie.getValue());
      }
    }
    
    return values;
  }

  public static String getCookieValue(HttpServletRequest request, String name) {
    
    for (Cookie cookie: request.getCookies()) {
      if (cookie.getName().equals(name)) {
        return cookie.getValue();
      }
    }
    
    return null;
  }

}
