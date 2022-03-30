package com.xenon.collections;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Ring buffer with locks in form of CAS
 * @param <E>
 */
@SuppressWarnings("unused")
public class BlockingDonut<E> {

    private final Object[] data;
    private int head, tail;
    private final AtomicReference<Object> semaphore = new AtomicReference<>(this);
    private final int capacity, modulo;

    public BlockingDonut(int capacity){
        if (capacity <= 0 || (capacity & - capacity) != capacity)   // necessary for modulo capacity to be cheap
            throw new IllegalArgumentException("capacity must be a power of 2");
        data = new Object[capacity];
        this.capacity = capacity;
        modulo = capacity - 1;
    }


    /**
     * Atomically adds the obj element at the head of this ring buffer.
     * Only one thread can access this function at a time.
     * @param obj the object to add to the ring buffer
     */
    public void offer(E obj){
        do{	// busy-wait strategy (do smg here)
        }while(!semaphore.compareAndSet(this, null));

        int c = head & modulo;
        ++head;
        data[c] = obj;
        if (data[(c + 1) & modulo] != null)
            ++tail;
        semaphore.set(this);
    }
    
    /**
     * Atomically removes the element at the tail of this ring buffer.
     * Only one thread can access this function at a time.
     * /
    @SuppressWarnings("unchecked")
    public E consume(){
        do{	// busy-wait strategy (do smg here)
        }while(!semaphore.compareAndSet(this, null));

        int c = tail & modulo;
        ++tail;
        E result = (E) data[c];

        data[c] = null;
        semaphore.set(this);
        return result;
    }

    @SuppressWarnings("unchecked")
    public void consumeAll(Consumer<E> consumer){
        for (int i=0; i < capacity; ++i)
            consumer.accept(consume());	// should've wrapped this over lock instead of always calling consume()
    }






    @Override
    public String toString() {
        return "AsynchronousDonut{" +
                "data=" + Arrays.toString(data) +
                '}';
    }
}
