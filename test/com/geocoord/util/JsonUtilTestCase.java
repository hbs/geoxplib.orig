package com.geocoord.util;

import org.junit.Test;

import com.geocoord.geo.Coverage;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class JsonUtilTestCase {
  @Test
  public void testCoverageFromJson() throws Exception {
    
    String jsonstr = "[ {'mode':'+','def':'circle:48:-4.5:100'}, {'mode':'-','def':'circle:48:-4.5:95'}]";
    JsonParser parser = new JsonParser();        
    JsonArray areas = parser.parse(jsonstr).getAsJsonArray();
    
    Coverage coverage = JsonUtil.coverageFromJson(areas, -4);
    coverage.optimize(0L);
    System.out.println(coverage.getCellCount());
    System.out.println(coverage.toString(" ", ""));
  }
}
