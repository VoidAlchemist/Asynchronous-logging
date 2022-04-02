package com.xenon.collections;

import com.xenon.collections.abstraction.Struct;
import com.xenon.utils.SpinLock;
import com.xenon.utils.readability.Values;

import java.util.function.Consumer;

/**
 * Ring buffer is too long and boring so named it torus, though it's nothing 3-dimensional.
 * A {@link SpinLock} object is used to lock critical sections (basically mutexes without context-switching).
 * @author Zenon
 * @param <T> the type of data the Torus will hold
 */
@SuppressWarnings("unused")
public class BlockingTorus<T> extends Struct<T> {

    protected final SpinLock lock = SpinLock.create();
    protected int head, tail;

    /**
     * Creates a new ring buffer object with the given maximal capacity.
     * @param capacity the max capacity of this ring buffer
     * @throws IllegalArgumentException if capacity is not a power of 2
     */
    public static <T> BlockingTorus<T> build(@Values("2^k, k∈ℕ") int capacity){
        if (capacity <= 0 || (capacity & -capacity) != capacity)
            throw new IllegalArgumentException("Torus' capacity must be a power of 2. Given "+capacity);
        return new BlockingTorus<>(capacity);
    }
    /**
     * Creates a new ring buffer object with the given maximal capacity.
     * @param capacity the max capacity of this ring buffer. any power of 2 is possible.
     */
    protected BlockingTorus(int capacity){super(capacity);}


    /**
     * Adds an object to the ring buffer at the head position. Can overwrite old values if nothing's consumed them.
     * @param t the object to be added
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    public void add(T t){
        final int mod = modulo; // put as much operation as possible outside the locked area whenever possible
        lock.lock();
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
    }

    /**
     * Adds an object to the ring buffer at the head position. Can overwrite old values if nothing's consumed them.
     * @param t the object to be added
     * @return whether it succeeded
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public boolean tryAdd(T t){
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
     * LMAX Disruptor leaves consumed elements in the buffer, which gets rid of every producer-consumer
     * possible conflicts, as consumers do nothing more than LOADs.
     * We thus don't need any {@link #tail} variable in that case.
     * It can be implemented quite easily:<br><br>
     * <pre><code>
     *     int next = head & mod;
     *     int index = data[next] == null ? 0 : next;   // if
     *     return data[index];
     * </code></pre>
     * In the case of the Disruptor, a single sequential-consistent LOAD for head is all it takes to get rid
     * of race conditions between producers and consumers.
     * Will probably implement a faster version of Torus with this in mind.
     * @return the consumed element
     */
    @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
    @Override
    public T consume(){
        final int mod = modulo; // put as much operation as possible outside the locked area whenever possible
        lock.lock();
        int t = tail;

        t &= mod;

        T result = (T) data[t];
        data[t] = null;
        if (result != null)
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

    /**
     * Unsafe method that returns if this ring buffer has room.
     * No locking is used to prevent race conditions on both {@link #data} and {@link #head}.
     * @return whether this ring buffer has room
     */
    public boolean hasRoom(){
        return data[head] == null;
    }

    /**
     * Unsafe method that returns if this ring buffer is empty.
     * No locking is used to prevent race conditions on both {@link #data} and {@link #tail}.
     * @return whether this ring buffer is empty
     */
    public boolean isEmpty(){
        lock.lock();
        Object o = data[tail];
        lock.unlock();
        return o == null;
    }

}
