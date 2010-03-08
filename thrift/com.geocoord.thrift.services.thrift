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
  com.geocoord.thrift.data.User load(1:string key) throws (1:com.geocoord.thrift.data.GeoCoordException e)
  com.geocoord.thrift.data.User store(1:com.geocoord.thrift.data.User user) throws (1:com.geocoord.thrift.data.GeoCoordException e)  
}

/**
 * Service used to access Layer objects persistently stored.
 */
service LayerService {
  /**
   * Administrate layers. This includes creating/deleteing/updating
   */
  com.geocoord.thrift.data.LayerAdminResponse admin(1:com.geocoord.thrift.data.LayerAdminRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
  
  com.geocoord.thrift.data.Layer load(1:string key) throws (1:com.geocoord.thrift.data.GeoCoordException e)
}

service PointService {
  com.geocoord.thrift.data.PointStoreResponse store(1:com.geocoord.thrift.data.PointStoreRequest point) throws (1:com.geocoord.thrift.data.GeoCoordException e)  
}


service CentroidService {
  com.geocoord.thrift.data.CentroidResponse search(1:com.geocoord.thrift.data.CentroidRequest request) throws (1:com.geocoord.thrift.data.GeoCoordException e)
}
