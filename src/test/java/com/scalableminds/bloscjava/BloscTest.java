package com.scalableminds.bloscjava;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;
import org.bytedeco.blosc2.global.blosc2;
import org.bytedeco.javacpp.BytePointer;
import org.junit.jupiter.api.Test;

public class BloscTest {

  byte[] makeTestBytes() {
    byte[] buf = new byte[256];
    for (int i = 0; i < buf.length; i++) {
      buf[i] = (byte) (i % 12);
    }
    return buf;
  }

  @Test
  public void testCompress() {
    byte[] buf = makeTestBytes();
    byte[] compBuf = Blosc.compress(buf, 1);
    byte[] decompBuf = Blosc.decompress(compBuf);
    assert Arrays.equals(buf, decompBuf);
  }

  @Test
  public void testCompressBlosclz() {
    byte[] buf = makeTestBytes();
    byte[] compBuf = Blosc.compress(buf, 1, Blosc.Compressor.BLOSCLZ);
    byte[] decompBuf = Blosc.decompress(compBuf);
    assert Arrays.equals(buf, decompBuf);
  }

  @Test
  public void testCompressLz4() {
    byte[] buf = makeTestBytes();
    byte[] compBuf = Blosc.compress(buf, 1, Blosc.Compressor.LZ4);
    byte[] decompBuf = Blosc.decompress(compBuf);
    assert Arrays.equals(buf, decompBuf);
  }

  @Test
  public void testCompressLz4hc() {
    byte[] buf = makeTestBytes();
    byte[] compBuf = Blosc.compress(buf, 1, Blosc.Compressor.LZ4HC);
    byte[] decompBuf = Blosc.decompress(compBuf);
    assert Arrays.equals(buf, decompBuf);
  }

  @Test
  public void testCompressZlib() {
    byte[] buf = makeTestBytes();
    byte[] compBuf = Blosc.compress(buf, 1, Blosc.Compressor.ZLIB);
    byte[] decompBuf = Blosc.decompress(compBuf);
    assert Arrays.equals(buf, decompBuf);
  }

  @Test
  public void testCompressZstd() {
    byte[] buf = makeTestBytes();
    byte[] compBuf = Blosc.compress(buf, 1, Blosc.Compressor.ZSTD);
    byte[] decompBuf = Blosc.decompress(compBuf);
    assert Arrays.equals(buf, decompBuf);
  }

  @Test
  public void testCompressZstdLevels() {
    byte[] buf = makeTestBytes();
    byte[] compBuf0 = Blosc.compress(buf, 1, Blosc.Compressor.ZSTD, 0);
    byte[] compBuf9 = Blosc.compress(buf, 1, Blosc.Compressor.ZSTD, 9);
    assert compBuf0.length > compBuf9.length;
    assert Arrays.equals(buf, Blosc.decompress(compBuf0));
    assert Arrays.equals(buf, Blosc.decompress(compBuf9));
  }

  @Test
  public void testCompressShuffle() {
    byte[] buf = makeTestBytes();
    byte[] compBufNo = Blosc.compress(buf, 1, Blosc.Compressor.ZSTD, 5, Blosc.Shuffle.NO_SHUFFLE);
    byte[] compBufBit = Blosc.compress(buf, 1, Blosc.Compressor.ZSTD, 5, Blosc.Shuffle.BIT_SHUFFLE);
    byte[] compBufByte = Blosc.compress(buf, 2, Blosc.Compressor.ZSTD, 5,
        Blosc.Shuffle.BYTE_SHUFFLE);
    assert Arrays.equals(buf, Blosc.decompress(compBufNo));
    assert Arrays.equals(buf, Blosc.decompress(compBufBit));
    assert Arrays.equals(buf, Blosc.decompress(compBufByte));
  }

  @Test
  public void testCompressedBuffersAreInTheBlosc1Format() {
    // C-Blosc 1.x reads only chunks with the format version 2 in their first byte; C-Blosc2 writes 5 unless asked
    for (Blosc.Compressor compressor : Blosc.Compressor.values()) {
      byte[] compBuf = Blosc.compress(makeTestBytes(), 1, compressor);
      assertEquals(2, compBuf[0], compressor.getValue());
    }
  }

  @Test
  public void testDecompressesBuffersOfBlosc2() {
    byte[] buf = makeTestBytes();
    BytePointer src = new BytePointer(buf.length);
    src.put(buf, 0, buf.length);
    BytePointer dest = new BytePointer(buf.length + blosc2.BLOSC2_MAX_OVERHEAD);
    int size = blosc2.blosc1_compress(5, 1, 1, buf.length, src, dest, dest.capacity());
    byte[] compBuf = new byte[size];
    dest.get(compBuf, 0, size);
    assert compBuf[0] != 2; // stock C-Blosc2 chunks are not Blosc 1.x chunks
    assertArrayEquals(buf, Blosc.decompress(compBuf));
  }

  @Test
  public void testRoundTripsManySizesTypesAndThreads() {
    Random random = new Random(0);
    for (int size : new int[] {0, 1, 7, 4096, 1000003}) {
      for (int typeSize : new int[] {1, 2, 4, 8}) {
        byte[] buf = new byte[size];
        for (int i = 0; i < size; i++) {
          buf[i] = (byte) (i % 5 == 0 ? random.nextInt() : i / typeSize / 16);
        }
        for (Blosc.Compressor compressor : Blosc.Compressor.values()) {
          for (Blosc.Shuffle shuffle : Blosc.Shuffle.values()) {
            byte[] compBuf = Blosc.compress(buf, typeSize, compressor, 5, shuffle, 0, 3);
            assertArrayEquals(buf, Blosc.decompress(compBuf, 3));
          }
        }
      }
    }
  }

  @Test
  public void testInvalidBuffersAreRefused() {
    assertThrows(RuntimeException.class, () -> Blosc.decompress(new byte[0]));
    assertThrows(RuntimeException.class, () -> Blosc.decompress(new byte[100]));
    byte[] compBuf = Blosc.compress(makeTestBytes(), 1);
    assertThrows(RuntimeException.class, () -> Blosc.decompress(Arrays.copyOf(compBuf, compBuf.length - 3)));
    assertThrows(IllegalArgumentException.class, () -> Blosc.compress(makeTestBytes(), 0));
  }
}
