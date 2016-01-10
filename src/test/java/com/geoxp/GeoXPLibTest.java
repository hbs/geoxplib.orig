//
//  GeoXP Lib, library for efficient geo data manipulation
//
//  Copyright (C) 1999-2016  Mathias Herberts
//
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Affero General Public License as
//  published by the Free Software Foundation, either version 3 of the
//  License, or (at your option) any later version and under the terms
//  of the GeoXP License Exception.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//

package com.geoxp;

import org.junit.Assert;
import org.junit.Test;

public class GeoXPLibTest {
  @Test
  public void testBytesFromGeoXPPoint() {
    long hhcode = 0x1234567897abcdefL;
    
    byte[] bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 2);
    Assert.assertEquals(1, bytes.length);
    Assert.assertEquals(0x10, bytes[0]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 4);
    Assert.assertEquals(1, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 6);
    Assert.assertEquals(2, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x30, bytes[1]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 8);
    Assert.assertEquals(2, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 10);
    Assert.assertEquals(3, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x50, bytes[2]);    

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 12);
    Assert.assertEquals(3, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 14);
    Assert.assertEquals(4, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x70, bytes[3]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 16);
    Assert.assertEquals(4, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 18);
    Assert.assertEquals(5, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x90, bytes[4]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 20);
    Assert.assertEquals(5, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 22);
    Assert.assertEquals(6, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xa0, bytes[5]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 24);
    Assert.assertEquals(6, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 26);
    Assert.assertEquals(7, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);
    Assert.assertEquals((byte) 0xc0, bytes[6]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 28);
    Assert.assertEquals(7, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);
    Assert.assertEquals((byte) 0xcd, bytes[6]);
    
    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 30);
    Assert.assertEquals(8, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);
    Assert.assertEquals((byte) 0xcd, bytes[6]);
    Assert.assertEquals((byte) 0xe0, bytes[7]);

    bytes = GeoXPLib.bytesFromGeoXPPoint(hhcode, 32);
    Assert.assertEquals(8, bytes.length);
    Assert.assertEquals(0x12, bytes[0]);
    Assert.assertEquals(0x34, bytes[1]);
    Assert.assertEquals(0x56, bytes[2]);
    Assert.assertEquals(0x78, bytes[3]);
    Assert.assertEquals((byte) 0x97, bytes[4]);
    Assert.assertEquals((byte) 0xab, bytes[5]);
    Assert.assertEquals((byte) 0xcd, bytes[6]);
    Assert.assertEquals((byte) 0xef, bytes[7]);

    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 0));
    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 1));
    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 3));
    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 33));
    Assert.assertNull(GeoXPLib.bytesFromGeoXPPoint(hhcode, 34));
  }
}
