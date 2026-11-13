package com.scalableminds.bloscjava;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class Blosc2Test {

  private static byte[] floats(int n) {
    ByteBuffer b = ByteBuffer.allocate(4 * n).order(ByteOrder.LITTLE_ENDIAN);
    Random random = new Random(1);
    for (int i = 0; i < n; i++) {
      b.putFloat((float) (Math.sin(i / 50.0) * 100 + random.nextGaussian()));
    }
    return b.array();
  }

  private static byte[] ints(int n) {
    ByteBuffer b = ByteBuffer.allocate(4 * n).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < n; i++) {
      b.putInt(1000 + i / 3);
    }
    return b.array();
  }

  @Test
  public void testDefaultsRoundTripAndAreTheBlosc2Format() {
    byte[] buf = ints(10000);
    byte[] compBuf = Blosc2.compress(buf, 4);
    assertNotEquals(2, compBuf[0], "Blosc2's chunks are not those of Blosc 1.x");
    assertArrayEquals(buf, Blosc2.decompress(compBuf));
    assertArrayEquals(buf, Blosc.decompress(compBuf));
    assertTrue(compBuf.length < buf.length / 4);
  }

  @Test
  public void testEveryFilterAndPipelineRoundTripsExceptTheLossyOne() {
    byte[] buf = ints(100003);
    for (Blosc.Compressor compressor : Blosc.Compressor.values()) {
      for (Blosc2.FilterSpec[] pipeline : new Blosc2.FilterSpec[][] {
          {},
          {new Blosc2.FilterSpec(Blosc2.Filter.BYTE_SHUFFLE)},
          {new Blosc2.FilterSpec(Blosc2.Filter.BIT_SHUFFLE)},
          {new Blosc2.FilterSpec(Blosc2.Filter.DELTA), new Blosc2.FilterSpec(Blosc2.Filter.BYTE_SHUFFLE)},
          {new Blosc2.FilterSpec(Blosc2.Filter.DELTA)}}) {
        Blosc2.Options options = Blosc2.Options.builder(4).compressor(compressor).filters(pipeline).numThreads(2).build();
        assertArrayEquals(buf, Blosc2.decompress(Blosc2.compress(buf, options), 2), compressor + " " + pipeline.length);
      }
    }
  }

  @Test
  public void testTruncatedPrecisionKeepsTheBitsItWasAskedTo() {
    byte[] buf = floats(100000);
    int bits = 10;
    Blosc2.Options lossy = Blosc2.Options.builder(4)
        .filters(new Blosc2.FilterSpec(Blosc2.Filter.TRUNC_PREC, bits), new Blosc2.FilterSpec(Blosc2.Filter.BYTE_SHUFFLE)).build();
    Blosc2.Options exact = Blosc2.Options.builder(4).build();
    byte[] compLossy = Blosc2.compress(buf, lossy);
    assertTrue(compLossy.length < Blosc2.compress(buf, exact).length, "fewer bits compress better");
    ByteBuffer in = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
    ByteBuffer out = ByteBuffer.wrap(Blosc2.decompress(compLossy)).order(ByteOrder.LITTLE_ENDIAN);
    int different = 0;
    for (int i = 0; i < 100000; i++) {
      float a = in.getFloat(), b = out.getFloat();
      if (a != b) {
        different++;
      }
      assertTrue(Math.abs(a - b) <= Math.abs(a) * Math.pow(2, -bits + 1), a + " " + b);
    }
    assertTrue(different > 50000, "most values lose their low bits");
  }

  @Test
  public void testOptionsAreChecked() {
    assertThrows(IllegalArgumentException.class, () -> Blosc2.Options.builder(0));
    assertThrows(IllegalArgumentException.class, () -> Blosc2.Options.builder(4).compressorLevel(10));
    assertThrows(IllegalArgumentException.class, () -> Blosc2.Options.builder(4).filters(new Blosc2.FilterSpec[Blosc2.MAX_FILTERS + 1]));
    assertThrows(IllegalArgumentException.class, () -> new Blosc2.FilterSpec(Blosc2.Filter.TRUNC_PREC, 200));
    assertEquals(6, Blosc2.MAX_FILTERS);
    // delta garbles what follows the last whole element, so it is refused; shuffle is fine with a partial element
    Blosc2.Options delta = Blosc2.Options.builder(4).filters(new Blosc2.FilterSpec(Blosc2.Filter.DELTA)).build();
    assertThrows(IllegalArgumentException.class, () -> Blosc2.compress(new byte[4099], delta));
    assertArrayEquals(new byte[4099], Blosc2.decompress(Blosc2.compress(new byte[4099], 4)));
  }
}
