package com.geoxp.twitter;

public class Twitter {
  /*
  public static final String CONSUMER_KEY = "e3D1Liq8H8REuD7JniQUQ";
  public static final String CONSUMER_SECRET = "WMmBYwESuPbBiO0QE8elKt1VXosFBBEX3pQjDHxYEA";
  */
  
  /*
   * OAuth parameters from http://dev.twitter.com/apps/189392
   */
  public static final String CONSUMER_KEY = "o7vMXEbuyOviDQeVyS8Tbw";
  public static final String CONSUMER_SECRET = "iiJragm4ycsXZ8GLKYrUaIWZWQit96l8Gyr6fEB2Uc";
  
  public static final String REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token";
  public static final String ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
  public static final String AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize";
  public static final String AUTHENTICATE_URL = "https://api.twitter.com/oauth/authenticate";
  
  public static final String API_ACCOUNT_VERIFY_CREDENTIALS_URL = "http://twitter.com/account/verify_credentials.json";
  
  //public static final String CALLBACK_URL = "http://www.GeoCoord.com/twitter/callback";
  //public static final String CALLBACK_URL = "http://www.geoxp.com/twitter/callback";
  public static final String CALLBACK_URL = "http://localhost:8888/twitter/callback";
  public static final String ERROR_URL = "http://www.GeoCoord.com/twitter/error";
}
