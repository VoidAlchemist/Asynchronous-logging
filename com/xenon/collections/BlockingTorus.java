package com.xenon.collections;

import com.xenon.utils.SpinLock;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Ring buffer is too long so named it torus, though it's nothing 3-dimensional.
 * A {@link SpinLock} object is used to lock critical sections (basically mutexes without context-switching).
 * @author Zenon
 * @param <T> the type of data the Torus will hold
 */
@SuppressWarnings("unused")
public class BlockingTorus<T> {

    protected final SpinLock lock = SpinLock.create();
    protected final Object[] data;
    protected int head, tail;
    protected final int modulo;

    /**
     * Creates a new ring buffer object with the given maximal capacity.
     * @param capacity the max capacity of this ring buffer
     * @throws IllegalArgumentException if capacity is not a power of 2
     */
    public static <T> BlockingTorus<T> build(int capacity){
        if (capacity <= 0 || (capacity & -capacity) != capacity)
            throw new IllegalArgumentException("Torus' capacity must be a power of 2. Given "+capacity);
        return new BlockingTorus<>(capacity);
    }
    /**
     * Creates a new ring buffer object with the given maximal capacity.
     * @param capacity the max capacity of this ring buffer. any power of 2 is possible.
     */
    protected BlockingTorus(int capacity){
        data = new Object[capacity];
        modulo = capacity - 1;
    }


    /**
     * Adds an object to the ring buffer at the head position. Can overwrite old values if nothing's consumed them.
     * @param t the object to be added
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public void offer(T t){
        final int mod = modulo; // put as much operation as possible outside the locked area whenever possible
        lock.lock();
        int h = head;

        h &= mod;

        Object o = data[h];
        if (o != null){ // overwrite case
            tail++;
        }
        data[h] = t;
        h++;

        head = h;
        lock.unlock();
    }

    /**
     * Adds an object to the ring buffer at the head position. Can overwrite old values if nothing's consumed them.
     * @param t the object to be added
     * @return whether it succeeded
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public boolean tryOffer(T t){
        final int mod = modulo; // put as much operation as possible outside the locked area whenever possible
        if (!lock.tryLock())
            return false;
        int h = head;

        h &= mod;

        Object o = data[h];
        if (o != null){
            tail++;
        }
        data[h] = t;
        h++;

        head = h;
        lock.unlock();
        return true;
    }


    /**
     * Consume the object at the tail of the ring buffer and returns it.
     * @return the consumed element
     */
    @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
    public T consume(){
        final int mod = modulo; // put as much operation as possible outside the locked area whenever possible
        lock.lock();
        int t = tail;

        t &= mod;

        T result = (T) data[t];
        data[t] = null;
        t++;

        tail = t;
        lock.unlock();
        return result;
    }

    /**
     * Consume everything in this ring buffer with the given consumer.
     * @param consumer the consumer
     */
    public void consumeAll(Consumer<T> consumer){
        for (T obj = consume(); obj != null; obj = consume())
            consumer.accept(obj);
    }

    @Override
    public String toString() {
        return "AsyncTorus{" +
                "data=" + Arrays.toString(data) +
                ", head=" + head +
                ", tail=" + tail +
                ", capacity=" + (modulo + 1) +
                '}';
    }
}
