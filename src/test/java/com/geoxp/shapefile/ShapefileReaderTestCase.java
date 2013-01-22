package com.geoxp.shapefile;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;

public class ShapefileReaderTestCase {
  @Test
  public void test() {
    File file = new File("/Users/hbs/lab/bretagne-shapefiles/bretagne-administrative.shp");
    
    try {
      Map connect = new HashMap();
      connect.put("url", file.toURL());

      ShapefileDataStore dataStore = new ShapefileDataStore(new URL(file.toURI().toString()));
      String[] typeNames = dataStore.getTypeNames();
      String typeName = typeNames[0];

      System.out.println("Reading content " + typeName);

      FeatureSource featureSource = dataStore.getFeatureSource(typeName);
      FeatureCollection collection = featureSource.getFeatures();
      FeatureIterator iterator = collection.features();


      try {
        while (iterator.hasNext()) {
          Feature feature = iterator.next();
          GeometryAttribute attr = feature.getDefaultGeometryProperty();
          System.out.println(attr);
        }
      } finally {
        iterator.close();
      }

    } catch (Throwable e) {}
  }
}
