package com.geocoord.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.bouncycastle.util.encoders.Base64;

import com.geocoord.lucene.AttributeTokenStream;
import com.google.common.base.Charsets;

public class NamingUtil {
  
  private static final String PUBLIC_ATTR_NAME_REGEXP = "^" + AttributeTokenStream.INDEXED_ATTRIBUTE_PREFIX + "?[a-zA-Z][a-zA-Z0-9.-]*$";
  
  /**
   * Return true if the given String looks like a UUID. The format
   * checked is that of RFC 4122:
   * 
   *       UUID                   = time-low "-" time-mid "-"
   *                            time-high-and-version "-"
   *                            clock-seq-and-reserved
   *                            clock-seq-low "-" node
   *   time-low               = 4hexOctet
   *   time-mid               = 2hexOctet
   *   time-high-and-version  = 2hexOctet
   *   clock-seq-and-reserved = hexOctet
   *   clock-seq-low          = hexOctet
   *   node                   = 6hexOctet
   *   hexOctet               = hexDigit hexDigit
   *   hexDigit =
   *         "0" / "1" / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9" /
   *         "a" / "b" / "c" / "d" / "e" / "f" /
   *         "A" / "B" / "C" / "D" / "E" / "F"
   * 
   * @param s
   * @return
   */
  public static boolean isUUID(String s) {
    if (null == s) {
      return false;
    }
    return s.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  }
  
  
  private static boolean checkFQDN(String name) {
    // FQDN cannot be longer than 254 characters (plus 1 for the final dot).
    if (name.length() > 254) {
      return false;
    }
    
    // RFC1912 states that labels are <= 63, start and end with letters or digits and can have internal hyphens.
    // FIXME(hbs): it also states that labels cannot be all digits, this is not enforced for now.
    
    if (!name.matches("^([a-z0-9][a-z0-9-]{0,61}[a-z0-9])(\\.[a-z0-9][a-z0-9-]{0,61}[a-z0-9])*$")) {
      return false;
    }
    
    return true;
  }
  
  /**
   * Check if a name is a valid layer name.
   * Layer names are similar to java packages, i.e. reversed internet names such as
   * com.geoxp.foo.bar
   * 
   * @param name
   * @return
   */
  public static boolean isValidLayerName(String name) {
    
    if (!checkFQDN(name)) {
      return false;
    }
    
    // Names must have at least two dots
    if (!name.matches(".+\\..+\\..+")) {
      return false;
    }
    		    
    return true;
  }
  
  /**
   * Check if a name is a valid name for an atom.
   * Atom names are the same as layer names except there is no minimum number of dots required.
   * 
   * @param name
   * @return
   */
  public static boolean isValidAtomName(String name) {
    return checkFQDN(name);
  }
  
  /**
   * Return a combined layer/atom name. Convention used is that of bang paths (i.e. UUCP).
   * -> layer!atom
   * 
   * @param layer
   * @param atom
   * @return
   */
  public static String getLayerAtomName(String layer, String atom) {
    StringBuilder sb = new StringBuilder();
    sb.append(layer);
    sb.append("!");
    sb.append(atom);
    return sb.toString();
  }
  
  /**
   * Compute two FNVs for the given String.
   * One is computed from left to right, the other from right to left so as to
   * obtain a 128bit id with a very low probability of conflict.
   * 
   * @param s
   * @return
   */
  public static byte[] getDoubleFNV(String s) {
    //
    // Convert string to bytes using UTF-8
    //
    
    byte[] data = s.getBytes(Charsets.UTF_8);
    
    //
    // Seed the digest with the 64bits FNV1 init value
    //
    // @see http://www.isthe.com/chongo/tech/comp/fnv/
    //
    long seedLtR = 0xcbf29ce484222325L;
    long seedRtL = 0xcbf29ce484222325L;
    
    for (int i = 0; i < data.length; i++) {
      seedLtR ^= (long) data[i];
      seedRtL ^= (long) data[data.length - 1 - i];
      // Could use seed *= 0x100000001b3L
      seedLtR += (seedLtR << 1) + (seedLtR << 4) + (seedLtR << 5) + (seedLtR << 7) + (seedLtR << 8) + (seedLtR << 40);
      seedRtL += (seedRtL << 1) + (seedRtL << 4) + (seedRtL << 5) + (seedRtL << 7) + (seedRtL << 8) + (seedRtL << 40);
    }
    
    ByteBuffer bb = ByteBuffer.allocate(16);
    bb.order(ByteOrder.BIG_ENDIAN);
    bb.putLong(seedLtR);
    bb.putLong(seedRtL);
    
    return bb.array();
  }

  /**
   * Return the Fowler Noll Vo 64bit FNV-1a hash of the data.
   * 
   * @param s
   * @return
   */
  public static byte[] getFNV(String s) {
    //
    // Convert string to bytes using UTF-8
    //
    
    byte[] data = s.getBytes(Charsets.UTF_8);
    
    //
    // Seed the digest with the 64bits FNV1 init value
    //
    // @see http://www.isthe.com/chongo/tech/comp/fnv/
    //
    long seedLtR = 0xcbf29ce484222325L;
    
    for (int i = 0; i < data.length; i++) {
      seedLtR ^= (long) data[i];
      // Could use seed *= 0x100000001b3L
      seedLtR += (seedLtR << 1) + (seedLtR << 4) + (seedLtR << 5) + (seedLtR << 7) + (seedLtR << 8) + (seedLtR << 40);
    }
    
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.order(ByteOrder.BIG_ENDIAN);
    bb.putLong(seedLtR);
    
    return bb.array();
  }

  public static byte[] getLayerAtomUuid(String layerId, String atomId) {
    //return getDoubleFNV(getLayerAtomName(layerId, atomId));
    byte[] uuid = new byte[16];
    System.arraycopy(getFNV(layerId), 0, uuid, 0, 8);
    System.arraycopy(getFNV(atomId), 0, uuid, 8, 8);
    return uuid;
  }
  
  public static String getEncodedLayerAtomUuid(String layerId, String atomId) {
    return new String(Base64.encode(getLayerAtomUuid(layerId, atomId)));
  }
  
  /**
   * Check if a String is a valid attribute name.
   * 
   * @param name String to check.
   * @return
   */
  public static boolean isValidPublicAttributeName(String name) {
    return name.matches(PUBLIC_ATTR_NAME_REGEXP);
  }

  /**
   * Check if a String is a System attribute name
   * @param name
   * @return
   */
  public static boolean isValidSystemAttributeName(String name) {
    return name.matches("^\\.[a-z][a-z0-9-]*$");
  }
}
