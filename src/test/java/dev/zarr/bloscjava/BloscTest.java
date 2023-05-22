package dev.zarr.bloscjava;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class BloscTest {

    @Test
    public void testCompress() {
        byte[] buf = new byte[256];
        for (int i = 0; i < buf.length; i++) buf[i] = (byte) (i % 12);

        byte[] compBuf = Blosc.compress(buf, 1, Blosc.Compressor.ZSTD, 9);
        System.out.println(compBuf.length);

        byte[] decompBuf = Blosc.decompress(compBuf);

        System.out.println(Arrays.equals(buf, decompBuf));
        System.out.println(Arrays.equals(buf, compBuf));
        System.out.println(Arrays.toString(buf));
        System.out.println(Arrays.toString(compBuf));
        System.out.println(Arrays.toString(decompBuf));
    }

}
