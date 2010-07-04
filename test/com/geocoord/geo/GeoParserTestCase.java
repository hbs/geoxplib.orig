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
    
    List<Long> hhcodes = GeoParser.parseEncodedPolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
    Assert.assertEquals(new ArrayList<Long>() {{ add(-8183920036795033730L); add(-8159399539329351909L); add(-8163195369541500114L); }}, hhcodes);
    
    hhcodes = GeoParser.parseEncodedPolyline("`~oia@");
    Assert.assertEquals(new ArrayList<Long>() {{ add(-4611685846584612350L);}}, hhcodes);
  }
}
