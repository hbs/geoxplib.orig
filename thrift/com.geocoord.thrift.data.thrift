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

//
// Key for twitterInfo map in struct User.
//

const string TWITTER_ACCOUNT_DETAILS = "ad" // JSON Representation of Twitter account details (as returned by ACCOUNT_VERIFY_CREDENTIALS)
const string TWITTER_SCREEN_NAME = "sn"
const string TWITTER_PHOTO_URL = "pu"
const string TWITTER_ACCESS_TOKEN = "at"
const string TWITTER_ACCESS_TOKEN_SECRET = "ats"
const string TWITTER_ID = "id"


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

const string CASSANDRA_CLUSTER = "GeoXP"
const string CASSANDRA_KEYSPACE = "GeoXP"
const string CASSANDRA_HISTORICAL_DATA_COLFAM = "HistoricalData"
const string CASSANDRA_ACTIVITY_STREAMS_COLFAM = "ActivityStreams"
const string CASSANDRA_LAYER_ROWKEY_PREFIX = "L"
const string CASSANDRA_ATOM_ROWKEY_PREFIX = "A"
const string CASSANDRA_USER_ROWKEY_PREFIX = "U"
const string CASSANDRA_USER_ALIAS_ROWKEY_PREFIX = "UA"
const string CASSANDRA_USERLAYERS_ROWKEY_PREFIX = "UL"
const string CASSANDRA_LAYER_NAME_ROWKEY_PREFIX = "LN"
const string CASSANDRA_CELL_ROWKEY_PREFIX = "G"

const i32 LAYER_HMAC_KEY_BYTE_SIZE = 32
const i32 USER_HMAC_KEY_BYTE_SIZE = 32

const i32 DEFAULT_PER_USER_MAX_LAYERS = 16

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
  USER_NOT_FOUND = 404,
  
  LAYER_ERROR = 500,
  LAYER_INVALID_GCLID = 501,
  LAYER_NOT_FOUND = 502,
  LAYER_DELETED = 503,
  LAYER_MISSING_HMAC = 504,
  
  CENTROID_SERVICE_ERORR = 600,
  CENTROID_SERVICE_PARSE_ERROR = 601,
  CENTROID_SERVICE_IO_ERROR = 602,
  
  CASSANDRA_ERROR = 700,
  CASSANDRA_HOLD_ERROR = 701,
  CASSANDRA_RELEASE_ERROR = 702
  CASSANDRA_LOCK_FAILED = 703,
  
  ATOM_ERROR = 800,
  ATOM_TYPE_MISMATCH = 801,
  ATOM_ID_MISMATCH = 802,
  ATOM_NOT_FOUND = 803,
  
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


/**
 * Structure defining a point as handled by GeoCoord.
 */
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
  // Altitude in meters
  //
  3: double altitude,
  
  //
  // Timestamp of point as ms since the Epoch. This value is
  // chosen by the user.
  //
  4: i64 timestamp,
  
  //
  // Layer this point belongs to
  //
  5: string layerId,
  
  //
  // User who created this point
  //
  6: string userId,
  
  //
  // Name of point
  //
  7: optional string name,
  
  //
  // Tags associated with point
  //
  8: optional string tags,
  
  //
  // User defined attributes, multivalued.
  // When returning a point result, system attributes may be included.
  // System attributes all start with '.', eg '.time'.
  //
  9: map<string,list<string>> attributes,
}

struct User {
  /**
   * Unique id of user
   */
  1: string userId,
  
  /**
   * HMAC Key of the user (256bits)
   */
  2: binary hmacKey,
  
  /**
   * Timestamp of update.
   */
  3: i64 timestamp,
  
  /**
   * Twitter user details
   *
   * @see TWITTER_* keys
   */
  4: map<string,string> twitterInfo,
  
  /**
   * Maximum number of layers for this user.
   */
  5: i32 maxLayers = DEFAULT_PER_USER_MAX_LAYERS,
}


//
// Cookie
//

struct Cookie {
  //
  // GeoCoord User ID
  //  
  1: string userId,
}


// TODO(hbs): add a list of user uuids which are allowed to post/delete to the layer (maybe delete only their points)
struct Layer {
  /**
   * UUID of the layer
   */
  1: string layerId,

  /**
   * UUID of user having created the layer
   */
  2: string userId,
  
  /**
   * HMAC Key for layer (256 bits)
   */
  3: binary hmacKey,
  
  /**
   * Privacy of layer.
   * Public layers can be searched freely. Private layers can only be searched
   * via search requests signed with the correct layer keys
   */
  4: bool publicLayer = 1
  
  /**
   * Should the layer be indexed.
   * Some layers (Skyhook SpotRank for example, which has 10s of B of updates per day) should
   * not be indexed. This will limit the kind of searches that can be performed (basically
   * only lookups per location), but at least it will be possible to have that much data.
   */
  5: bool indexed = 1,
  
  /**
   * Timestamp of this version
   */
  6: i64 timestamp,
  
  /**
   * Marker indicating that the layer has been deleted.
   */
  7: bool deleted = 0,  
}

enum AtomType {
  POINT = 1,
}
struct Atom {
  /**
   * Type of Atom
   */
  1: AtomType type,
  
  /**
   * Timestamp of Atom update.
   */
  2: i64 timestamp,
  
  /**
   * Marker indicating that the atom has been deleted
   */
  3: bool deleted = 0;   
  
  /**
   * Point component.
   */
  4: optional Point point,
}

enum ActivityEventType {
  CREATE = 1,
  UPDATE = 2,
  REMOVE = 3,
}

struct ActivityEvent {
  /**
   * Type of activity event.
   */
  1: ActivityEventType type,
  
  /**
   * Atoms concerned.
   * For UPDATE, the list MUST be even and
   * contain a succession of <old atom><new atom> 
   */
  2: list<Atom> atoms,
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


//
// UserService related objects
//
////////////////////////////////////////////////////////////////////////////////

struct UserCreateRequest {
  /**
   * User to create.
   */
  1: User user,
}

struct UserCreateResponse {
  /**
   * User just created.
   */
  1: User user,
}

struct UserAliasRequest {
  /**
   * User id of user for which we want to set an alias.
   */
  1: string userId,
  
  /**
   * Alias to set.
   */
  2: string alias,
}

struct UserAliasResponse {
}

struct UserRetrieveRequest {
  /**
   * UUID of the user to retrieve
   */
  1: string userId,
  
  /**
   * Should we also retrieve the user's layers
   */
  2: bool includeLayers = 0,
  
  /**
   * Alias to use for retrievel.
   */
  3: string alias,
}

struct UserRetrieveResponse {
  /**
   * Retrieved userer.
   */
  1: User user,
  
  /**
   * List of retrieved layers
   */
  2: list<Layer> layers,
}

struct UserUpdateRequest {
  /**
   * User to update
   */
  1: User user,
}

struct UserUpdateResponse {
  /**
   * Updated user.
   */
  1: User user,
}

////////////////////////////////////////////////////////////////////////////////
//
// UserService related objects
//

//
// LayerService related objects
//
////////////////////////////////////////////////////////////////////////////////

struct LayerCreateRequest {
  /**
   * Cookie of the requester.
   */
  1: Cookie cookie,
  
  /**
   * Layer to create.
   */
  2: Layer layer,
}

struct LayerCreateResponse {
  /**
   * Layer just created.
   */
  1: Layer layer,
}

struct LayerRetrieveRequest {
  /**
   * UUID of the layer to retrieve
   */
  1: string layerId,
}

struct LayerRetrieveResponse {
  /**
   * Retrieved layer.
   */
  1: Layer layer,
}

struct LayerUpdateRequest {
  /**
   * Layer to update
   */
  1: Layer layer,
}

struct LayerUpdateResponse {
  /**
   * Updated layer.
   */
  1: Layer layer,
}

struct LayerRemoveRequest {
  /**
   * Layer to delete
   */
  1: Layer layer,
}

struct LayerRemoveResponse {
  /**
   * Deleted layer.
   */
  1: Layer layer,
}

////////////////////////////////////////////////////////////////////////////////
//
// LayerService related objects
//



//
// AtomService related objects
//
////////////////////////////////////////////////////////////////////////////////

struct AtomCreateRequest {
  /**
   * Cookie of the requester.
   */
  1: Cookie cookie,
  
  /**
   * Atom to create.
   */
  2: Atom atom,
}

struct AtomCreateResponse {
  /**
   * Atom just created.
   */
  1: Atom atom,
}

struct AtomRetrieveRequest {
  /**
   * UUID of the Atom to retrieve
   */
  1: string atomId,
}

struct AtomRetrieveResponse {
  /**
   * Retrieved Atom.
   */
  1: Atom atom,
}

struct AtomUpdateRequest {
  /**
   * Old Atom
   */
  1: Atom atom,
  
  /**
   * New Atom
   */
  2: Atom newAtom,  
}

struct AtomUpdateResponse {
  /**
   * Updated Atom.
   */
  1: Atom atom,
}

struct AtomRemoveRequest {
  /**
   * Atom to delete
   */
  1: Atom atom,
}

struct AtomRemoveResponse {
  /**
   * Deleted Atom.
   */
  1: Atom atom,
}

////////////////////////////////////////////////////////////////////////////////
//
// AtomService related objects
//

