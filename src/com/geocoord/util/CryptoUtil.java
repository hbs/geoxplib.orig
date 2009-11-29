package com.geocoord.util;

import java.io.UnsupportedEncodingException;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESWrapEngine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.geocoord.thrift.data.GeoCoordException;
import com.geocoord.thrift.data.GeoCoordExceptionCode;

public class CryptoUtil {

  private static final byte[] wrapKeyBytes = {(byte) 0xA6, (byte) 0xF6, (byte) 0x48, (byte) 0x7C,
                                              (byte) 0x72, (byte) 0x12, (byte) 0x51, (byte) 0x48,
                                              (byte) 0x48, (byte) 0x1C, (byte) 0x97, (byte) 0x81,
                                              (byte) 0xA8, (byte) 0x5C, (byte) 0x9A, (byte) 0xB8,
                                              (byte) 0xF8, (byte) 0x54, (byte) 0x8F, (byte) 0xD7,
                                              (byte) 0x80, (byte) 0x09, (byte) 0xB0, (byte) 0x8C,
                                              (byte) 0xAA, (byte) 0xD0, (byte) 0x4B, (byte) 0x1E,
                                              (byte) 0x67, (byte) 0x1B, (byte) 0x46, (byte) 0x8F};
  
  private static final byte[] wrapIVBytes = { (byte) 0xCC, (byte) 0xFC, (byte) 0x00, (byte) 0x83,
                                              (byte) 0x8C, (byte) 0xFD, (byte) 0x3B, (byte) 0xD5};
  
  private static final KeyParameter wrapKey = new KeyParameter(wrapKeyBytes);
  private static final ParametersWithIV wrapParams = new ParametersWithIV(wrapKey, wrapIVBytes);

  public static byte[] wrap(byte[] data) {
    AESWrapEngine aes = new AESWrapEngine();
    aes.init(true, wrapParams);
    return aes.wrap(data, 0, data.length);
  }
  
  public static byte[] unwrap(byte[] data) throws GeoCoordException {
    try {
      AESWrapEngine aes = new AESWrapEngine();
      aes.init(false, wrapParams);
      return aes.unwrap(data, 0, data.length);      
    } catch (InvalidCipherTextException icte) {
      throw new GeoCoordException(GeoCoordExceptionCode.CRYPTO_INVALID_CIPHER_TEXT);
    }
  }

  public static byte[] pad(int boundary, byte[] data) {
    
    //
    // Allocate a byte array to hold the data and be a multiple of boundary bytes
    // If data is already a multiple of boundary bytes, the allocated array will
    // be boundary bytes bigger than data.
    //
    
    byte[] padded = new byte[data.length + (boundary - data.length % boundary)];
    
    //
    // Copy the original data
    //
    
    System.arraycopy (data, 0, padded, 0, data.length);
    
    //
    // Add padding bytes
    //
    
    PKCS7Padding padding = new PKCS7Padding();
    
    padding.addPadding(padded, data.length);
                    
    return padded;
  }

  public static byte[] unpad(byte[] data) throws GeoCoordException {
    
    try {
      PKCS7Padding padding = new PKCS7Padding();
      
      int pad = padding.padCount(data);
      
      byte[] unpadded = new byte[data.length - pad];
      
      System.arraycopy(data,0,unpadded,0,data.length - pad);
      
      return unpadded;      
    } catch (InvalidCipherTextException icte) {
      throw new GeoCoordException(GeoCoordExceptionCode.CRYPTO_INVALID_CIPHER_TEXT);
    }
  }

  public static long FNV1a64(byte[] data) throws GeoCoordException {
    return FNV1a64(data, 0, data.length);
  }
  
  public static long FNV1a64(byte[] data, int offset, int len) throws GeoCoordException {
    //
    // Seed the digest with the 64bits FNV1 init value
    // 
    // @see http://www.isthe.com/chongo/tech/comp/fnv/
    //
    
    long seed = 0xcbf29ce484222325L;
        
    for (int i = 0; i < len; i++) {
      seed ^= data[i + offset];
      // Could use seed *= 0x100000001b3L
      seed += (seed << 1) + (seed << 4) + (seed << 5) + (seed << 7) + (seed << 8) + (seed << 40);
    }
        
    return seed;            
  }
  
  public static long FNV1a64(String str) throws GeoCoordException {
    try {
      return FNV1a64(str.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException uee) {
      throw new GeoCoordException(GeoCoordExceptionCode.ENCODING_ERROR);
    }
    }
  }
