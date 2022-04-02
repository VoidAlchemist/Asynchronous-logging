package com.xenon.collections;

import com.xenon.collections.abstraction.Struct;
import com.xenon.utils.SpinLock;
import com.xenon.utils.readability.Values;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Ring buffer is too long so named it torus, though it's nothing 3-dimensional.
 * A {@link SpinLock} object is used to lock critical sections (basically mutexes without context-switching).
 * @author Zenon
 * @param <T> the type of data the Torus will hold
 */
@SuppressWarnings("unused")
public class AsyncTorus<T> extends Struct<T> {

    protected final AtomicInteger head = new AtomicInteger();

    /**
     * Creates a new ring buffer object with the given maximal capacity.
     * @param capacity the max capacity of this ring buffer
     * @throws IllegalArgumentException if capacity is not a power of 2
     */
    public static <T> AsyncTorus<T> build(@Values("2^k, k∈ℕ") int capacity){
        if (capacity <= 0 || (capacity & -capacity) != capacity)
            throw new IllegalArgumentException("Torus' capacity must be a power of 2. Given "+capacity);
        return new AsyncTorus<>(capacity);
    }
    /**
     * Creates a new ring buffer object with the given maximal capacity.
     * @param capacity the max capacity of this ring buffer. any power of 2 is possible.
     */
    protected AsyncTorus(int capacity){super(capacity);}


    /**
     * Adds an object to the ring buffer at the head position. Can overwrite old values if nothing's consumed them.
     * For this version of the Torus class, no lock is ever used, only fetch&Add.
     * @param t the object to be added
     */
    @Override
    public void add(T t) {
        int h = head.getAndIncrement(); // what makes it lock free.
        // (only works when at most N threads call this method at the same time, N < capacity.
        // but anyway, in general, this ring buffer's capacity should be set accordingly
        // to the number of threads that will run)

        h &= modulo;

        data[h] = t;
    }

    /**
     * Do nothing
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    @Override
    public T consume() {
        throw new UnsupportedOperationException();
    }

    /**
     * Consume everything in this ring buffer with the given consumer.
     * @param consumer the consumer
     */
    @SuppressWarnings("unchecked")
    public void consumeAll(Consumer<T> consumer){
        int h = head.getAcquire();
        int h1 = h & modulo;
        if (h1 != h)
            head.compareAndSet(h, h1);

        int index = data[h1] == null ? 0 : h1;
        for (int i=index; i < index + modulo + 1;++i)
            consumer.accept((T) data[i]);
    }

}
