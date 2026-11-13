package com.scalableminds.bloscjava;

import org.bytedeco.blosc2.blosc2_context;
import org.bytedeco.blosc2.blosc2_cparams;
import org.bytedeco.blosc2.blosc2_dparams;
import org.bytedeco.blosc2.global.blosc2;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.SizeTPointer;

/**
 * A Java library for the Blosc codec. Wraps around the <a href="https://github.com/Blosc/c-blosc2">C-Blosc2
 * library</a> through its <a href="https://github.com/bytedeco/javacpp-presets/tree/master/blosc2">JavaCPP
 * preset</a>, which provides the native libraries.
 * <p>
 * Compressed buffers are in the chunk format of Blosc 1.x, which C-Blosc 1.x, numcodecs and Zarr read.
 * Buffers of either format, Blosc 1.x or Blosc2, are decompressed.
 */
public class Blosc {

  static {
    blosc2.blosc2_init();
  }

  // Disable constructor
  private Blosc() {
  }

  /**
   * Compresses a byte array with Blosc.
   *
   * @param src             Byte array to be compressed.
   * @param typeSize        Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for
   *                        int32).
   * @param compressor      Compression algorithm. Available choices: LZ4, LZ4HC, BLOSCLZ, ZSTD,
   *                        ZLIB.
   * @param compressorLevel Number from 0 to 9 (0 = little compression, 9 = strongest compression).
   * @param shuffle         Whether to use shuffling. Available choices: NOSHUFFLE, BIT_SHUFFLE,
   *                        BYTE_SHUFFLE. Recommended: BIT_SHUFFLE for typeSize == 1, BYTE_SHUFFLE
   *                        otherwise.
   * @param blockSize       Requested size of compressed blocks. Use 0 for automatic block sizes.
   * @param numThreads      Number of threads to be used internally.
   * @return the compressed byte array
   */
  public static byte[] compress(byte[] src, int typeSize, Compressor compressor,
      int compressorLevel, Shuffle shuffle, int blockSize, int numThreads) {
    if (typeSize < 1) {
      throw new IllegalArgumentException("Argument typeSize needs to be larger than 0.");
    }
    return _compress(src, typeSize, compressorLevel, shuffle.shuffle, blockSize,
        compressor.compressor, numThreads);
  }

  /**
   * Compresses a byte array with Blosc.
   *
   * @param src             Byte array to be compressed.
   * @param typeSize        Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for
   *                        int32).
   * @param compressor      Compression algorithm. Available choices: LZ4, LZ4HC, BLOSCLZ, ZSTD,
   *                        ZLIB.
   * @param compressorLevel Number from 0 to 9 (0 = little compression, 9 = strongest compression).
   * @param shuffle         Whether to use shuffling. Available choices: NOSHUFFLE, BIT_SHUFFLE,
   *                        BYTE_SHUFFLE. Recommended: BIT_SHUFFLE for typeSize == 1, BYTE_SHUFFLE
   *                        otherwise.
   * @param blockSize       Requested size of compressed blocks. Use 0 for automatic block sizes.
   * @return the compressed byte array
   */
  public static byte[] compress(byte[] src, int typeSize, Compressor compressor,
      int compressorLevel, Shuffle shuffle, int blockSize) {
    return compress(src, typeSize, compressor, compressorLevel, shuffle, blockSize, 1);
  }

  /**
   * Compresses a byte array with Blosc with default settings.
   *
   * @param src             Byte array to be compressed.
   * @param typeSize        Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for
   *                        int32).
   * @param compressor      Compression algorithm. Available choices: Available choices: LZ4, LZ4HC,
   *                        BLOSCLZ, ZSTD, ZLIB.
   * @param compressorLevel Number from 0 to 9 (0 = little compression, 9 = strongest compression).
   * @param shuffle         Whether to use shuffling. Available choices: NOSHUFFLE, BIT_SHUFFLE,
   *                        BYTE_SHUFFLE. Recommended: BIT_SHUFFLE for typeSize == 1, BYTE_SHUFFLE
   *                        otherwise.
   * @return the compressed byte array
   */
  public static byte[] compress(byte[] src, int typeSize, Compressor compressor,
      int compressorLevel, Shuffle shuffle) {
    return compress(src, typeSize, compressor, compressorLevel, shuffle, 0);
  }

  /**
   * Compresses a byte array with Blosc with default settings.
   *
   * @param src             Byte array to be compressed.
   * @param typeSize        Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for
   *                        int32).
   * @param compressor      Compression algorithm. Available choices: LZ4, LZ4HC, BLOSCLZ, ZSTD,
   *                        ZLIB.
   * @param compressorLevel Number from 0 to 9 (0 = little compression, 9 = strongest compression).
   * @return the compressed byte array
   */
  public static byte[] compress(byte[] src, int typeSize, Compressor compressor,
      int compressorLevel) {
    return compress(src, typeSize, compressor, compressorLevel,
        typeSize == 1 ? Shuffle.BIT_SHUFFLE : Shuffle.BYTE_SHUFFLE);
  }

  /**
   * Compresses a byte array with Blosc with default settings.
   *
   * @param src        Byte array to be compressed.
   * @param typeSize   Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for
   *                   int32).
   * @param compressor Compression algorithm. Available choices: LZ4, LZ4HC, BLOSCLZ, ZSTD, ZLIB.
   * @return the compressed byte array
   */
  public static byte[] compress(byte[] src, int typeSize, Compressor compressor) {
    return compress(src, typeSize, compressor, 5);
  }

  /**
   * Compresses a byte array with Blosc with default settings (ZSTD compression).
   *
   * @param src      Byte array to be compressed.
   * @param typeSize Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for
   *                 int32).
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

  private static byte[] _compress(byte[] src, int typesize, int clevel, int shuffle, int blocksize,
      String cname, int numinternalthreads) {
    int compcode = blosc2.blosc2_compname_to_compcode(cname);
    if (compcode < 0) {
      throw new IllegalArgumentException("Unknown compressor " + cname);
    }
    blosc2_cparams cparams = new blosc2_cparams();
    cparams.compcode((byte) compcode);
    cparams.clevel((byte) clevel);
    cparams.typesize(typesize);
    cparams.nthreads((short) Math.max(1, numinternalthreads));
    cparams.blocksize(blocksize);
    cparams.splitmode(blosc2.BLOSC_FORWARD_COMPAT_SPLIT);
    // the shuffle is the last of the filters, as in Blosc 1.x
    byte filter = shuffle == Shuffle.BYTE_SHUFFLE.shuffle ? (byte) blosc2.BLOSC_SHUFFLE
        : shuffle == Shuffle.BIT_SHUFFLE.shuffle ? (byte) blosc2.BLOSC_BITSHUFFLE
            : (byte) blosc2.BLOSC_NOFILTER;
    cparams.filters(blosc2.BLOSC2_MAX_FILTERS - 1, filter);
    blosc2_context context = blosc2.blosc2_create_cctx(cparams);
    if (context == null || context.isNull()) {
      throw new RuntimeException("Error while compressing buffer");
    }
    // oversized, as the buffer of the JNI version was
    long destSize = (long) src.length + blosc2.BLOSC2_MAX_OVERHEAD;
    BytePointer srcPtr = new BytePointer(Math.max(1, src.length));
    BytePointer destPtr = new BytePointer(destSize);
    try {
      if (blosc2.blosc2_ctx_set_blosc1_format(context, true) < 0) {
        throw new RuntimeException("Error while compressing buffer");
      }
      srcPtr.put(src, 0, src.length);
      int realDestSize = blosc2.blosc2_compress_ctx(context, srcPtr, src.length, destPtr, (int) destSize);
      if (realDestSize < 0) {
        throw new RuntimeException("Error while compressing buffer");
      }
      byte[] dest = new byte[realDestSize];
      destPtr.get(dest, 0, realDestSize);
      return dest;
    } finally {
      blosc2.blosc2_free_ctx(context);
      srcPtr.deallocate();
      destPtr.deallocate();
    }
  }

  private static byte[] _decompress(byte[] src, int numinternalthreads) {
    BytePointer srcPtr = new BytePointer(Math.max(1, src.length));
    BytePointer destPtr = null;
    blosc2_context context = null;
    try {
      srcPtr.put(src, 0, src.length);
      // validates the buffer, and gets the size of the result from the Blosc header
      SizeTPointer nbytes = new SizeTPointer(1);
      if (blosc2.blosc1_cbuffer_validate(srcPtr, src.length, nbytes) < 0) {
        throw new RuntimeException("Buffer does not contain valid blosc-encoded contents");
      }
      long destSize = nbytes.get();
      if (destSize > Integer.MAX_VALUE - 8) {
        throw new OutOfMemoryError("Cannot allocate " + destSize + " bytes for decompressed buffer");
      }
      destPtr = new BytePointer(Math.max(1, destSize));
      blosc2_dparams dparams = new blosc2_dparams();
      dparams.nthreads((short) Math.max(1, numinternalthreads));
      context = blosc2.blosc2_create_dctx(dparams);
      if (context == null || context.isNull()) {
        throw new RuntimeException("Error while decompressing buffer");
      }
      int realDestSize = blosc2.blosc2_decompress_ctx(context, srcPtr, src.length, destPtr, (int) destSize);
      if (realDestSize < 0) {
        throw new RuntimeException("Error while decompressing buffer");
      }
      byte[] dest = new byte[realDestSize];
      destPtr.get(dest, 0, realDestSize);
      return dest;
    } finally {
      if (context != null && !context.isNull()) {
        blosc2.blosc2_free_ctx(context);
      }
      srcPtr.deallocate();
      if (destPtr != null) {
        destPtr.deallocate();
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
        if (e.compressor.equalsIgnoreCase(compressor)) {
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
    NO_SHUFFLE(0, "noshuffle"),
    /**
     * Byte-wise shuffling
     */
    BYTE_SHUFFLE(1, "shuffle"),
    /**
     * Bit-wise shuffling
     */
    BIT_SHUFFLE(2, "bitshuffle");
    private final int shuffle;
    private final String shuffleString;

    Shuffle(int shuffle, String shuffleString) {
      this.shuffle = shuffle;
      this.shuffleString = shuffleString;
    }

    public static Shuffle fromInt(int shuffle) {
      for (Shuffle e : values()) {
        if (e.shuffle == shuffle) {
          return e;
        }
      }
      return null;
    }

    public static Shuffle fromString(String shuffle) {
      for (Shuffle e : values()) {
        if (e.shuffleString.equalsIgnoreCase(shuffle)) {
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
