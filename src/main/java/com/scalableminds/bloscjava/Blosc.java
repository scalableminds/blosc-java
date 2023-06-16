package com.scalableminds.bloscjava;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * A Java library for the Blosc codec. Wraps around the <a href="https://github.com/Blosc/c-blosc">c-blosc library</a> with JNI.
 */
public class Blosc {
    static {
        try {
            System.load(loadLibraryFromJarToTemp().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load libbloscjni.");
        }
    }

    // Disable constructor
    private Blosc() {
    }

    /**
     * Compresses a byte array with Blosc.
     *
     * @param src             Byte array to be compressed.
     * @param typeSize        Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for int32).
     * @param compressor      Compression algorithm. Available choices: LZ4, LZ4HC, BLOSCLZ, ZSTD<, ZLIB.
     * @param compressorLevel Number from 0 to 9 (0 = little compression, 9 = strongest compression).
     * @param shuffle         Whether to use shuffling. Available choices: NOSHUFFLE, BIT_SHUFFLE, BYTE_SHUFFLE. Recommended:
     *                        BIT_SHUFFLE for typeSize == 1, BYTE_SHUFFLE otherwise.
     * @param blockSize       Requested size of compressed blocks. Use 0 for automatic block sizes.
     * @param numThreads      Number of threads to be used internally.
     * @return the compressed byte array
     */
    public static byte[] compress(byte[] src, int typeSize, Compressor compressor, int compressorLevel, Shuffle shuffle, int blockSize, int numThreads) {
        return _compress(src, typeSize, compressorLevel, shuffle.shuffle, blockSize, compressor.compressor, numThreads);
    }

    /**
     * Compresses a byte array with Blosc.
     *
     * @param src             Byte array to be compressed.
     * @param typeSize        Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for int32).
     * @param compressor      Compression algorithm. Available choices: LZ4, LZ4HC, BLOSCLZ, ZSTD<, ZLIB.
     * @param compressorLevel Number from 0 to 9 (0 = little compression, 9 = strongest compression).
     * @param shuffle         Whether to use shuffling. Available choices: NOSHUFFLE, BIT_SHUFFLE, BYTE_SHUFFLE. Recommended:
     *                        BIT_SHUFFLE for typeSize == 1, BYTE_SHUFFLE otherwise.
     * @param blockSize       Requested size of compressed blocks. Use 0 for automatic block sizes.
     * @return the compressed byte array
     */
    public static byte[] compress(byte[] src, int typeSize, Compressor compressor, int compressorLevel, Shuffle shuffle, int blockSize) {
        return compress(src, typeSize, compressor, compressorLevel, shuffle, blockSize, 1);
    }

    /**
     * Compresses a byte array with Blosc with default settings.
     *
     * @param src             Byte array to be compressed.
     * @param typeSize        Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for int32).
     * @param compressor      Compression algorithm. Available choices: Available choices: LZ4, LZ4HC, BLOSCLZ,
     *                        ZSTD<, ZLIB.
     * @param compressorLevel Number from 0 to 9 (0 = little compression, 9 = strongest compression).
     * @param shuffle         Whether to use shuffling. Available choices: NOSHUFFLE, BIT_SHUFFLE, BYTE_SHUFFLE. Recommended:
     *                        BIT_SHUFFLE for typeSize == 1, BYTE_SHUFFLE otherwise.
     * @return the compressed byte array
     */
    public static byte[] compress(byte[] src, int typeSize, Compressor compressor, int compressorLevel, Shuffle shuffle) {
        return compress(src, typeSize, compressor, compressorLevel, shuffle, 0);
    }

    /**
     * Compresses a byte array with Blosc with default settings.
     *
     * @param src             Byte array to be compressed.
     * @param typeSize        Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for int32).
     * @param compressor      Compression algorithm. Available choices: LZ4, LZ4HC, BLOSCLZ, ZSTD<, ZLIB.
     * @param compressorLevel Number from 0 to 9 (0 = little compression, 9 = strongest compression).
     * @return the compressed byte array
     */
    public static byte[] compress(byte[] src, int typeSize, Compressor compressor, int compressorLevel) {
        return compress(src, typeSize, compressor, compressorLevel,
                typeSize == 1 ? Shuffle.BIT_SHUFFLE : Shuffle.BYTE_SHUFFLE);
    }

    /**
     * Compresses a byte array with Blosc with default settings.
     *
     * @param src        Byte array to be compressed.
     * @param typeSize   Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for int32).
     * @param compressor Compression algorithm. Available choices: LZ4, LZ4HC, BLOSCLZ, ZSTD<, ZLIB.
     * @return the compressed byte array
     */
    public static byte[] compress(byte[] src, int typeSize, Compressor compressor) {
        return compress(src, typeSize, compressor, 5);
    }

    /**
     * Compresses a byte array with Blosc with default settings (ZSTD compression).
     *
     * @param src      Byte array to be compressed.
     * @param typeSize Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for int32).
     * @return the compressed byte array
     */
    public static byte[] compress(byte[] src, int typeSize) {
        return compress(src, typeSize, Compressor.ZSTD);
    }

    /**
     * Decompresses a byte array with Blosc.
     *
     * @param src        Byte array to be decompressed.
     * @param numThreads Number of threads to be used internally.
     * @return the decompressed byte array
     */
    public static byte[] decompress(byte[] src, int numThreads) {
        return _decompress(src, numThreads);
    }

    /**
     * Decompresses a byte array with Blosc.
     *
     * @param src Byte array to be decompressed.
     * @return the decompressed byte array
     */
    public static byte[] decompress(byte[] src) {
        return decompress(src, 1);
    }

    private static native byte[] _compress(byte[] src, int typesize, int clevel, int shuffle, int blocksize, String cname, int numinternalthreads);

    private static native byte[] _decompress(byte[] src, int numinternalthreads);


    private static File loadLibraryFromJarToTemp() throws IOException {
        final String os = System.getProperty("os.name").toLowerCase();
        final String filePrefix = "libbloscjni";
        InputStream is = null;
        try {
            String extension = ".so";
            if (os.contains("mac")) extension = ".dylib";
            if (os.contains("win")) extension = ".dll";

            // attempt to look up the static library in the jar file
            String libraryFileName = filePrefix + extension;
            is = Blosc.class.getClassLoader().getResourceAsStream(libraryFileName);

            if (is == null) {
                throw new RuntimeException(libraryFileName + " was not found inside JAR.");
            }

            final File temp = File.createTempFile(filePrefix, extension);
            if (temp.exists()) {
                temp.deleteOnExit();
            } else {
                throw new RuntimeException("File " + temp.getAbsolutePath() + " does not exist.");
            }

            // copy the library from the Jar file to the temp destination
            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return temp;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Configures the internal compression of the Blosc codec.
     */
    public enum Compressor {
        /**
         * <a href="http://www.lz4.org/">LZ4 compression</a>
         */
        LZ4("lz4"),
        /**
         * High compression variant of <a href="http://www.lz4.org/">LZ4 compression</a>
         */
        LZ4HC("lz4hc"),
        /**
         * Compression based on <a href="https://ariya.github.io/FastLZ/">FastLZ</a>
         */
        BLOSCLZ("blosclz"),
        /**
         * <a href="https://facebook.github.io/zstd/">Zstandard compression</a>
         */
        ZSTD("zstd"),
        /**
         * <a href="https://zlib.net/">Zlib compression</a>
         */
        ZLIB("zlib");
        private final String compressor;

        Compressor(String compressor) {
            this.compressor = compressor;
        }

        public static Compressor fromString(String compressor) {
            for (Compressor e : values()) {
                if (e.compressor.equals(compressor)) {
                    return e;
                }
            }
            return null;
        }

        public String getValue() {
            return compressor;
        }
    }

    /**
     * Configures the shuffling for the Blosc codec.
     */
    public enum Shuffle {
        /**
         * Disable shuffling
         */
        NO_SHUFFLE(0),
        /**
         * Byte-wise shuffling
         */
        BYTE_SHUFFLE(1),
        /**
         * Bit-wise shuffling
         */
        BIT_SHUFFLE(2);
        private final int shuffle;

        Shuffle(int shuffle) {
            this.shuffle = shuffle;
        }

        public static Shuffle fromInt(int shuffle) {
            for (Shuffle e : values()) {
                if (e.shuffle == shuffle) {
                    return e;
                }
            }
            return null;
        }

        public int getValue() {
            return shuffle;
        }
    }
}