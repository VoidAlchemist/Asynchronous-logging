package com.xenon.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

@SuppressWarnings({"unused", "unchecked"})
public class Donut<E> implements Queue<E> {

    private final E[] data;
    private final int capacity;
    private int size, addIndex, getIndex;

    public Donut(int capacity){
        assert capacity >= 0 : "Don't set a negative capacity! Poor donut.";
        this.capacity = capacity;
        data = (E[]) new Object[capacity];
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
        for (E e : data)
            if (e == o || (o instanceof String && e.equals(o)))
                return true;
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return (Iterator<E>) Arrays.asList(toArray());
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[size]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        int j = 0;
        for (int i=0; i < capacity; i++){
            E e = data[i];
            if (e != null) {
                a[j] = (T) e;
                ++j;
            }
        }
        return a;
    }

    @Override
    public boolean add(E e) {
        addIndex %= capacity;
        boolean overwrote = false;
        E old = data[addIndex];
        data[addIndex] = e;
        if (old == null)
            ++size;
        else{
            ++getIndex;
            overwrote = true;
        }

        ++addIndex;
        return overwrote;
    }

    @Override
    public boolean offer(E e) {
        addIndex %= capacity;
        E old = data[addIndex];
        if (old == null){
            data[addIndex] = e;
            ++addIndex;
            ++size;
            return true;
        }else{
            return false;
        }
    }

    @Override
    public E remove() {
        getIndex %= capacity;
        E removed = data[getIndex];
        data[getIndex] = null;
        ++getIndex;
        return removed;
    }

    @Override
    public E poll() {
        return remove();
    }

    @Override
    public E element() {
        return data[getIndex %= capacity];
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
        for (Object e : c)
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
        for (int i=0; i < capacity; i++)
            data[i] = null;
        size = getIndex = addIndex = 0;
    }

    @Override
    public String toString() {
        return "Donut {"+Arrays.toString(data)+'}';
    }
}
