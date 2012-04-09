package com.geocoord.util;

import org.junit.Assert;
import org.junit.Test;

public class LayerUtilsTestCase {

  @Test
  public void testEncodeGeneration() {
    Assert.assertEquals("A", LayerUtils.encodeGeneration(0L));
    Assert.assertEquals("AAAAAAAAAAI", LayerUtils.encodeGeneration(0x8000000000000000L));
    Assert.assertEquals("//////////P", LayerUtils.encodeGeneration(0xffffffffffffffffL));
    Assert.assertEquals("B", LayerUtils.encodeGeneration(0x1L));
    Assert.assertEquals("/", LayerUtils.encodeGeneration(0x3fL));
    Assert.assertEquals("AB", LayerUtils.encodeGeneration(0x40L));
  }
}
