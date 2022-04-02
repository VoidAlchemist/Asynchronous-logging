package com.xenon.collections;


import com.xenon.collections.abstraction.Struct;
import com.xenon.utils.readability.Values;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Donut with locks in form of CAS.
 * @param <T>
 * @author Zenon
 * @see AsyncDonut
 */
@SuppressWarnings("unused")
public class BlockingDonut<T> extends Struct<T> {

    private int head, tail;
    private final AtomicReference<Object> semaphore = new AtomicReference<>(this);


    /**
     * Super-classes should check if capacity is a power of 2, in order to use {@link #modulo}, otherwise useless.
     *
     * @param capacity the object capacity of the struct. must be a power of 2
     * @throws IllegalArgumentException if capacity is not a power of 2
     */
    public static <T> BlockingDonut<T> build(@Values("2^k, k∈ℕ") int capacity){
        if (capacity <= 0 || (capacity & - capacity) != capacity)   // necessary for modulo capacity to be cheap
            throw new IllegalArgumentException("capacity must be a power of 2");
        return new BlockingDonut<>(capacity);
    }

    /**
     * Super-classes should check if capacity is a power of 2, in order to use {@link #modulo}, otherwise useless.
     *
     * @param capacity the object capacity of the struct
     */
    protected BlockingDonut(int capacity) {
        super(capacity);
    }


    /**
     * Atomically adds the obj element at the tail of this ring buffer.
     * We make the assumption that N threads will access this method at the same time, N <= capacity.
     * If N > capacity, the modulo capacity is broken, and weird data races might take place.
     * @param obj the object to add to the ring buffer
     */
    @Override
    public void add(T obj){
        do{
        }while(!semaphore.compareAndSet(this, null));

        int c = head & modulo;
        ++head;
        data[c] = obj;
        if (data[(c + 1) & modulo] != null)
            ++tail;
        semaphore.set(this);
    }

    /**
     * Atomically consume the element at the tail.
     * @return the consumed element
     */
    @SuppressWarnings("unchecked")
    public T consume(){
        do{
        }while(!semaphore.compareAndSet(this, null));

        int c = tail & modulo;
        ++tail;
        T result = (T) data[c];

        data[c] = null;
        semaphore.set(this);
        return result;
    }

    /**
     * Executes the given consumer on every object in the donut.
     * @param consumer the consumer to be applied
     */
    public void consumeAll(Consumer<T> consumer){
        for (int i=0; i < modulo + 1; ++i)
            consumer.accept(consume());
    }

}
