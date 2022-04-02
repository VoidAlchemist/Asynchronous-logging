package com.xenon.collections;

import com.xenon.collections.abstraction.Struct;
import com.xenon.utils.Cursor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Represents an asynchronous pool. A pool struct shouldn't support {@link #add(Object)} nor {@link #consume()},
 * so these methods are <code>UnsupportedOperationException</code>-tagged (Objects re-use).
 * Consider using {@link #retrieve()} and {@link #poll()} or even {@link #pollAll(Consumer)}.
 * @param <T> the type of data the pool will hold
 * @author Zenon
 */
@SuppressWarnings("unused")
public abstract class AsyncPool<T> extends Struct<T> {

    /**
     * the head's cursor
     */
    protected final Cursor head = new Cursor();
    /**
     * the tail's cursor
     */
    protected final Cursor tail = new Cursor();

    /**
     *
     * @param capacity the pool's capacity
     * @see Struct#Struct(int)
     */
    protected AsyncPool(int capacity){super(capacity);}

    /**
     * Use {@link #retrieve()} instead of this method.
     * @throws UnsupportedOperationException always
     */
    @Override
    public final void add(T t) {
        throw new UnsupportedOperationException();
    }

    /**
     * Use {@link #poll()} instead of this method.
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    @Override
    public final T consume() {
        throw new UnsupportedOperationException();
    }


    /**
     * Producer-side method to get an instance from the pool.
     * Calling it will increment the pool's cursor in order to make the next call to this method return the next object.
     * @return the retrieved object
     */
    public abstract T retrieve();

    /**
     * Consumer-side method to get an instance from the pool.
     * Contrary to {@link #retrieve()}, it does not increment the cursor.
     * @return the object at the cursor position
     */
    public abstract T poll();


    /**
     * Consumer-side method to consume every available objects in the pool.
     * Usually equivalent to:
     * <pre><code>
     *     for (T t = poll(); !stopCondition(); t = poll())
     *         consumer.accept(t);
     * </code></pre>
     * with <code>stopCondition()</code> usually being <code>t == null</code>.
     * @param consumer the consumer to be applied
     * @see com.xenon.logging.LogEventPool
     */
    public abstract void pollAll(Consumer<T> consumer);
}
