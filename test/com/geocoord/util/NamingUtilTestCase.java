package com.geocoord.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.gwt.benchmarks.client.Teardown;

import junit.framework.TestCase;

public class NamingUtilTestCase {

  @Test
  public void testIsUUID() {
    Assert.assertTrue(NamingUtil.isUUID(UUID.randomUUID().toString()));
  }
  
  @Test
  public void testIsValidLayerName() {
    // Check total len < 255
    Assert.assertFalse(NamingUtil.isValidLayerName("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

    // Check atom len < 64
    Assert.assertFalse(NamingUtil.isValidLayerName("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

    // Check start/end with a-z or 0-9
    Assert.assertFalse(NamingUtil.isValidLayerName("-abc"));
    Assert.assertFalse(NamingUtil.isValidLayerName("abc-"));
    
    // Check no digitis only labels
    //Assert.assertFalse(NamingUtil.isValidLayerName("0000"));
    
    // No end dot
    Assert.assertFalse(NamingUtil.isValidLayerName("com.geoxp.layers."));
    
    Assert.assertTrue(NamingUtil.isValidLayerName("com.geoxp.layers"));
  }

  @Test
  public void testIsValidAtomName() {
    // Check total len < 255
    Assert.assertFalse(NamingUtil.isValidAtomName("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

    // Check atom len < 64
    Assert.assertFalse(NamingUtil.isValidAtomName("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

    // Check start/end with a-z or 0-9
    Assert.assertFalse(NamingUtil.isValidAtomName("-abc"));
    Assert.assertFalse(NamingUtil.isValidAtomName("abc-"));
    
    // Check no digitis only labels
    //Assert.assertFalse(NamingUtil.isValidLayerName("0000"));
    
    // No end dot
    Assert.assertFalse(NamingUtil.isValidAtomName("com.geoxp.layers."));
    
    Assert.assertTrue(NamingUtil.isValidAtomName("com.geoxp.layers"));
    
    // Dotless
    Assert.assertTrue(NamingUtil.isValidAtomName("foo"));
  }

  @Test
  public void testIsValidPublicAttributeName() {
    Assert.assertFalse(NamingUtil.isValidPublicAttributeName("0"));
    Assert.assertFalse(NamingUtil.isValidPublicAttributeName("-bar"));
    Assert.assertFalse(NamingUtil.isValidPublicAttributeName("FOO"));
    Assert.assertFalse(NamingUtil.isValidPublicAttributeName(".foo"));
    Assert.assertTrue(NamingUtil.isValidPublicAttributeName("abcdefghijklmnopqrstuvwxyz0123456789:.-"));
  }

  @Test
  public void testIsValidSystemAttributeName() {
    Assert.assertFalse(NamingUtil.isValidSystemAttributeName("0"));
    Assert.assertFalse(NamingUtil.isValidSystemAttributeName("FOO"));
    Assert.assertFalse(NamingUtil.isValidSystemAttributeName("type"));    
    Assert.assertFalse(NamingUtil.isValidSystemAttributeName(".0type"));    
    Assert.assertFalse(NamingUtil.isValidSystemAttributeName(".-type"));    
    Assert.assertFalse(NamingUtil.isValidSystemAttributeName("abcdefghijklmnopqrstuvwxyz0123456789-"));
    Assert.assertTrue(NamingUtil.isValidSystemAttributeName(".foo"));
    Assert.assertTrue(NamingUtil.isValidSystemAttributeName(".abcdefghijklmnopqrstuvwxyz0123456789-"));
  }
  
  @Test
  public void testDoubleFNV() {    
    // Compute double FNV of a palindrom, will lead two exact same hashes.
    byte[] dblfnv = NamingUtil.getDoubleFNV("aaaaaaaa");
    
    // Check that
    for (int i = 0; i < 8; i++) {
      Assert.assertEquals(dblfnv[i], dblfnv[i + 8]);
    }
    
    dblfnv = NamingUtil.getDoubleFNV("chongo <Landon Curt Noll> /\\../\\");
    
    ByteBuffer bb = ByteBuffer.wrap(dblfnv);
    bb.order(ByteOrder.BIG_ENDIAN);
    bb.rewind();
    
    Assert.assertEquals(0x2c8f4c9af81bcf06L, bb.getLong());
    Assert.assertEquals(0x893818ffc476fc18L, bb.getLong());

    dblfnv = NamingUtil.getDoubleFNV("\\/..\\/ >lloN truC nodnaL< ognohc");
    
    bb = ByteBuffer.wrap(dblfnv);
    bb.order(ByteOrder.BIG_ENDIAN);
    bb.rewind();
    
    Assert.assertEquals(0x893818ffc476fc18L, bb.getLong());
    Assert.assertEquals(0x2c8f4c9af81bcf06L, bb.getLong());
  }
  
  @Test
  public void testLayerAtomName() {
    Assert.assertEquals("layer!atom", NamingUtil.getLayerAtomName("layer", "atom"));
  }
}
