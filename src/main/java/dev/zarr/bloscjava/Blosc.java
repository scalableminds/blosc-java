package dev.zarr.bloscjava;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Blosc {
    private static final String OS = System.getProperty("os.name").toLowerCase();


    static {
        try {
            System.load(loadLibraryFromJarToTemp().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load libbloscjni.");
        }
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
        return compress(src, typeSize, compressor, compressorLevel,
                typeSize == 1 ? Shuffle.BIT_SHUFFLE : Shuffle.BYTE_SHUFFLE);
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


    private static File loadLibraryFromJarToTemp() throws IOException {
        final String filePrefix = "libbloscjni";
        InputStream is = null;
        try {
            String extension = ".so";
            if (OS.contains("mac")) extension = ".dylib";
            if (OS.contains("win")) extension = ".dll";

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

    public enum Compressor {
        LZ4("lz4"), LZ4HC("lz4hc"), BLOSCLZ("blosclz"), ZSTD("zstd"), SNAPPY("snappy"), ZLIB("zlib");
        private final String compressor;

        Compressor(String compressor) {
            this.compressor = compressor;
        }

        public String value() {
            return compressor;
        }
    }

    public enum Shuffle {
        NO_SHUFFLE(0), BYTE_SHUFFLE(1), BIT_SHUFFLE(2);
        private final int shuffle;

        Shuffle(int shuffle) {
            this.shuffle = shuffle;
        }

        public int value() {
            return shuffle;
        }
    }
}