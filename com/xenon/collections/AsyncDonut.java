package com.xenon.collections;


import com.xenon.collections.abstraction.Struct;
import com.xenon.utils.readability.Values;
import test.com.xenon.logger.WeakVolatileInt;

import java.util.function.Consumer;

/**
 * Asynchronous version of {@link BlockingDonut}.
 * @param <T> the type of data the donut will hold
 * @author Zenon
 * @see BlockingDonut
 */
@SuppressWarnings("unused")
public class AsyncDonut<T> extends Struct<T> {

    private final WeakVolatileInt head = new WeakVolatileInt();
    private final WeakVolatileInt tail = new WeakVolatileInt();

    /**
     * Creates a new AsyncDonut
     * @param capacity the object capacity of the struct. must be a power of 2
     * @throws IllegalArgumentException if capacity is not a power of 2
     */
    public static <T> AsyncDonut<T> build(@Values("2^k, k∈ℕ") int capacity){
        if (capacity <= 0 || (capacity & - capacity) != capacity)   // necessary for modulo capacity to be cheap
            throw new IllegalArgumentException("capacity must be a power of 2");
        return new AsyncDonut<>(capacity);
    }

    /**
     * Super-classes should check if capacity is a power of 2, in order to use {@link #modulo}, otherwise useless.
     *
     * @param capacity the object capacity of the struct. must be a power of 2
     */
    protected AsyncDonut(int capacity){super(capacity);}


    /**
     * Atomically adds the obj element at the tail of this ring buffer.
     * We make the assumption that N threads will access this method at the same time, N <= capacity.
     * If N > capacity, the modulo capacity is broken, and weird data races might take place.
     * @param obj the object to add to the ring buffer
     */
    @Override
    public void add(T obj){
        int c = head.fetchAndAdd(1);  // atomic op
        int c1 = c & modulo;

        if (c != c1)
            head.realCas(c, c1);  // threads may help each other on this

        data[c1] = obj;

        if (data[c1 + 1] != null)   // overwrite case
            tail.fetchAndAdd(1);    // we must move tail along
    }

    /**
     * Atomically consume the element at the tail.
     * @return the consumed element
     */
    @SuppressWarnings("unchecked")
    public T consume(){
        int c = tail.fetchAndAdd(1);
        int c1 = c & modulo;

        if (c != c1)
            tail.realCas(c, c1);
        T result = (T) data[c1];

        if (c1 != (head.getVolatile() & modulo))
            data[c1] = null;    // unsafe. safe version would be with data being AtomicReferenceArray

        return result;
    }

    /**
     * Executes the given consumer on every object in the donut.
     * @param consumer the consumer to be applied
     */
    @SuppressWarnings("unchecked")
    public void consumeAll(Consumer<T> consumer){
        int c = tail.getVolatile();
        for (int i=0; i < modulo + 1; i++){
            T e = (T) data[(i + c) & modulo];
            if (e != null)
                consumer.accept(e);
        }

        tail.setVolatile(c);
    }

}
