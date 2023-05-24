# blosc-java

A stateless, cross-platform, JNI-based wrapper around the [c-blosc](https://github.com/Blosc/c-blosc) library.

The packaged JARs contain binaries for Windows, Mac OS (x86_64 only) and Linux.

## Usage
```java
import dev.zarr.bloscjava.Blosc;

// Generate some random data
int SIZE = 1000000;
byte[] buf = new byte[SIZE];
for (int i = 0; i < SIZE; i++) {
    buf[i] = (int)(i % 24);
}

byte[] compressedBuf = Blosc.compress(buf, 1, Blosc.Compressor.ZSTD, 9);
byte[] decompressedBuf = Blosc.decompress(compressedBuf);

assert Arrays.equals(buf, decompressedBuf);
```

### API
```java
byte[] compress(
    byte[] src, 
    int typeSize, 
    Blosc.Compressor compressor, 
    int compressorLevel, 
    Blosc.Shuffle shuffle, 
    int blockSize,
    int numThreads
)
```

- `src`: Byte array to be compressed. Required.
- `typeSize`: Number of bytes per primitive value (e.g. 1 for int8, 2 for int16, 4 for int32). Required.
- `compressor`: Compression algorithm. Available choices: `LZ4`, `LZ4HC`, `BLOSCLZ`, `ZSTD`, `SNAPPY`, `ZLIB`. 
  Default: `ZSTD`
- `compressorLevel`: Number from 0 to 9 (0 = little compression, 9 = strongest compression). Default: 5.
- `shuffle`: Whether to use shuffling. Available choices: `NOSHUFFLE`, `BIT_SHUFFLE`, `BYTE_SHUFFLE`. Default: 
  `BIT_SHUFFLE` for typeSize == 1, `BYTE_SHUFFLE` otherwise.
- `blockSize`: Requested size of compressed blocks. Use 0 for automatic block sizes.
- `numThreads`: Number of threads to be used internally. Default: 1.

```java
byte[] decompress(byte[] src, int numThreads)
```

- `src`: Byte array to be decompressed. Required.
- `numThreads`: Number of threads to be used internally. Default: 1.

## Installation

Add the `dev.zarr.blosc-java` dependency to your Maven project:

```xml
    <dependencies>
        <dependency>
            <groupId>dev.zarr</groupId>
            <artifactId>blosc-java</artifactId>
            <version>BLOSC_VERSION</version>
        </dependency>
    </dependencies>
```