package com.xenon.collections;


import test.com.xenon.logger.WeakVolatileInt;

import java.util.Arrays;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class AsyncDonut<E> {

    private final Object[] data;
    private final WeakVolatileInt head = new WeakVolatileInt();
    private final WeakVolatileInt tail = new WeakVolatileInt();
    private final int capacity, modulo;

    public AsyncDonut(int capacity){
        if (capacity <= 0 || (capacity & - capacity) != capacity)   // necessary for modulo capacity to be cheap
            throw new IllegalArgumentException("capacity must be a power of 2");
        data = new Object[capacity];
        this.capacity = capacity;
        modulo = capacity - 1;
    }


    /**
     * Atomically adds the obj element at the tail of this ring buffer.
     * We make the assumption that N threads will access this method at the same time, N <= capacity.
     * If N > capacity, the modulo capacity is broken, and weird data races might take place.
     * @param obj the object to add to the ring buffer
     */
    public void offer(E obj){

        int c = head.fetchAndAdd(1);  // atomic op
        int realCursor = c;

        if (c >= capacity) {
            realCursor &= modulo;    // x & (N-1) <=> x % N if N is power of 2
            head.realCas(c, realCursor);  // threads may help each other on this
        }

        data[realCursor] = obj;

        if (data[realCursor + 1] != null)   // overwrite case
            tail.fetchAndAdd(1);    // we must move tail along
    }

    @SuppressWarnings("unchecked")
    public E consume(){
        int c = tail.fetchAndAdd(1);
        int realCursor = c;

        if (c >= capacity){
            realCursor &= modulo;
            tail.realCas(c, realCursor);
        }
        E result = (E) data[realCursor];

        if (realCursor != (head.getVolatile() & modulo))
            data[realCursor] = null;    // unsafe. safe version would be with data being AtomicReferenceArray

        return result;
    }

    @SuppressWarnings("unchecked")
    public void consumeAll(Consumer<E> consumer){
        int c = tail.getVolatile();
        for (int i=0; i < capacity; i++){
            E e = (E) data[(i + c) & modulo];
            if (e != null)
                consumer.accept(e);
        }



        tail.setVolatile(c);
    }






    @Override
    public String toString() {
        return "AsynchronousDonut{" +
                "data=" + Arrays.toString(data) +
                '}';
    }
}
