/**
 * CountingSet.java
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @param <T>
 * @author Ryan Wilson
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

    /**
     * 
     */
    public CountingSet()
    {
        set = new HashMap<T,Integer>();
    }

    /**
     * 
     * @param initialCapacity
     */
    public CountingSet(int initialCapacity)
    {
        set = new HashMap<T,Integer>(initialCapacity);
    }

    /**
     * 
     * @param initialCapacity
     * @param loadFactor
     */
    public CountingSet(int initialCapacity, float loadFactor)
    {
        set = new HashMap<T,Integer>(initialCapacity,loadFactor);
    }

    /**
     * 
     * @param c
     */
    public CountingSet(Collection<? extends T> c)
    {
        set = new HashMap<T,Integer>();
        for (T e : c) add(e);
    }

    /**
     * 
     * @param o
     * @return
     */
    public synchronized int getAddCount(Object o)
    {
        Integer i = set.get(o);
        return i == null ? 0 : i.intValue();
    }

    /**
     * 
     */
    public synchronized void clear()
    {
        set.clear();
    }

    /**
     * 
     * @param o
     * @return
     */
    public synchronized boolean add(T o)
    {
        if (set.containsKey(o)) {
            set.put(o,set.get(o) + 1);
            return false;
        } else {
            set.put(o,1);
            return true;
        }
    }

    /**
     * @param c
     * @return true always, since every element will either be added to the
     *         set, or increment an element that was already in the set.
     */
    public synchronized boolean addAll(Collection<? extends T> c) {
        for (T e : c) add(e);
        return true;
    }

    /**
     * 
     * @param o
     * @return
     */
    public synchronized boolean contains(Object o)
    {
        return set.containsKey(o);
    }

    /**
     * 
     * @param c
     * @return
     */
    public synchronized boolean containsAll(Collection<?> c)
    {
        boolean all = true;
        for (Object o : c) all &= contains(o);
        return all;
    }

    /**
     * 
     * @return
     */
    public synchronized boolean isEmpty()
    {
        return set.isEmpty();
    }

    /**
     * 
     * @return
     */
    public synchronized Iterator<T> iterator()
    {
        return set.keySet().iterator();
    }

    /**
     * 
     * @param o
     * @return
     */
    public synchronized boolean remove(Object o)
    {
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

    /**
     * 
     * @param c
     * @return
     */
    public synchronized boolean removeAll(Collection<?> c)
    {
        boolean ret = false;
        for (Object e : c) ret |= remove(e);
        return ret;
    }

    /**
     * 
     * @param c
     * @return
     */
    public synchronized boolean retainAll(Collection<?> c)
    {
        
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

    /**
     * 
     * @return
     */
    public synchronized int size()
    {
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
     * @param <T>
     * @param a
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
