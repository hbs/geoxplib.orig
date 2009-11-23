namespace java com.geocoord.thrift.data


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