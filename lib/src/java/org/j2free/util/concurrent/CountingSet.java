/*
 * CountingSet.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;

/**
 * A thread-safe <code>Set</code> that keeps track of how many times
 * an element has been added.  The set can be dumped as a list in asc or
 * desc order of how many times an element was added.
 *
 * @author ryan
 */
@ThreadSafe
public final class CountingSet<T> implements Iterable<T>, Collection<T>, Set<T> {

    private final class Asc<T> implements Comparator<T> {
        public int compare(T one, T two) {
            int oneC = set.get(one);
            int twoC = set.get(two);

            if (oneC > twoC) {
                return -1;
            }

            return 1;
        }
    }

    private final class Desc<T> implements Comparator<T> {
        public int compare(T one, T two) {
            int oneC = set.get(one);
            int twoC = set.get(two);

            if (oneC > twoC) {
                return 1;
            }

            return -1;
        }
    }

    private final HashMap<T,Integer> set;

    public CountingSet() {
        set = new HashMap<T,Integer>();
    }

    public CountingSet(int initialCapacity) {
        set = new HashMap<T,Integer>(initialCapacity);
    }

    public CountingSet(int initialCapacity, float loadFactor) {
        set = new HashMap<T,Integer>(initialCapacity,loadFactor);
    }

    public CountingSet(Collection<? extends T> c) {
        set = new HashMap<T,Integer>();
        for (T e : c) add(e);
    }

    public synchronized int getAddCount(Object o) {
        Integer i = set.get(o);
        return i == null ? 0 : i.intValue();
    }

    public synchronized void clear() {
        set.clear();
    }

    public synchronized boolean add(T o) {
        if (set.containsKey(o)) {
            set.put(o,set.get(o) + 1);
            return false;
        } else {
            set.put(o,1);
            return true;
        }
    }

    /**
     * @return true always, since every element will either be added to the
     *         set, or increment an element that was already in the set.
     */
    public synchronized boolean addAll(Collection<? extends T> c) {
        for (T e : c) add(e);
        return true;
    }

    public synchronized boolean contains(Object o) {
        return set.containsKey(o);
    }

    public synchronized boolean containsAll(Collection<?> c) {
        boolean all = true;
        for (Object o : c) all &= contains(o);
        return all;
    }

    public synchronized boolean isEmpty() {
        return set.isEmpty();
    }

    public synchronized Iterator<T> iterator() {
        return set.keySet().iterator();
    }

    public synchronized boolean remove(Object o) {
        Integer i = set.get(o);
        if (i == null) {
            return false;
        } else if (i == 1) {
            return (set.remove(o) != null);
        } else {
            set.put((T)o, set.get(o) - 1);
            return true;
        }
    }

    public synchronized boolean removeAll(Collection<?> c) {
        boolean ret = false;
        for (Object e : c) ret |= remove(e);
        return ret;
    }

    public synchronized boolean retainAll(Collection<?> c) {
        
        boolean mod = false;

        Iterator<T> itr = set.keySet().iterator();
        T e;
        
        while (itr.hasNext()) {
            e = itr.next();
            if (!c.contains(e))
                mod |= remove(e);
        }

        return mod;
    }

    public synchronized int size() {
        return set.size();
    }

    /**
     * @return an Object[] containing the elements of this set
     *         ordered in ascending order by the number of times
     *         they were added to this set.
     */
    public synchronized Object[] toArray() {
        Object[] a = set.keySet().toArray();
        Arrays.sort(a,new Asc());
        return a;
    }

    /**
     * @return a T[] containing the elements of this set
     *         ordered in ascending order by the number of times
     *         they were added to this set.
     */
    public synchronized <T> T[] toArray(T[] a) {
        T[] array = set.keySet().toArray(a);
        Arrays.sort(array,new Asc());
        return array;
    }
}
