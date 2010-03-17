package com.geocoord.util;

public class NamingUtil {
  
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
    return s.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  }
  
  public static boolean isValidLayerName(String name) {
    return name.matches("^[a-zA-Z][a-zA-Z0-9\\.:_@-]+$") && !isUUID(name);
  }
  
  public static boolean isValidPointName(String name) {
    return isValidLayerName(name);
  }
  
  /**
   * Check if a String is a valid attribute name.
   * 
   * @param name String to check.
   * @return
   */
  public static boolean isValidPublicAttributeName(String name) {
    return name.matches("^[a-z][a-z0-9-]*$");
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
