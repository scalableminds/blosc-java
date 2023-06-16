package com.scalableminds.bloscjava;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class BloscTest {

    byte[] makeTestBytes() {
        byte[] buf = new byte[256];
        for (int i = 0; i < buf.length; i++) buf[i] = (byte) (i % 12);
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
        byte[] compBufByte = Blosc.compress(buf, 2, Blosc.Compressor.ZSTD, 5, Blosc.Shuffle.BYTE_SHUFFLE);
        assert Arrays.equals(buf, Blosc.decompress(compBufNo));
        assert Arrays.equals(buf, Blosc.decompress(compBufBit));
        assert Arrays.equals(buf, Blosc.decompress(compBufByte));
    }
}
