package com.geocoord.geo;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class GeoParserTestCase {

  @Test
  public void testParsePolyline() {
    //
    // Test values from http://code.google.com/apis/maps/documentation/utilities/polylinealgorithm.html
    //
    
    List<Long>[] hhcoords = GeoParser.parseEncodedPolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
    
    Assert.assertEquals(3, hhcoords[0].size());
    Assert.assertEquals(3, hhcoords[1].size());
    
    Assert.assertEquals(new ArrayList<Long>() {{ add(3066129430L); add(3118623475L); add(3179516567L); }}, hhcoords[0]);    
    Assert.assertEquals(new ArrayList<Long>() {{ add(713441789L); add(704493941L); add(638840593L); }}, hhcoords[1]);
        
    hhcoords = GeoParser.parseEncodedPolyline("`~oia@");
    
    
    Assert.assertEquals(-2147083022L, (long) hhcoords[0].get(0));
    Assert.assertEquals(2147483648L, (long) hhcoords[1].get(0));
    
    Assert.assertEquals(HHCodeHelper.toLongLat(-179.98321), (long) hhcoords[0].get(0));    
    Assert.assertEquals(HHCodeHelper.toLongLat(0.0), (long) hhcoords[1].get(0));    
  }
  
}
