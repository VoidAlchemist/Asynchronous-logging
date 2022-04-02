package com.xenon.collections.abstraction;

import java.util.Arrays;

/**
 * An abstraction for a class that wraps an array of Objects. Added {@link #modulo} for convenience
 * as all my Struct implementations needs to have a capacity power of 2.
 * @author Zenon
 * @param <T>
 * @see com.xenon.collections
 */
@SuppressWarnings("unused")
public abstract class Struct<T> {

    /**
     * the underlying object array
     */
    protected final Object[] data;

    /**
     * equals to <code>data.length - 1</code>. useful to perform cheap modulo with bit-hacks when
     * <code>data.length</code> is a power of 2.
     */
    protected final int modulo;

    /**
     * Super-classes should check if capacity is a power of 2, in order to use {@link #modulo}, otherwise useless.
     * @param capacity the object capacity of the struct
     */
    protected Struct(int capacity){
        data = new Object[capacity];
        modulo = capacity - 1;
    }

    /**
     * Adds an object to the struct.
     * @param t the object offered to the struct
     */
    public abstract void add(T t);

    /**
     * Consume an object from the struct (usually the older object in the underlying array).
     * @return the consumed object
     */
    public abstract T consume();


    /**
     *
     * @return a copy of the underlying array
     */
    public Object[] toArray(){
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public String toString() {
        return "Struct{" +
                "data=" + Arrays.toString(data) +
                '}';
    }
}
