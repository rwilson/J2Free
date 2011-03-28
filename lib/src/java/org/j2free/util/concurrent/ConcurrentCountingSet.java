/*
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
import java.util.Iterator;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import net.jcip.annotations.ThreadSafe;

/**
 * A thread-safe concurrent <tt>Set</tt> that keeps track of how many times
 * an element has been added.  The set can be dumped as a list in asc or
 * desc order of how many times an element was added.
 *
 * Basically, a performance improvement on {@link CountingSet} by using a
 * {@link java.util.concurrent.ConcurrentHashMap} as the underlying map
 * and using non-blocking CAS algorithms in the <tt>add</tt> and <tt>remove</tt>
 * methods.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public final class ConcurrentCountingSet<T> implements Iterable<T>, 
                                                       Collection<T>,
                                                       Set<T>
{
    private final ConcurrentHashMap<T, Integer> set;

    public ConcurrentCountingSet()
    {
        set = new ConcurrentHashMap<T,Integer>();
    }

    public ConcurrentCountingSet(int initialCapacity)
    {
        set = new ConcurrentHashMap<T,Integer>(initialCapacity);
    }

    public ConcurrentCountingSet(Collection<? extends T> c)
    {
        set = new ConcurrentHashMap<T,Integer>(Math.max((int) (c.size() / .75f) + 1, 16));
        for (T e : c) add(e);
    }

    public int getAddCount(T o)
    {
        Integer i = set.get(o);
        return i == null ? 0 : i.intValue();
    }

    public T min()
    {
        Object[] array = toArray();
        return array.length == 0 ? null : (T)array[0];
    }

    public T max()
    {
        Object[] array = toArray();
        return array.length == 0 ? null : (T)array[array.length - 1];
    }

    public void clear()
    {
        set.clear();
    }

    public boolean add(T o)
    {
        if (set.putIfAbsent(o, 1) == null)
            return true;

        for (;;)
        {
            int cur = set.get(o);
            if (set.replace(o, cur, cur + 1))
                break;
        }

        return false;
    }

    /**
     * @return true if any add for an individual element in the provided
     *         collection would return true
     */
    public boolean addAll(Collection<? extends T> c)
    {
        boolean ret = false;
        for (T e : c)
            ret |= add(e);
        return ret;
    }

    public boolean contains(Object o)
    {
        return set.containsKey(o);
    }

    public boolean containsAll(Collection<?> c)
    {
        boolean all = true;
        for (Object o : c) all &= contains(o);
        return all;
    }

    public boolean isEmpty()
    {
        return set.isEmpty();
    }

    public Iterator<T> iterator()
    {
        return set.keySet().iterator();
    }

    public boolean remove(Object o)
    {
        for (;;)
        {
            Integer cur = set.get(o);
            if (cur == null)
                return false;
            else if (cur == 1)
            {
                if (set.remove(o, cur))
                    return true;
            } 
            else
            {
                if (set.replace((T)o, cur, cur - 1))
                    return true;
            }
        }
    }

    public boolean removeAll(Collection<?> c)
    {
        boolean ret = false;
        for (Object e : c) ret |= remove(e);
        return ret;
    }

    public boolean retainAll(Collection<?> c)
    {
        boolean mod = false;

        Iterator<T> itr = set.keySet().iterator();
        while (itr.hasNext())
        {
            T e = itr.next();
            if (!c.contains(e))
                mod |= remove(e);
        }
        return mod;
    }

    public int size()
    {
        return set.size();
    }

    private final class Asc<T> implements Comparator<T>
    {
        public int compare(T one, T two)
        {
            int oneC = set.get(one);
            int twoC = set.get(two);

            if (oneC > twoC)
                return 1;

            if (oneC < twoC)
                return -1;

            return 0;
        }
    }
    
    /**
     * @return an Object[] containing the elements of this set
     *         ordered in ascending order by the number of times
     *         they were added to this set.
     */
    public Object[] toArray()
    {
        Object[] a = set.keySet().toArray();
        Arrays.sort(a, new Asc());
        return a;
    }

    /**
     * @return a T[] containing the elements of this set
     *         ordered in ascending order by the number of times
     *         they were added to this set.
     */
    public <T> T[] toArray(T[] a)
    {
        a = set.keySet().toArray(a);
        Arrays.sort(a, new Asc());
        return a;
    }
}
