package com.scalableminds.bloscjava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bytedeco.blosc2.blosc2_context;
import org.bytedeco.blosc2.blosc2_cparams;
import org.bytedeco.blosc2.global.blosc2;
import org.bytedeco.javacpp.BytePointer;

/**
 * The Blosc codec in the chunk format of <a href="https://github.com/Blosc/c-blosc2">Blosc2</a>, which C-Blosc 1.x
 * cannot read. {@link Blosc} writes the format that it can; this class has what only Blosc2 has: a pipeline of
 * several filters, and the filters that Blosc 1.x lacks.
 * <p>
 * Decompress with {@link #decompress(byte[])} or with {@link Blosc#decompress(byte[])}: both read either format.
 */
public class Blosc2 {

  // Disable constructor
  private Blosc2() {
  }

  /**
   * A filter of the pipeline that runs on the data before it is compressed (and in reverse after it is
   * decompressed).
   */
  public enum Filter {
    /**
     * Byte-wise shuffling: regroups the bytes of the elements by their position in the element
     */
    BYTE_SHUFFLE(blosc2.BLOSC_SHUFFLE),
    /**
     * Bit-wise shuffling
     */
    BIT_SHUFFLE(blosc2.BLOSC_BITSHUFFLE),
    /**
     * Replaces each block with the differences from the first block
     */
    DELTA(blosc2.BLOSC_DELTA),
    /**
     * Zeroes the low bits of the mantissa of floating point elements (float32 or float64), keeping the number of
     * bits that the filter's argument says: lossy, and it compresses better.
     */
    TRUNC_PREC(blosc2.BLOSC_TRUNC_PREC);

    private final int code;

    Filter(int code) {
      this.code = code;
    }

    public int getValue() {
      return code;
    }
  }

  /** A filter and its argument ({@link Filter#TRUNC_PREC}: the number of mantissa bits to keep). */
  public static final class FilterSpec {
    private final Filter filter;
    private final int argument;

    public FilterSpec(Filter filter, int argument) {
      if (argument < -128 || argument > 127) {
        throw new IllegalArgumentException("A filter's argument is a byte, not " + argument);
      }
      this.filter = filter;
      this.argument = argument;
    }

    public FilterSpec(Filter filter) {
      this(filter, 0);
    }

    public Filter getFilter() {
      return filter;
    }

    public int getArgument() {
      return argument;
    }
  }

  /**
   * What to compress with: the number of bytes per element, the compressor and its level, the filters in the order
   * they run (at most {@value #MAX_FILTERS}), the size of Blosc's blocks and the number of threads.
   */
  public static final class Options {
    private final int typeSize;
    private final Blosc.Compressor compressor;
    private final int compressorLevel;
    private final List<FilterSpec> filters;
    private final int blockSize;
    private final int numThreads;

    private Options(Builder b) {
      this.typeSize = b.typeSize;
      this.compressor = b.compressor;
      this.compressorLevel = b.compressorLevel;
      this.filters = Collections.unmodifiableList(new ArrayList<>(b.filters));
      this.blockSize = b.blockSize;
      this.numThreads = b.numThreads;
    }

    public static Builder builder(int typeSize) {
      return new Builder(typeSize);
    }
  }

  public static final int MAX_FILTERS = blosc2.BLOSC2_MAX_FILTERS;

  /** Zstd at level 5, byte shuffling for elements of more than a byte and bit shuffling for bytes, one thread. */
  public static final class Builder {
    private final int typeSize;
    private Blosc.Compressor compressor = Blosc.Compressor.ZSTD;
    private int compressorLevel = 5;
    private final List<FilterSpec> filters = new ArrayList<>();
    private boolean filtersSet = false;
    private int blockSize = 0;
    private int numThreads = 1;

    private Builder(int typeSize) {
      if (typeSize < 1 || typeSize > 255) {
        throw new IllegalArgumentException("Argument typeSize needs to be between 1 and 255.");
      }
      this.typeSize = typeSize;
    }

    public Builder compressor(Blosc.Compressor compressor) {
      this.compressor = compressor;
      return this;
    }

    /** From 0 (little compression) to 9 (strongest compression). */
    public Builder compressorLevel(int compressorLevel) {
      if (compressorLevel < 0 || compressorLevel > 9) {
        throw new IllegalArgumentException("Argument compressorLevel needs to be between 0 and 9.");
      }
      this.compressorLevel = compressorLevel;
      return this;
    }

    /** The filters, in the order they run before compression: replaces the default. None means no filters. */
    public Builder filters(FilterSpec... filters) {
      if (filters.length > MAX_FILTERS) {
        throw new IllegalArgumentException("At most " + MAX_FILTERS + " filters, not " + filters.length);
      }
      this.filters.clear();
      this.filters.addAll(Arrays.asList(filters));
      this.filtersSet = true;
      return this;
    }

    /** Requested size of Blosc's blocks. Use 0 for automatic block sizes. */
    public Builder blockSize(int blockSize) {
      this.blockSize = blockSize;
      return this;
    }

    public Builder numThreads(int numThreads) {
      this.numThreads = numThreads;
      return this;
    }

    public Options build() {
      if (!filtersSet) {
        filters.add(new FilterSpec(typeSize == 1 ? Filter.BIT_SHUFFLE : Filter.BYTE_SHUFFLE));
      }
      return new Options(this);
    }
  }

  /**
   * Compresses a byte array with Blosc2.
   *
   * @param src     Byte array to be compressed.
   * @param options How to compress it.
   * @return the compressed byte array
   */
  public static byte[] compress(byte[] src, Options options) {
    int compcode = blosc2.blosc2_compname_to_compcode(options.compressor.getValue());
    if (compcode < 0) {
      throw new IllegalArgumentException("Unknown compressor " + options.compressor.getValue());
    }
    // C-Blosc2's delta filter garbles the bytes after the last whole element, so it gets a buffer of whole elements only
    if (src.length % options.typeSize != 0) {
      for (FilterSpec f : options.filters) {
        if (f.filter == Filter.DELTA) {
          throw new IllegalArgumentException("The delta filter needs a whole number of elements: " + src.length
              + " bytes are not a multiple of typeSize " + options.typeSize);
        }
      }
    }
    blosc2_cparams cparams = new blosc2_cparams();
    cparams.compcode((byte) compcode);
    cparams.clevel((byte) options.compressorLevel);
    cparams.typesize(options.typeSize);
    cparams.nthreads((short) Math.max(1, options.numThreads));
    cparams.blocksize(options.blockSize);
    cparams.splitmode(blosc2.BLOSC_FORWARD_COMPAT_SPLIT);
    // the pipeline runs from the first slot to the last, and a slot of no filter is skipped
    for (int i = 0; i < options.filters.size(); i++) {
      cparams.filters(i, (byte) options.filters.get(i).filter.code);
      cparams.filters_meta(i, (byte) options.filters.get(i).argument);
    }
    blosc2_context context = blosc2.blosc2_create_cctx(cparams);
    if (context == null || context.isNull()) {
      throw new RuntimeException("Error while compressing buffer");
    }
    long destSize = (long) src.length + blosc2.BLOSC2_MAX_OVERHEAD;
    BytePointer srcPtr = new BytePointer(Math.max(1, src.length));
    BytePointer destPtr = new BytePointer(destSize);
    try {
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

  /**
   * Compresses a byte array with Blosc2 with default settings (ZSTD compression, level 5, shuffling).
   *
   * @param src      Byte array to be compressed.
   * @param typeSize Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for int32).
   * @return the compressed byte array
   */
  public static byte[] compress(byte[] src, int typeSize) {
    return compress(src, Options.builder(typeSize).build());
  }

  /**
   * Decompresses a byte array of the Blosc 1.x or the Blosc2 format.
   *
   * @param src        Byte array to be decompressed.
   * @param numThreads Number of threads to be used internally.
   * @return the decompressed byte array
   */
  public static byte[] decompress(byte[] src, int numThreads) {
    return Blosc.decompress(src, numThreads);
  }

  /**
   * Decompresses a byte array of the Blosc 1.x or the Blosc2 format.
   *
   * @param src Byte array to be decompressed.
   * @return the decompressed byte array
   */
  public static byte[] decompress(byte[] src) {
    return Blosc.decompress(src);
  }
}
