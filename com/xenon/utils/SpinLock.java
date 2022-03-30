package com.xenon.utils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A very simple SpinLock implementation in Java, like you can find by hundreds on blogs.
 * Essentially, it's a lock, but without context switching.
 * @author Zenon
 */
@SuppressWarnings("all")
public class SpinLock{

    /**
     * The HANDLE for using atomic operations on {@link #value}
     */
    private static final VarHandle HANDLE;

    static {
        try {
            HANDLE = MethodHandles.lookup().in(SpinLock.class)
                    .findVarHandle(SpinLock.class, "value", boolean.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * the underlying boolean value
     * represents whether some thread is holding the lock
     */
    private boolean value;


    /**
     *
     * @return a new <code>SpinLock</code> object
     */
    public static SpinLock create(){
        return new SpinLock();
    }
    private SpinLock(){}

    /**
     * Spins until it manages to get the lock.
     * Uses <code>Thread.yield()</code> to avoid busy-wait.
     */
    public void lock(){
        while ((boolean) HANDLE.compareAndExchangeAcquire(this, false, true))
            while ((boolean) HANDLE.get(this))
                Thread.yield();
    }

    /**
     * Spins until it manages to get the lock.
     * If it doesn't manage to, busy-waits infinitely.
     * Recommended only if you starve for speed and don't care about CPU power consumption (so not recommended actually).
     */
    public void lockClockBurning(){
        while ((boolean) HANDLE.compareAndExchangeAcquire(this, false, true))
            while ((boolean) HANDLE.get(this));
    }


    /**
     * Tries to get the lock.
     * First loads {@link #value} to see if the lock is free and if it is, tries a <code>compareAndExchanges</code>.
     * It should be used when a custom behaviour is wanted when acquiring the lock fails
     * (replacing <code>Thread.yield()</code> or <code>;</code> with something more useful).
     * The program should then be spinning on this method until it works.
     * @return whether acquiring the lock succeeded
     */
    public boolean tryLock(){
        return !(boolean) HANDLE.get(this) && !(boolean) HANDLE.compareAndExchangeAcquire(this, false, true);
    }

    /**
     * Sets {@link #value} to false, telling other threads the lock is free.
     */
    public void unlock(){
        HANDLE.set(this, false);
    }
}
