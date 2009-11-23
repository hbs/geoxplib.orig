namespace java com.geocoord.thrift.services

include "com.geocoord.thrift.data.thrift"

service CoverageService {
  com.geocoord.thrift.data.CoverageResponse getCoverage(1:com.geocoord.thrift.data.CoverageRequest request)  
}