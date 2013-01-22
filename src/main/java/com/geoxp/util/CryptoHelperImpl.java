package com.geoxp.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoHelperImpl implements CryptoHelper {
  
  private static SecureRandom SR = null;
  
  static {
    try {
      SR = SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException nsae) {
      
    }    
  }
  
  @Override
  public SecureRandom getSecureRandom() {
    return SR;
  }
} 
