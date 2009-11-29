namespace java com.geocoord.thrift.data


enum GeoCoordExceptionCode {
  GENERIC_ERROR = 0,
  THRIFT_ERROR = 2,
  ENCODING_ERROR = 3,
  SQL_ERROR = 4,
  CLASS_ERROR = 5,
  
  DB_ERROR = 100,
  DB_CONNECTION_ALREADY_HELD = 101,
  DB_CONTEXT = 102,
  DB_DATASOURCE = 103,
  DB_CONNECTION = 104,
  DB_NO_CONNECTION_HELD = 105,
  DB_CONNECTION_ALREADY_RELEASED = 106,
  DB_CONNECTION_HELD = 107,
  DB_COMMIT = 108,
  DB_ROLLBACK = 109,
  
  CRYPTO_ERROR = 200,
  CRYPTO_INVALID_CIPHER_TEXT = 201,
}

exception GeoCoordException {
  //
  // Code of the exception
  // @see #GeoCoordExceptionCode for details
  //
  1: GeoCoordExceptionCode code,
}

struct CoverageResponse {
  // Cells of the coverage.
  // Key is cell name, value is list of coordinates (sw/ne corners in the order swLat,swLon,neLat,neLon)
  // of covering squares.
  1: map<string,list<double>> cells,
}

enum CoverageType {
  POLYLINE = 1,
  POLYGON = 2,
}

struct CoverageRequest {
  //
  // Type of coverage
  //
  1: CoverageType type,
  
  //
  // Polygon/Polyline to cover, even list of degree lat/lon
  //
  2: list<double> path,
  
  //
  // Resolution
  //
  3: i32 resolution,
  
  //
  // Threshold for coverage optimization, as a hex string.
  //
  4: string threshold,
}


struct Point {
  //
  // Unique id of point
  //
  1: string pointId,
  
  //
  // Location of point as a HHCode
  //
  2: i64 hhcode,
  
  //
  // Latitude/Longitude so we do not have to recompute them
  //
  3: double latitude,
  4: double longitude,
  
  //
  // Altitude in meters
  //
  5: double altitude,
  
  //
  // Timestamp of point as ms since the Epoch
  //
  6: i64 timestamp,
  
  //
  // Layer this point belongs to
  //
  7: string layerId,
  
  //
  // User who created this point
  //
  8: string userId,  
}


struct User {
  //
  // Unique id of user
  //
  1: string gcuid,
  
  //
  // JSON Representation of Twitter account details (as returned by ACCOUNT_VERIFY_CREDENTIALS)
  //
  2: string twitterAccountDetails,
  
  //
  // Twitter screen name
  //
  3: string twitterScreenName,
  
  //
  // Twitter photo Url
  //
  4: string twitterPhotoURL,

  //
  // Twitter access token
  //
  5: string twitterAccessToken,
  
  //
  // Twitter access token secret
  //
  6: string twitterAccessTokenSecret,
  
  //
  // Twitter id
  //
  7: string twitterId,
    
}