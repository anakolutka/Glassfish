
package com.kenai.jffi;

/**
 * Holds objects the native code must handle - such as primitive arrays
 */
final class ObjectBuffer {
    /** Copy the array contents to native memory before calling the function */
    public static final int IN = 0x1;

    /** After calling the function, reload the array contents from native memory */
    public static final int OUT = 0x2;

    /** Append a NUL byte to the array contents after copying to native memory */
    public static final int ZERO_TERMINATE = 0x4;

    /** Pin the array memory and pass the JVM memory pointer directly to the function */
    public static final int PINNED = 0x8;

    /*
     * WARNING: The following flags cannot be altered without recompiling the native code 
     */
    static final int INDEX_SHIFT = 16;
    static final int INDEX_MASK = 0x00ff0000;
    static final int TYPE_SHIFT = 24;
    static final int TYPE_MASK = 0xff << TYPE_SHIFT;
    static final int PRIM_MASK = 0x0f << TYPE_SHIFT;
    static final int FLAGS_SHIFT = 0;
    static final int FLAGS_MASK = 0xff;

    static final int ARRAY = 0x10 << TYPE_SHIFT;
    static final int BUFFER = 0x20 << TYPE_SHIFT;
    
    static final int BYTE = 0x1 << TYPE_SHIFT;
    static final int SHORT = 0x2 << TYPE_SHIFT;
    static final int INT = 0x3 << TYPE_SHIFT;
    static final int LONG = 0x4 << TYPE_SHIFT;
    static final int FLOAT = 0x5 << TYPE_SHIFT;
    static final int DOUBLE = 0x6 << TYPE_SHIFT;

    /** The objects stored in this buffer */
    private Object[] objects = new Object[1];

    /** 
     * The flags/offset/length descriptor array.
     *
     * Along with each object, a 3-tuple is stored in the descriptor array.
     *
     * The first element of the tuple stores a mask of the type, parameter index and array flags
     * The second element stores the offset within the array the data starts.
     * The third element stores the length of data.
     */
    private int[] info = new int[objects.length * 3];

    /** The index of the next descriptor storage slot */
    private int infoIndex = 0;

    /** The index of the next object storage slot */
    private int objectIndex = 0;

    /**
     * Gets the number of objects stored in this <tt>ObjectBuffer</tt>.
     *
     * @return the number of objects already stored.
     */
    final int objectCount() {
        return objectIndex;
    }

    /**
     * Gets the object descriptor array.
     *
     * @return An array of integers describing the objects stored.
     */
    final int[] info() {
        return info;
    }

    /**
     * Gets the array of stored objects.
     *
     * @return An array of objects stored in this buffer.
     */
    final Object[] objects() {
        return objects;
    }
    /** Ensures that sufficient space is available to insert at least one more object */
    private final void ensureSpace() {
        if (objects.length <= (objectIndex + 1)) {
            Object[] newObjects = new Object[objects.length << 1];
            System.arraycopy(objects, 0, newObjects, 0, objectIndex);
            objects = newObjects;
            int[] newInfo = new int[objects.length * 3];
            System.arraycopy(info, 0, newInfo, 0, objectIndex * 3);
            info = newInfo;
        }
    }

    /**
     * Encodes the native object flags for an array.
     *
     * @param flags The array flags (IN, OUT) for the object.
     * @param type The type of the object.
     * @param index The parameter index the object should be passed as.
     * @return A bitmask of flags.
     */
    private static final int makeArrayFlags(int flags, int type, int index) {
        return (flags & FLAGS_MASK) | ((index << INDEX_SHIFT) & INDEX_MASK) | type;
    }

    /**
     * Encodes the native object flags for an NIO Buffer.
     *
     * @param index The parameter index of the buffer.
     * @return A bitmask of flags.
     */
    private static final int makeBufferFlags(int index) {
        return ((index << INDEX_SHIFT) & INDEX_MASK) | BUFFER;
    }

    /**
     * Adds a java byte array as a pointer parameter.
     *
     * @param array The java array to use as the pointer parameter.
     * @param offset The offset from the start of the array.
     * @param length The length of the array to use.
     * @param flags The flags to use (IN, OUT, NULTERMINATE)
     */
    public void putArray(int index, byte[] array, int offset, int length, int flags) {
        putObject(array, offset, length, makeArrayFlags(flags, BYTE | ARRAY, index));
    }

    /**
     * Adds a java short array as a pointer parameter.
     *
     * @param array The java array to use as the pointer parameter.
     * @param offset The offset from the start of the array.
     * @param length The length of the array to use.
     * @param flags The flags to use (IN, OUT)
     */
    public void putArray(int index, short[] array, int offset, int length, int flags) {
        putObject(array, offset, length, makeArrayFlags(flags, SHORT | ARRAY, index));
    }

    /**
     * Adds a java int array as a pointer parameter.
     *
     * @param array The java array to use as the pointer parameter.
     * @param offset The offset from the start of the array.
     * @param length The length of the array to use.
     * @param flags The flags to use (IN, OUT)
     */
    public void putArray(int index, int[] array, int offset, int length, int flags) {
        putObject(array, offset, length, makeArrayFlags(flags, INT | ARRAY, index));
    }

    /**
     * Adds a java long array as a pointer parameter.
     *
     * @param array The java array to use as the pointer parameter.
     * @param offset The offset from the start of the array.
     * @param length The length of the array to use.
     * @param flags The flags to use (IN, OUT)
     */
    public void putArray(int index, long[] array, int offset, int length, int flags) {
        putObject(array, offset, length, makeArrayFlags(flags, LONG | ARRAY, index));
    }

    /**
     * Adds a java float array as a pointer parameter.
     *
     * @param array The java array to use as the pointer parameter.
     * @param offset The offset from the start of the array.
     * @param length The length of the array to use.
     * @param flags The flags to use (IN, OUT)
     */
    public void putArray(int index, float[] array, int offset, int length, int flags) {
        putObject(array, offset, length, makeArrayFlags(flags, FLOAT | ARRAY, index));
    }

    /**
     * Adds a java double array as a pointer parameter.
     *
     * @param array The java array to use as the pointer parameter.
     * @param offset The offset from the start of the array.
     * @param length The length of the array to use.
     * @param flags The flags to use (IN, OUT)
     */
    public void putArray(int index, double[] array, int offset, int length, int flags) {
        putObject(array, offset, length, makeArrayFlags(flags, DOUBLE | ARRAY, index));
    }

    /**
     * Adds a java direct buffer as a pointer parameter.
     * @param buffer The buffer to use as a pointer argument.
     * @param offset An offset to add to the buffer native address.
     * @param length The length of the buffer to use.
     */
    public void putDirectBuffer(int index, java.nio.Buffer obj, int offset, int length) {
        putObject(obj, offset, length, makeBufferFlags(index));
    }
    private void putObject(Object array, int offset, int length, int flags) {
        ensureSpace();
        objects[objectIndex++] = array;
        info[infoIndex++] = flags;
        info[infoIndex++] = offset;
        info[infoIndex++] = length;
    }
}
