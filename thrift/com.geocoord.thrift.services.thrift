namespace java com.geocoord.thrift.services

include "com.geocoord.thrift.data.thrift"

service CoverageService {
  com.geocoord.thrift.data.CoverageResponse getCoverage(1:com.geocoord.thrift.data.CoverageRequest request)  
}

service DataService {
  //com.geocoord.thrift.data.DataResponse lookup(1:com.geocoord.thrift.data.DataRequest) throws (1:com.geocoord.thrift.data.GeoCoordException e)
}

service SearchService {
}

service UserService {
  com.geocoord.thrift.data.UserCreateResponse   create(1:com.geocoord.thrift.data.UserCreateRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
  com.geocoord.thrift.data.UserAliasResponse    alias(1:com.geocoord.thrift.data.UserAliasRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
  com.geocoord.thrift.data.UserRetrieveResponse retrieve(1:com.geocoord.thrift.data.UserRetrieveRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)    
  com.geocoord.thrift.data.UserUpdateResponse   update(1:com.geocoord.thrift.data.UserUpdateRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)  
}

/**
 * Service used to access Layer objects persistently stored.
 */
service LayerService {
  com.geocoord.thrift.data.LayerCreateResponse   create(1:com.geocoord.thrift.data.LayerCreateRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
  com.geocoord.thrift.data.LayerRetrieveResponse retrieve(1:com.geocoord.thrift.data.LayerRetrieveRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
  com.geocoord.thrift.data.LayerUpdateResponse   update(1:com.geocoord.thrift.data.LayerUpdateRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
  com.geocoord.thrift.data.LayerRemoveResponse   remove(1:com.geocoord.thrift.data.LayerRemoveRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)      
}

/**
 * Service to manage atoms (points, polygons, paths)
 */
service AtomService {
  com.geocoord.thrift.data.AtomStoreResponse    store(1:com.geocoord.thrift.data.AtomStoreRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
  com.geocoord.thrift.data.AtomRetrieveResponse retrieve(1:com.geocoord.thrift.data.AtomRetrieveRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
  com.geocoord.thrift.data.AtomRemoveResponse   remove(1:com.geocoord.thrift.data.AtomRemoveRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)      
}

/**
 * An activity service records the changes and makes sure
 * the data gets correctly indexed.
 */
service ActivityService {
  void record(1:com.geocoord.thrift.data.ActivityEvent event) throws (1:com.geocoord.thrift.data.GeoCoordException e)
}

service CentroidService {
  com.geocoord.thrift.data.CentroidResponse search(1:com.geocoord.thrift.data.CentroidRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
}
