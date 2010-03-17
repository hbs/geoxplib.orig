package com.geocoord.util;

import java.util.UUID;

import junit.framework.TestCase;

public class NamingUtilTestCase extends TestCase {
  public void testIsUUID() {
    assertTrue(NamingUtil.isUUID(UUID.randomUUID().toString()));
  }
  
  public void testIsValidLayerName() {
    assertFalse(NamingUtil.isValidLayerName(UUID.randomUUID().toString()));
    assertFalse(NamingUtil.isValidLayerName("#$"));
    assertFalse(NamingUtil.isValidLayerName("0000"));
    assertTrue(NamingUtil.isValidLayerName("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-@:"));
  }

  public void testIsValidPointName() {
    assertFalse(NamingUtil.isValidLayerName(UUID.randomUUID().toString()));
    assertFalse(NamingUtil.isValidLayerName("#$"));
    assertFalse(NamingUtil.isValidLayerName("0000"));
    assertTrue(NamingUtil.isValidLayerName("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-@:"));    
  }

  public void testIsValidPublicAttributeName() {
    assertFalse(NamingUtil.isValidPublicAttributeName("0"));
    assertFalse(NamingUtil.isValidPublicAttributeName("-bar"));
    assertFalse(NamingUtil.isValidPublicAttributeName("FOO"));
    assertFalse(NamingUtil.isValidPublicAttributeName(".foo"));
    assertTrue(NamingUtil.isValidPublicAttributeName("abcdefghijklmnopqrstuvwxyz0123456789-"));
  }

  public void testIsValidSystemAttributeName() {
    assertFalse(NamingUtil.isValidSystemAttributeName("0"));
    assertFalse(NamingUtil.isValidSystemAttributeName("FOO"));
    assertFalse(NamingUtil.isValidSystemAttributeName("type"));    
    assertFalse(NamingUtil.isValidSystemAttributeName(".0type"));    
    assertFalse(NamingUtil.isValidSystemAttributeName(".-type"));    
    assertFalse(NamingUtil.isValidSystemAttributeName("abcdefghijklmnopqrstuvwxyz0123456789-"));
    assertTrue(NamingUtil.isValidSystemAttributeName(".foo"));
    assertTrue(NamingUtil.isValidSystemAttributeName(".abcdefghijklmnopqrstuvwxyz0123456789-"));
  }
}
