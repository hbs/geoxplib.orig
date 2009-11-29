package com.geocoord.util;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.bouncycastle.util.encoders.Hex;

public class CryptoUtilTestCase extends TestCase {
  
  public void testPercentEncodeRfc3986() throws Exception {
    String s = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~àé/=%";
    
    assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~%C3%A0%C3%A9%2F%3D%25", CryptoUtil.percentEncodeRfc2986(s));
  }
  
  public void testSignRequest() throws Exception {
    
    byte[] key = Hex.decode("0000000000000000000000000000000000000000000000000000000000000000");
    
    HttpServletRequest mock = createMock(HttpServletRequest.class);

    expect(mock.getMethod()).andReturn("POST");
    expect(mock.getRequestURI()).andReturn("/foo/bar");
    expect(mock.getParameterNames()).andReturn(new Enumeration<String>() {
      private int idx = 0;
      private String[] names = { "b", "à", "sig" };
      
      public boolean hasMoreElements() {
        return idx < names.length;
      }
      
      public String nextElement() {
        if (idx < names.length) {
          return names[idx++];
        } else {
          return null;
        }
      }
    });
    expect(mock.getParameterValues("à")).andReturn(new String[] { "val2", "val1" });
    expect(mock.getParameterValues("b")).andReturn(new String[] { "à" });
    expect(mock.getParameterValues("sig")).andReturn(new String[] { "a" });
    replay(mock);
    
    String sig = CryptoUtil.signRequest(mock, key);
    
    assertEquals("457040f4c8ed61924c54846c75f4de234900b584172394a38400b9d076100ca4", sig);
  }
}
