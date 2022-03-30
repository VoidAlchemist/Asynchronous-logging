package com.xenon.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

@SuppressWarnings({"unused", "unchecked"})
public class Donut<E> implements Queue<E> {

    private final Object[] data;
    private final int capacity, modulo;
    private int size, head, tail;

    public Donut(int capacity){
        if (capacity <= 0 || (capacity & - capacity) != capacity)
            throw new IllegalArgumentException("Donut's capacity must be a power of 2. Given "+capacity);
        modulo = capacity - 1;
        this.capacity = capacity;
        data = new Object[capacity];
    }


    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size < 1;
    }

    @Override
    public boolean contains(Object o) {
        for (var e : data)
            if (e == o || (o instanceof String && e.equals(o)))
                return true;
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return (Iterator<E>) Arrays.asList((E[])toArray());
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[size]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        int j = 0;
        for (int i = 0; i < capacity; i++){
            Object e = data[i];
            if (e != null) {
                a[j] = (T) e;
                ++j;
            }
        }
        return a;
    }

    @Override
    public boolean add(E e) {
        head &= modulo;
        boolean overwrote = false;
        Object old = data[head];
        data[head] = e;
        if (old == null)
            ++size;
        else{
            ++tail;
            overwrote = true;
        }

        ++head;
        return overwrote;
    }

    @Override
    public boolean offer(E e) {
        head &= modulo;
        Object old = data[head];
        if (old == null){
            data[head] = e;
            ++head;
            ++size;
            return true;
        }else{
            return false;
        }
    }

    @Override
    public E remove() {
        tail &= modulo;
        E removed = (E) data[tail];
        data[tail] = null;
        ++tail;
        return removed;
    }

    @Override
    public E poll() {
        return remove();
    }

    @Override
    public E element() {
        return (E) data[tail &= modulo];
    }

    @Override
    public E peek() {
        return element();
    }

    /**
     *
     * @param o the object to remove
     * @throws UnsupportedOperationException because it will create holes in the donut.
     */
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (var e : c)
            if (!contains(e))
                return false;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (E e : c)
            add(e);
        return true;
    }

    /**
     *
     * @param c the collection to remove
     * @throws UnsupportedOperationException because it will create holes in the donut.
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param c the collection to save
     * @throws UnsupportedOperationException because it will create holes in the donut.
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        for (int i = 0; i < capacity; i++)
            data[i] = null;
        size = tail = head = 0;
    }

    @Override
    public String toString() {
        return "Donut {"+Arrays.toString(data)+'}';
    }
}
