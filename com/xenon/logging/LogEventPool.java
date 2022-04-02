package com.xenon.logging;

import com.xenon.collections.AsyncPool;
import com.xenon.utils.readability.Values;

import java.util.function.Consumer;

/**
 * @author Zenon
 */
public class LogEventPool extends AsyncPool<LogEvent> {

    protected int tail;

    /**
     * Creates a pool object
     * @param capacity the pool's capacity
     * @return the new pool instance
     */
    public static LogEventPool build(@Values("2^k, k∈ℕ") int capacity){
        if (capacity <= 0 || (capacity & -capacity) != capacity)
            throw new IllegalArgumentException("capacity must be a power of 2. Given "+capacity);
        return new LogEventPool(capacity);
    }

    protected LogEventPool(int capacity) {
        super(capacity);
        for (int i = 0; i < capacity; i++)
            data[i] = new LogEvent();
    }

    @Override
    public LogEvent retrieve() {
        int c = head.f$i();
        c &= modulo;

        return (LogEvent) data[c];
    }

    @Override
    public LogEvent poll() {
        int c = head.get();
        int c1 = c & modulo;

        if (c != c1)
            head.cas(c, c1);

        tail &= modulo;
        if (tail == c1)
            return null;

        LogEvent event = (LogEvent) data[tail];
        tail++;

        return event;
    }

    /**
     * Consumer-side method to consume every available objects in the pool.
     * Equivalent to:
     * <pre><code>
     *     for (LogEvent e = poll(); e != null; e = poll())
     *         consumer.accept(e);
     * </code></pre>
     * @param consumer the consumer to be applied
     * @see #poll()
     */
    @Override
    public void pollAll(Consumer<LogEvent> consumer) {
        for (LogEvent e = poll(); e != null; e = poll())
            consumer.accept(e);
    }
}
