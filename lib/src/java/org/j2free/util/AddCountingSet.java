/*
 * AccessCountMap.java
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
package org.j2free.util;

import java.util.*;
import org.j2free.util.concurrent.ConcurrentCountingSet;
import org.j2free.util.concurrent.CountingSet;

/**
 * @param <E>
 * @deprecated not thread-safe, use {@link CountingSet} or {@link ConcurrentCountingSet} instead.
 * @author Ryan Wilson
 */
@Deprecated
public class AddCountingSet<E> extends HashSet<E> {
    
    private HashMap<E,Integer> set;
    
    /**
     * 
     */
    public AddCountingSet()
    {
        set = new HashMap<E,Integer>();
    }
    
    /**
     * 
     * @param initialCapacity
     */
    public AddCountingSet(int initialCapacity)
    {
        set = new HashMap<E,Integer>(initialCapacity);
    }
    
    /**
     * 
     * @param initialCapacity
     * @param loadFactor
     */
    public AddCountingSet(int initialCapacity, float loadFactor)
    {
        set = new HashMap<E,Integer>(initialCapacity,loadFactor);
    }
    
    /**
     * 
     * @param c
     */
    public AddCountingSet(Collection<? extends E> c)
    {
        set = new HashMap<E,Integer>();
        for (E e : c) add(e);
    }
    
    /**
     * 
     * @return
     */
    public Map<E,Integer> getMap()
    {
        return Collections.unmodifiableMap(set);
    }
    
    /**
     * 
     */
    public void clear()
    {
        set.clear();
    }
    
    /**
     * 
     * @param o
     * @return
     */
    public boolean add(E o)
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
     * 
     * @param o
     * @return
     */
    public boolean contains(Object o)
    {
        return set.containsKey(o);
    }
    
    /**
     * 
     * @param o
     * @return
     */
    public int getAddCount(Object o)
    {
        return set.get(o);
    }
    
    /**
     * 
     * @return
     */
    public boolean isEmpty()
    {
        return set.isEmpty();
    }
    
    /**
     * 
     * @return
     */
    public Iterator<E> iterator()
    {
        return set.keySet().iterator();
    }
    
    /**
     * 
     * @param o
     * @return
     */
    public boolean remove(Object o)
    {
        return (set.remove(o) != null);
    }
    
    /**
     * 
     * @return
     */
    public int size()
    {
        return set.size();
    }
    
    /**
     * 
     * @param asc
     * @return
     */
    public List<E> orderedList(final boolean asc)
    {
        ArrayList<E> ordered = new ArrayList<E>(set.size());
        for (E e : set.keySet())
            ordered.add(e);
        
        Collections.sort(ordered, new Comparator<E>() {
            public int compare(E one, E two) {
                int oneC = set.get(one);
                int twoC = set.get(two);
                
                if (oneC > twoC)
                    return (asc) ? -1 : 1;
                
                return (asc) ? 1 : -1;
            }
        });
        
        return ordered;
    }

    /**
     * 
     * @param threshold
     * @return
     */
    public HashMap<E,Integer> getOverThreshold(int threshold)
    {
        HashMap<E,Integer> over = new HashMap<E,Integer>();
        Iterator<Map.Entry<E,Integer>> setItr = set.entrySet().iterator();
        Map.Entry<E,Integer> ent = null;
        while (setItr.hasNext()) {
            ent = setItr.next();
            if (ent.getValue() > threshold)
                over.put(ent.getKey(),ent.getValue());
        }
        return over;
    }
    
}
