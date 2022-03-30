package test.com.xenon.logger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 *
 * Will be used as cursor for a lock-free ring buffer, much like LMAX Disruptor.
 *
 * @author Zenon
 */
public class WeakVolatileInt {

    /**
     * The handle for the value field, used to simulate {@link java.util.concurrent.atomic.AtomicInteger}.
     */
    private static final VarHandle HANDLE;  // need an instance of VarHandle for CAS

    static {
        try {
            /*
            * The in(Class) method limits the search of the field directly to our class,
            * though we have to write it in findVarHandle anyway.
            * */
            HANDLE = MethodHandles.lookup().in(WeakVolatileInt.class)
                    .findVarHandle(WeakVolatileInt.class, "value", int.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The underlying data.
     * The goal is to avoid using volatile, and of course AtomicInteger.
     */
    private int value;

    /**
     *
     * @param i initial value
     */
    public WeakVolatileInt(int i){
        value = i;
    }
    public WeakVolatileInt(){}

    /**
     * Unsafe set that ignores what happens after. Future loads may be put before the actual set,
     * thus reading the wrong value, or future stores may be put before the actual set, thus getting voided.
     * @param newValue the new value
     */
    public void set(int newValue){
        VarHandle.releaseFence();   // prevents previous stores & loads to be re-organized with our store.
        value = newValue;
    }

    /**
     * Volatile sets the value.
     * Equivalent of {@link java.util.concurrent.atomic.AtomicInteger#set(int)}.
     * @param newValue the new value
     */
    public void setVolatile(int newValue){

        VarHandle.releaseFence();   // prevents previous stores & loads to be re-organized with our store.
        value = newValue;
        VarHandle.fullFence();  // prevents our store to be re-organized with future loads & store.
        // Sadly, Java only implements a load-store against load-store, and not just store against load-store.
        // so we use fullFence.
    }
    
    public int get(){
        int temp = value;
        VarHandle.acquireFence();   // load-store barrier. Prevents jumping in the future.
        return value;
    }

    public int getVolatile(){

        VarHandle.fullFence();  // store-load barrier. safely reads the value
        int temp = value;
        VarHandle.acquireFence();   // load-store barrier. Prevents jumping in the future.
        return temp;
    }

    /**
     * Compare & Swap function. Note that it's a joke, as I rewrote the CAS myself. CAS is based on hardware-level
     * instructions, so writing it in plain Java sounds wrong. Plenty of races possible.
     * Plus, AtomicInteger uses native code for that, so don't use this method, seriously.
     * Use {@link #realCas(int, int)} instead.
     * @param expected expected value
     * @param newValue new value
     * @return whether it succeeded
     * @see #realCas(int, int)
     */
    @Deprecated
    public boolean cas(int expected, int newValue){

        VarHandle.fullFence();  // store-load barrier. Ensures that previous stores are visible in the if instruction,
                                // as loads are slow.
        int currentValue = value;   // put this load outside the if to visualize branching issues. not sure though

        VarHandle.releaseFence();   // load-store barrier. Ensures the correct value is checked.

        if (currentValue == expected){

            value = newValue;
            VarHandle.fullFence();  // store-load barrier. Ensures the change is visible.
            // Could've called setVolatile instead of manually writing the 2 last fences, but I kept it this way
            // to remind me to check branch prediction issues, if there's any.
            return true;
        }
        return false;
    }

    /**
     * Serious Compare & Swap function.
     * Equivalent to {@link java.util.concurrent.atomic.AtomicInteger#compareAndSet(int, int)}.
     * @param expected expected value
     * @param newValue new value
     * @return whether it succeeded
     */
    public boolean realCas(int expected, int newValue){
        return HANDLE.compareAndSet(this, expected, newValue);
    }


    /**
     * Classic Fetch & Add function.
     * Equivalent to {@link java.util.concurrent.atomic.AtomicInteger#getAndAdd(int)}.
     * @param n added delta
     * @return the value before the n were added
     */
    public int fetchAndAdd(int n){
        return (int) HANDLE.getAndAdd(this, n);
    }


    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
