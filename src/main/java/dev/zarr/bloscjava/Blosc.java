package dev.zarr.bloscjava;

public class Blosc {

    static {
        System.load(System.getProperty("user.dir") + "/bloscjni/libbloscjni.so");
    }

    public static byte[] compress(byte[] src, int typeSize, Compressor compressor, int compressorLevel, Shuffle shuffle, int blockSize, int numThreads) {
        return _compress(src, typeSize, compressorLevel, shuffle.value(), blockSize, compressor.value(), numThreads);
    }

    public static byte[] compress(byte[] src, int typeSize, Compressor compressor, int compressorLevel, Shuffle shuffle, int blockSize) {
        return compress(src, typeSize, compressor, compressorLevel, shuffle, blockSize, 1);
    }

    public static byte[] compress(byte[] src, int typeSize, Compressor compressor, int compressorLevel, Shuffle shuffle) {
        return compress(src, typeSize, compressor, compressorLevel, shuffle, 0);
    }

    public static byte[] compress(byte[] src, int typeSize, Compressor compressor, int compressorLevel) {
        return compress(src, typeSize, compressor, 5, typeSize == 1 ? Shuffle.BIT_SHUFFLE : Shuffle.BYTE_SHUFFLE);
    }

    public static byte[] compress(byte[] src, int typeSize, Compressor compressor) {
        return compress(src, typeSize, compressor, 5);
    }

    public static byte[] compress(byte[] src, int typeSize) {
        return compress(src, typeSize, Compressor.LZ4);
    }

    public static byte[] decompress(byte[] src, int numThreads) {
        return _decompress(src, numThreads);
    }

    public static byte[] decompress(byte[] src) {
        return decompress(src, 1);
    }

    private static native byte[] _compress(byte[] src, int typesize, int clevel, int shuffle, int blocksize, String cname, int numinternalthreads);

    private static native byte[] _decompress(byte[] src, int numinternalthreads);


    public static enum Compressor {
        LZ4("lz4"), LZ4HC("lz4hc"), BLOSCLZ("blosclz"), ZSTD("zstd"), SNAPPY("snappy"), ZLIB("zlib");
        private String compressor;

        Compressor(String compressor) {
            this.compressor = compressor;
        }

        public String value() {
            return compressor;
        }
    }

    public static enum Shuffle {
        NO_SHUFFLE(0), BYTE_SHUFFLE(1), BIT_SHUFFLE(2);
        private int shuffle;

        Shuffle(int shuffle) {
            this.shuffle = shuffle;
        }

        public int value() {
            return shuffle;
        }
    }
}