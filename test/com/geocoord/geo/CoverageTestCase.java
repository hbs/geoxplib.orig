package com.geocoord.geo;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;

import org.junit.Test;

import com.bbn.openmap.util.propertyEditor.FilePropertyEditor;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CoverageTestCase extends TestCase {

  @Test
  public void testOptimize_Thresholds() {
    Coverage coverage = new Coverage();
    coverage.addCell(32, 0x1L);
    
    //
    // Optimize coverage with a clustering threshold of 1 for resolution 32, i.e.
    // if one cell of resolution 32 is found, replace it by its enclosing cell at
    // resolution 30.
    //
    
    coverage.optimize(0x00000000000000001L);
    
    //
    // The optimized coverage should have 1 cell at resolution 30 (0) and
    // none at resolution 32.
    //
    
    assertEquals("000000000000000", coverage.toString());
  }

  @Test
  public void testOptimize_Threshold() {
    
    Coverage coverage = new Coverage();
    
    coverage.addCell(4, 0xa000000000000000L);
    coverage.addCell(4, 0xa100000000000000L);
    coverage.addCell(4, 0xa200000000000000L);
    coverage.addCell(4, 0xa300000000000000L);
    
    int hashcode = coverage.hashCode();
    
    // Threshold of 5, the coverage should not be modified.
    coverage.optimize(0x0500000000000000L);
    assertEquals(hashcode, coverage.hashCode());

    // Threshold of 4, the coverage should be modified
    coverage.optimize(0x0400000000000000L);

    assertEquals("a", coverage.toString());
  }

  @Test
  public void testOptimize_CleanUp() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(2,0xa000000000000000L);
    coverage.addCell(6,0xa000000000000000L);
    coverage.addCell(6,0xa010000000000000L);
    coverage.addCell(6,0xa020000000000000L);
    coverage.addCell(6,0xa030000000000000L);

    // Threshold of 5, the coverage should not be modified.
    coverage.optimize(0x0500000000000000L);

    assertEquals("a", coverage.toString());
  }

  @Test
  public void testPrune() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(4, 0x1100000000000000L);
    coverage.addCell(4, 0x1200000000000000L);
    coverage.addCell(4, 0x1300000000000000L);
    coverage.addCell(4, 0x1400000000000000L);
    coverage.addCell(4, 0x1500000000000000L);
    
    //
    // Check that when above the threshold cells are kept
    //
    
    coverage.prune(0x0400000000000000L, 2, 0);
    
    Assert.assertEquals(5, coverage.getCellCount());
    
    //
    // Check that minresolution overrides the threshold
    //
    
    coverage.prune(0x0500000000000000L, 4, 0);
    
    Assert.assertEquals(5, coverage.getCellCount());
    
    //
    // Check that when at or below the threshold, cells are removed
    //
    
    coverage.prune(0x0500000000000000L, 2, 0);
    
    Assert.assertEquals(0, coverage.getCellCount());
  }
  
  @Test
  public void testArea() {
    Coverage coverage = new Coverage();
    
    assertEquals(0L, coverage.area());
    
    coverage.addCell(2, 0x0L);
    
    assertEquals(0x0800000000000000L, coverage.area());

    coverage.addCell(2, 0x1000000000000000L);
    coverage.addCell(2, 0x2000000000000000L);
    coverage.addCell(2, 0x3000000000000000L);
    coverage.addCell(2, 0x4000000000000000L);
    coverage.addCell(2, 0x5000000000000000L);
    coverage.addCell(2, 0x6000000000000000L);
    coverage.addCell(2, 0x7000000000000000L);
    coverage.addCell(2, 0x8000000000000000L);
    
    assertEquals(0x4800000000000000L, coverage.area());
    
    coverage.addCell(32, 0x8000000000000000L);
    assertEquals(0x4800000000000000L, coverage.area());

    coverage.addCell(30, 0x8000000000000000L);
    assertEquals(0x4800000000000008L, coverage.area());

    coverage.addCell(28, 0x8000000000000000L);
    assertEquals(0x4800000000000088L, coverage.area());

    coverage.addCell(26, 0x8000000000000000L);
    assertEquals(0x4800000000000888L, coverage.area());

    coverage.addCell(24, 0x8000000000000000L);
    assertEquals(0x4800000000008888L, coverage.area());

    coverage.addCell(22, 0x8000000000000000L);
    assertEquals(0x4800000000088888L, coverage.area());

    coverage.addCell(20, 0x8000000000000000L);
    assertEquals(0x4800000000888888L, coverage.area());

    coverage.addCell(18, 0x8000000000000000L);
    assertEquals(0x4800000008888888L, coverage.area());

    coverage.addCell(16, 0x8000000000000000L);
    assertEquals(0x4800000088888888L, coverage.area());

    coverage.addCell(14, 0x8000000000000000L);
    assertEquals(0x4800000888888888L, coverage.area());

    coverage.addCell(12, 0x8000000000000000L);
    assertEquals(0x4800008888888888L, coverage.area());

    coverage.addCell(10, 0x8000000000000000L);
    assertEquals(0x4800088888888888L, coverage.area());

    coverage.addCell(8, 0x8000000000000000L);
    assertEquals(0x4800888888888888L, coverage.area());

    coverage.addCell(6, 0x8000000000000000L);
    assertEquals(0x4808888888888888L, coverage.area());

    coverage.addCell(4, 0x8000000000000000L);
    assertEquals(0x4888888888888888L, coverage.area());
  }
  
  @Test
  public void testToString() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(6, 0x0L);
    
    coverage.addCell(2, 0x1000000000000000L);
    coverage.addCell(2, 0x2000000000000000L);
    coverage.addCell(2, 0x3000000000000000L);
    coverage.addCell(2, 0x4000000000000000L);
    coverage.addCell(2, 0x5000000000000000L);
    coverage.addCell(2, 0x6000000000000000L);
    coverage.addCell(2, 0x7000000000000000L);
    coverage.addCell(2, 0x8000000000000000L);
    
    coverage.addCell(32, 0x8000000000000000L);
    coverage.addCell(30, 0x8000000000000000L);
    coverage.addCell(28, 0x8000000000000000L);
    coverage.addCell(26, 0x8000000000000000L);
    coverage.addCell(24, 0x8000000000000000L);
    coverage.addCell(22, 0x8000000000000000L);
    coverage.addCell(20, 0x8000000000000000L);
    coverage.addCell(18, 0x8000000000000000L);
    coverage.addCell(16, 0x8000000000000000L);
    coverage.addCell(14, 0x8000000000000000L);
    coverage.addCell(12, 0x8000000000000000L);
    coverage.addCell(10, 0x8000000000000000L);
    coverage.addCell(8, 0x8000000000000000L);
    coverage.addCell(6, 0x8000000000000000L);
    coverage.addCell(4, 0x8000000000000000L);
    
    Assert.assertEquals("8 1 2 3 4 5 6 7 80 800 000 8000 80000 800000 8000000 80000000 800000000 8000000000 80000000000 800000000000 8000000000000 80000000000000 800000000000000 8000000000000000", coverage.toString());
  }
  
  @Test
  public void testToGeoCells() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(2, 0x1234567890abcdefL);
    
    long[] geocells = coverage.toGeoCells(32);
    
    Assert.assertEquals(1, geocells.length);
    Assert.assertEquals(0x1100000000000000L, geocells[0]);

    coverage.addCell(4, 0x1234567890abcdefL);
    
    geocells = coverage.toGeoCells(32);
    
    Assert.assertEquals(2, geocells.length);
    Assert.assertEquals(0x1100000000000000L, geocells[0]);
    Assert.assertEquals(0x2120000000000000L, geocells[1]);

    coverage.addCell(6, 0x1234567890abcdefL);
    
    geocells = coverage.toGeoCells(32);
    
    Assert.assertEquals(3, geocells.length);
    Assert.assertEquals(0x1100000000000000L, geocells[0]);
    Assert.assertEquals(0x2120000000000000L, geocells[1]);
    Assert.assertEquals(0x3123000000000000L, geocells[2]);

    coverage.addCell(8, 0x1234567890abcdefL);
    
    geocells = coverage.toGeoCells(32);
    
    Assert.assertEquals(4, geocells.length);
    Assert.assertEquals(0x1100000000000000L, geocells[0]);
    Assert.assertEquals(0x2120000000000000L, geocells[1]);
    Assert.assertEquals(0x3123000000000000L, geocells[2]);
    Assert.assertEquals(0x4123400000000000L, geocells[3]);

    coverage.addCell(10, 0x1234567890abcdefL);
    
    geocells = coverage.toGeoCells(32);
    
    Assert.assertEquals(5, geocells.length);
    Assert.assertEquals(0x1100000000000000L, geocells[0]);
    Assert.assertEquals(0x2120000000000000L, geocells[1]);
    Assert.assertEquals(0x3123000000000000L, geocells[2]);
    Assert.assertEquals(0x4123400000000000L, geocells[3]);
    Assert.assertEquals(0x5123450000000000L, geocells[4]);

    coverage.addCell(12, 0x1234567890abcdefL);
    
    geocells = coverage.toGeoCells(32);
    
    Assert.assertEquals(6, geocells.length);
    Assert.assertEquals(0x1100000000000000L, geocells[0]);
    Assert.assertEquals(0x2120000000000000L, geocells[1]);
    Assert.assertEquals(0x3123000000000000L, geocells[2]);
    Assert.assertEquals(0x4123400000000000L, geocells[3]);
    Assert.assertEquals(0x5123450000000000L, geocells[4]);
    Assert.assertEquals(0x6123456000000000L, geocells[5]);

    //
    // Now change the finest resolution
    //
    
    geocells = coverage.toGeoCells(10);
    
    Assert.assertEquals(5, geocells.length);
    Assert.assertEquals(0x1100000000000000L, geocells[0]);
    Assert.assertEquals(0x2120000000000000L, geocells[1]);
    Assert.assertEquals(0x3123000000000000L, geocells[2]);
    Assert.assertEquals(0x4123400000000000L, geocells[3]);
    Assert.assertEquals(0x5123450000000000L, geocells[4]);

    geocells = coverage.toGeoCells(8);
    
    Assert.assertEquals(4, geocells.length);
    Assert.assertEquals(0x1100000000000000L, geocells[0]);
    Assert.assertEquals(0x2120000000000000L, geocells[1]);
    Assert.assertEquals(0x3123000000000000L, geocells[2]);
    Assert.assertEquals(0x4123400000000000L, geocells[3]);
    
    //
    // Add another cell at an existing resolution
    //
    
    coverage.addCell(2, 0xfedcba0987654321L);
    
    geocells = coverage.toGeoCells(2);
    
    Assert.assertEquals(2, geocells.length);

    Assert.assertTrue(geocells[0] != geocells[1]);
    Assert.assertTrue(0x1100000000000000L == geocells[0] || 0x1f00000000000000L == geocells[0]);
    Assert.assertTrue(0x1100000000000000L == geocells[1] || 0x1f00000000000000L == geocells[1]);    
  }
  
  @Test
  public void testNormalize_Expand() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(2, 0xf000000000000000L);
    
    coverage.normalize(4);
    
    assertEquals(1, coverage.getResolutions().size());
    assertTrue(coverage.getResolutions().contains(4));
    assertEquals("fe fc fa f8 f6 f4 f2 f0 ff fd fb f9 f7 f5 f3 f1", coverage.toString());
  }

  @Test
  public void testNormalize_Compact() {
    Coverage coverage = new Coverage();
    
    coverage.addCell(2, 0xf000000000000000L);
    coverage.addCell(32,0xf000000000000001L);
    coverage.addCell(30,0xf000000000000010L);
    
    coverage.normalize(4);
    
    assertEquals(1, coverage.getResolutions().size());
    assertTrue(coverage.getResolutions().contains(4));
    assertEquals("fe fc fa f8 f6 f4 f2 f0 ff fd fb f9 f7 f5 f3 f1", coverage.toString());
  }

  @Test
  public void testClone() {
    Coverage coverage = new Coverage();
    coverage.addCell(2,0);
    coverage.normalize(6);
    
    Coverage clone = coverage.deepCopy();
    
    assertEquals(clone.toString(),coverage.toString());
    coverage.removeCell(6, 0L);
    assertNotSame(clone.toString(),coverage.toString());
  }
  
  @Test
  public void testMinus_Normalize() {
    Coverage a = new Coverage();
    a.addCell(2, 0);
    int hca = a.hashCode();
    
    Coverage b = new Coverage();
    b.addCell(4, 0);
    int hcb = b.hashCode();
    
    Coverage c = Coverage.minus(a, b);
    
    // Check that a and b were not altered
    Assert.assertEquals(hca, a.hashCode());
    Assert.assertEquals(hcb, b.hashCode());
    
    Assert.assertEquals("02 04 06 08 0a 0c 0e 01 03 05 07 09 0b 0d 0f", c.toString());

    b = new Coverage();
    b.addCell(12, 0);
    
    c = Coverage.minus_normalize(a, b);
    
    // Ensure that the resolution of c is 6
    Assert.assertEquals(1,c.getResolutions().size());
    Assert.assertTrue(c.getResolutions().contains(6));
    
    // Check that c does not contain 000
    Assert.assertEquals(255, c.getCellCount());
    Assert.assertTrue(-1 == c.toString().indexOf("000"));
  }
  
  @Test
  public void testMinus() {
    Coverage a = new Coverage();
    a.addCell(2, 0);
    int hca = a.hashCode();
    
    Coverage b = new Coverage();
    b.addCell(4, 0);
    int hcb = b.hashCode();
    
    Coverage c = Coverage.minus(a, b);
    
    // Check that a and b were not altered
    Assert.assertEquals(hca, a.hashCode());
    Assert.assertEquals(hcb, b.hashCode());
    
    Assert.assertEquals("02 04 06 08 0a 0c 0e 01 03 05 07 09 0b 0d 0f", c.toString());

    b = new Coverage();
    b.addCell(12, 0);
    
    c = Coverage.minus(a, b);
    
    Assert.assertEquals(5,c.getResolutions().size());
    Assert.assertTrue(c.getResolutions().contains(6));
    
    // Check that c does not contain 000
    Assert.assertEquals(75, c.getCellCount());
    //System.out.println("C >>> " + c.toString());    
    Assert.assertTrue(-1 == c.toString().indexOf(" 000 "));    
    Assert.assertFalse(c.toString().startsWith("000 "));    
    Assert.assertFalse(c.toString().endsWith("000"));    
  }
  
  @Test
  public void testIntersection_Normalize() {
    Coverage a = new Coverage();
    a.addCell(2, 0);
    int hca = a.hashCode();
    
    Coverage b = new Coverage();
    b.addCell(6, 0);
    int hcb = b.hashCode();

    Coverage c = Coverage.intersection_normalize(a, b);
    
    // Check that a and b were not altered
    Assert.assertEquals(hca, a.hashCode());
    Assert.assertEquals(hcb, b.hashCode());
    
    Assert.assertEquals(1, c.getCellCount());
    Assert.assertFalse(-1 == c.toString().indexOf("000"));
    
    b = new Coverage();
    b.addCell(12, 0);
    
    c = Coverage.intersection_normalize(a, b);
    
    // Ensure that the resolution of c is 6
    Assert.assertTrue(1 == c.getResolutions().size());
    Assert.assertTrue(c.getResolutions().contains(6));
    
    // Check that c does contain only 000
    Assert.assertEquals(1, c.getCellCount());
    Assert.assertFalse(-1 == c.toString().indexOf("000"));

    //
    // Now check empty intersection
    //
    
    b = new Coverage();
    b.addCell(2, 0xf000000000000000L);
    
    c = Coverage.intersection_normalize(a, b);
    
    Assert.assertEquals(0, c.getCellCount());    
  }

  @Test
  public void testIntersection() {
    Coverage a = new Coverage();
    a.addCell(2, 0);
    int hca = a.hashCode();
    
    Coverage b = new Coverage();
    b.addCell(6, 0);
    int hcb = b.hashCode();

    Coverage c = Coverage.intersection(a, b);
    
    // Check that a and b were not altered
    Assert.assertEquals(hca, a.hashCode());
    Assert.assertEquals(hcb, b.hashCode());
    
    Assert.assertEquals(1, c.getCellCount());
    Assert.assertFalse(-1 == c.toString().indexOf("000"));
    
    b = new Coverage();
    b.addCell(12, 0);
    
    c = Coverage.intersection(a, b);
    
    // Ensure that the resolution of c is 12
    Assert.assertEquals(1,c.getResolutions().size());
    Assert.assertTrue(c.getResolutions().contains(12));
    
    
    // Check that c does contain only 000000
    Assert.assertEquals(1, c.getCellCount());
    Assert.assertEquals("000000", c.toString());

    //
    // Now check empty intersection
    //
    
    b = new Coverage();
    b.addCell(2, 0xf000000000000000L);
    
    c = Coverage.intersection(a, b);
    
    Assert.assertEquals(0, c.getCellCount());    
  }

  @Test
  public void testDummy() {
    Coverage a = new Coverage();
    a.addCell(2,0);

    Coverage b = new Coverage();
    b.addCell(6,0);

    Coverage.minus(a, b);    
  }
  
  @Test
  public void testReduce() {
    Coverage a = new Coverage();
    a.addCell(2, 0);
    a.normalize(8);
    a.removeCell(8, 0);

    int count = a.getCellCount();
    
    while (count > 16) {
      a.reduce(count - 1);
      Assert.assertTrue(a.getCellCount() <= count);
      a.optimize(0L);
      count = a.getCellCount();
    }
  }
  
  @Test
  public void testContains() throws Exception {
    
    Coverage coverage = GeoParser.parseCircle("48.5:-4.5:1000", -4);
   
    coverage.optimize(0L);

    FileOutputStream fos = new FileOutputStream("/var/tmp/1000.kml");
    fos.write(CoverageHelper.toKML(coverage).getBytes());
    fos.close();
    
    
    long[] geocells = coverage.toGeoCells(30);

    for (long geocell: geocells) {
      System.out.printf("%16x\n", geocell);
    }
    //Arrays.sort(geocells);
    
    System.out.println("LEN=" + geocells.length);
    
    for (int i = 2; i < 32; i += 2) {
      long nano = System.nanoTime();
      boolean c = Coverage.contains(geocells, i, 0xb570facc3c000000L);
      nano = System.nanoTime() - nano;
      System.out.println("R=" + i + "   contains=" + c + "   nano=" + nano);
    }
    
    long nano = System.nanoTime();
    boolean c = Coverage.contains(geocells, 0xb570facc3c000000L);
    nano = System.nanoTime() - nano;
    System.out.println("contains=" + c + "   nano=" + nano);

  }
  
  //@Test
  public void testKMK() throws Exception {
    //Thread.sleep(25000);
    BufferedReader br = new BufferedReader(new FileReader("/Users/hbs/lab/OpenSkyMap/wpts.circle"));
    
    Coverage c = new Coverage();
    
    while (true) {
      String line = br.readLine();
      if (null == line) {
        break;
      }
      Coverage cc = GeoParser.parseCircle(line.replaceAll("^circle:",""), -4);
      cc.optimize(0);
      c.merge(cc);      
    }
    
    c.optimize(0);
    
    long[] geocells = c.toGeoCells(30);
    
    long hhcode = 0xe026001850000000L;
    
    for (int k = 0; k < 10; k++) {
    long nano = System.nanoTime();
    boolean cont = Coverage.contains(geocells, hhcode);
    nano = System.nanoTime() - nano;
    System.out.println("long[] contains=" + cont + "   nano=" + nano);
           
    nano = System.nanoTime();
    for (int i = 2; i < 32; i+=2) {
      cont = c.contains(i, hhcode);
      if (cont) {
        break;
      }
    }
    nano = System.nanoTime() - nano;
    System.out.println("Coverage contains=" + cont + "   nano=" + nano);
    }
    
    System.out.println(c.getCellCount());
    
    FileOutputStream fos = new FileOutputStream("/var/tmp/wpts.kml");
    OutputStreamWriter osw = new OutputStreamWriter(fos);
    CoverageHelper.toKML(c, osw, true);
    osw.close();
    //Thread.sleep(25000);
  }
  
  @Test
  public void testNormalize_Split() throws Exception {
    Coverage a = GeoParser.parseArea("circle:48.0:-4.5:5000", 0);
    Coverage b = GeoParser.parseArea("circle:48.0:-4.54:3000", -4);
    
    //a.optimize(0L);
    //b.optimize(0L);
    
    System.out.println(a.getCellCount() + " " + b.getCellCount());
    Coverage.normalize(a,b);
    System.out.println(a.getCellCount() + " " + b.getCellCount());
    
    PrintWriter out = new PrintWriter(new FileWriter("/var/tmp/a.kml"));
    CoverageHelper.toKML(a, out, true);
    out.close();
    
    out = new PrintWriter(new FileWriter("/var/tmp/b.kml"));
    CoverageHelper.toKML(b, out, true);
    out.close();
  }
  
  @Test
  public void testAutoOptimize() throws Exception {
    int resolution = -10;
    Coverage c;
    
    for (int i = 0; i < 100; i++) {
    long nano = System.nanoTime();    
    c = new Coverage();
    //c.setAutoDedup(true);
    c.setAutoThresholds(0L);
    GeoParser.parseCircle("48.0:-4.5:2000", resolution, c);
    c.dedup();
    //c.optimize(0L);
    nano = System.nanoTime() - nano;
    System.out.println("autoOptimize Cells=" + c.getCellCount() + " in " + (nano / 1000000.0) + " ms");
    Writer writer = new FileWriter("/var/tmp/cov1.kml");
    CoverageHelper.toKML(c, writer, true);
    writer.close();
    
    nano = System.nanoTime();
    c = new Coverage();
    //GeoParser.parseCircle("48.0:-4.5:2000", resolution, c);
    c.optimize(0L);
    nano = System.nanoTime() - nano;
    System.out.println("Cells=" + c.getCellCount() + " in " + (nano / 1000000.0) + " ms");
    }
  }
}
