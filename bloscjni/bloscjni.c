#include "com_scalableminds_bloscjava_Blosc.h"
#include "../c-blosc/blosc/blosc.h"

JNIEXPORT jbyteArray JNICALL Java_com_scalableminds_bloscjava_Blosc__1compress(JNIEnv *env, jclass thisClass, jbyteArray src, jint typesize, jint clevel, jint shuffle, jint blocksize, jstring cname, jint numinternalthreads)
{
  // Reading source buffer
  jboolean isCopy;
  const void *srcPtr = (void *)(*env)->GetByteArrayElements(env, src, &isCopy);
  jint srcSize = (*env)->GetArrayLength(env, src);

  // Allocating dest buffer, probably oversized
  size_t destSize = srcSize + BLOSC_MIN_HEADER_LENGTH;
  void *destPtr = (char *)malloc(destSize);

  // Handling allocation failure
  if (destPtr == 0)
  {
    char *exBuffer;
    sprintf(exBuffer, "Cannot allocate %zu bytes for compressed buffer", destSize);
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"), exBuffer);

    // Cleanup after exception
    if (isCopy)
      (*env)->ReleaseByteArrayElements(env, src, (jbyte *)srcPtr, JNI_ABORT);
    return NULL;
  }

  // Compressor name, trusts that Java code sends the right String
  const char *cnamePtr = (*env)->GetStringUTFChars(env, cname, NULL);

  // Actually doing the compression
  // Return value is the actual dest buffer size
  int realDestSize = blosc_compress_ctx(clevel, shuffle, (size_t)typesize,
                                        (size_t)srcSize, srcPtr, destPtr,
                                        destSize, cnamePtr,
                                        (size_t)blocksize, numinternalthreads);

  // Handling compression failure
  if (realDestSize < 0)
  {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"), "Error while compressing buffer");

    // Cleanup after exception
    if (isCopy)
      (*env)->ReleaseByteArrayElements(env, src, (jbyte *)srcPtr, JNI_ABORT);
    free(destPtr);
    return NULL;
  }

  // Prepare return buffer
  jbyteArray dest = (*env)->NewByteArray(env, realDestSize);
  (*env)->SetByteArrayRegion(env, dest, 0, realDestSize, destPtr);

  // Cleanup before return
  free(destPtr);
  if (isCopy)
    (*env)->ReleaseByteArrayElements(env, src, (jbyte *)srcPtr, JNI_ABORT);

  return dest;
}

JNIEXPORT jbyteArray JNICALL Java_com_scalableminds_bloscjava_Blosc__1decompress(JNIEnv *env, jclass thisClass, jbyteArray src, jint numinternalthreads)
{
  // Reading source buffer
  jboolean isCopy;
  const void *srcPtr = (void *)(*env)->GetByteArrayElements(env, src, &isCopy);
  jint srcSize = (*env)->GetArrayLength(env, src);

  // Validating source buffer and getting the dest buffer size from the Blosc header
  size_t destSize;
  if (blosc_cbuffer_validate(srcPtr, srcSize, &destSize) < 0)
  {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"),
                     "Buffer does not contain valid blosc-encoded contents");

    // Cleanup after exception
    if (isCopy)
      (*env)->ReleaseByteArrayElements(env, src, (jbyte *)srcPtr, JNI_ABORT);
    return NULL;
  }

  // Allocating dest buffer
  void *destPtr = malloc(destSize);

  // Handling allocation failure
  if (destPtr == 0)
  {
    char *exBuffer;
    sprintf(exBuffer, "Cannot allocate %zu bytes for decompressed buffer", destSize);
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"), exBuffer);

    // Cleanup after exception
    if (isCopy)
      (*env)->ReleaseByteArrayElements(env, src, (jbyte *)srcPtr, JNI_ABORT);
    return NULL;
  }

  // Actually doing the compression
  int realDestSize = blosc_decompress_ctx(srcPtr, destPtr, destSize, numinternalthreads);

  // Handling decompression failure
  if (realDestSize < 0)
  {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"),
                     "Error while decompressing buffer");

    // Cleanup after exception
    if (isCopy)
      (*env)->ReleaseByteArrayElements(env, src, (jbyte *)srcPtr, JNI_ABORT);
    free(destPtr);
    return NULL;
  }

  // Prepare return buffer
  jbyteArray dest = (*env)->NewByteArray(env, realDestSize);
  (*env)->SetByteArrayRegion(env, dest, 0, realDestSize, destPtr);

  // Cleanup before return
  free(destPtr);
  if (isCopy)
    (*env)->ReleaseByteArrayElements(env, src, (jbyte *)srcPtr, JNI_ABORT);

  return dest;
}