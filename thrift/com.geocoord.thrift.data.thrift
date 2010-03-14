namespace java com.geocoord.thrift.data

/**
 * Name of the authentication cookie.
 */
const string GEOCOORD_AUTH_COOKIE_NAME = "gca"

/**
 * MaxAge of the authentication cookie in 's' (100 days)
 */
const i32 GEOCOORD_AUTH_COOKIE_TTL = 8640000
 
const string GEOCOORD_HOME_PAGE_URL = "/";

//
// Cassandra related constants
//

const string CASSANDRA_GEOCOORD_KEYSPACE = "GeoCoord"
const string CASSANDRA_ADMATOMS_CF = "AdmAtoms"
const string CASSANDRA_GEOATOMS_CF = "GeoAtoms"
const string CASSANDRA_INDEXOPS_CF = "IndexOps"
const string CASSANDRA_OWNER_COLUMN = "owner";

//
// Lucene Index related constants
//

const string LUCENE_ID_FIELD = "id"
const string LUCENE_HHCODE_FIELD = "hhcode"
const string LUCENE_CELLS_FIELD = "cells"

//
// API Related constants
//

// Request signatures are valid for 15 seconds
const i64 API_SIGNATURE_TTL = 15000

const string API_PARAM_TS = "ts"
const string API_PARAM_ID = "id"
const string API_PARAM_SIG = "sig"

const string API_PARAM_LAYER_CREATE_NAME = "name"
const string API_PARAM_LAYER_CREATE_PRIVACY = "privacy"

const string API_PARAM_POINT_NAME = "gcname"
const string API_PARAM_POINT_LAT = "gclat"
const string API_PARAM_POINT_LON = "gclon"

const string API_PARAM_POINT_CREATE_COUNT = "count"

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
  
  API_ERROR = 300,
  API_MISSING_TIMESTAMP = 301,
  API_MISSING_ID = 302,
  API_MISSING_SIGNATURE = 303,
  API_INVALID_TIMESTAMP = 304,
  API_EXPIRED_SIGNATURE = 305,
  API_INVALID_ID = 306,
  API_INVALID_SIGNATURE = 307,
  API_MISSING_NAME = 308,
  API_MISSING_PRIVACY = 309,
  API_TOO_MANY_LAYERS =310,
  API_INVALID_POINT_COUNT = 311,
  
  USER_ERROR = 400,
  USER_INVALID_GCUID = 401,
  USER_INVALID_TWITTER_ID = 402,
  USER_INVALID_ID_TYPE = 403,
  
  LAYER_ERROR = 500,
  LAYER_INVALID_GCLID = 501,
  
  CENTROID_SERVICE_ERORR = 600,
  CENTROID_SERVICE_PARSE_ERROR = 601,
  CENTROID_SERVICE_IO_ERROR = 602,
  
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
  1: string gcpid,
  
  //
  // Location of point as a HHCode
  //
  2: i64 hhcode,
  
  //
  // Altitude in meters
  //
  3: double altitude,
  
  //
  // Timestamp of point as ms since the Epoch
  //
  4: i64 timestamp,
  
  //
  // Layer this point belongs to
  //
  5: string gclid,
  
  //
  // User who created this point
  //
  6: string gcuid,
  
  //
  // Name of point
  //
  7: optional string gcname,
  
  //
  // Tags associated with point
  //
  8: optional string gctags,
  
  //
  // Text associated with point
  //
  9: optional string gctext,
  
  //
  // URL associated with point
  //
  10: optional string url,
  
  //
  // User defined attributes, multivalued
  //
  11: map<string,list<string>> gcattr,
  
  
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
  
  //
  // HMAC Key of the user (256bits)
  //
  8: binary hmacKey,
  
  //
  // Maximum number of allowed layers
  //
  9: i32 maxLayers = 2,
}





//
// GeoCoordCookie
//

struct GeoCoordCookie {
  //
  // GeoCoord User ID
  //  
  1: string gcuid,
  //
  // FNV of gcuid
  //
  2: i64 fnv,
}



//
// LayerAdminRequest
//

enum LayerAdminRequestType {
  CREATE = 1,
  COUNT = 2,
}

struct LayerAdminRequest {
  // Type of request
  1: LayerAdminRequestType type,
  // User issueing the request
  2: string gcuid,
  // Id of layer
  3: string gclid,
  // Name of layer
  4: string name,
  // Privacy of layer
  5: bool publicLayer,
}

struct LayerAdminResponse {
  // Id of layer
  1: optional string gclid,
  // Count of layers (for COUNT requests)
  2: optional i64 count,
}

struct Layer {
  // UUID of layer
  1: string gclid,
  // UUID of user having created the layer
  2: string gcuid,
  // HMAC Key for layer
  3: binary hmacKey,
  // Name of layer - Allows to access the layer using its name
  4: string name,
  // Privacy of layer
  5: bool publicLayer 
}

struct PointStoreRequest {
  /**
   * Cookie of requesting user
   */
  1: GeoCoordCookie cookie,
  /**
   * List of points to create.
   */
  2: list<Point> points,
}

struct PointStoreResponse {
  /**
   * List of points created.
   */
  1: list<Point> points,
}

struct CentroidRequest {
  /**
   * Include points for cells having less than or that many markers
   */
  1: i32 pointThreshold,
  2: double topLat,
  3: double bottomLat,
  4: double leftLon,
  5: double rightLon,
  6: i32 resolution,
  /**
   * Only use at most that many points to compute centroid.
   */ 
  7: i32 maxCentroidPoints,
}

struct CentroidPoint {
  1: double lat,
  2: double lon,
  3: string id,
}

struct Centroid {
  1: double topLat,
  2: double bottomLat,
  3: double leftLon,
  4: double rightLon,
  /**
   * Number of points found in the zone above
   */
  5: i32 count,
  /**
   * List of markers found in the zone (either all the markers
   * found in the zone if there are just a few or maybe the latest
   * ones)
   */
  6: list<CentroidPoint> points,
  /**
   * Centroid coordinates (in long)
   */
  7: optional i64 longLat,
  8: optional i64 longLon,
  /**
   * Double coordinates
   */
  9: double lat,
  10: double lon,
}

struct CentroidResponse {
  1: list<Centroid> centroids,
}