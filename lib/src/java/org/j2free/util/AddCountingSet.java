/*
 * AccessCountMap.java
 *
 * Created on March 27, 2007, 11:43 PM
 *
 */

package org.j2free.util;

import java.util.*;
import org.j2free.util.concurrent.ConcurrentCountingSet;
import org.j2free.util.concurrent.CountingSet;

/**
 * @deprecated not thread-safe, use {@link CountingSet} or {@link ConcurrentCountingSet} instead.
 * @author Ryan Wilson
 */
@Deprecated
public class AddCountingSet<E> extends HashSet<E> {
    
    private HashMap<E,Integer> set;
    
    public AddCountingSet() {
        set = new HashMap<E,Integer>();
    }
    
    public AddCountingSet(int initialCapacity) {
        set = new HashMap<E,Integer>(initialCapacity);
    }
    
    public AddCountingSet(int initialCapacity, float loadFactor) {
        set = new HashMap<E,Integer>(initialCapacity,loadFactor);
    }
    
    public AddCountingSet(Collection<? extends E> c) {
        set = new HashMap<E,Integer>();
        for (E e : c) add(e);
    }
    
    public Map<E,Integer> getMap() {
        return Collections.unmodifiableMap(set);
    }
    
    public void clear() {
        set.clear();
    }
    
    public boolean add(E o) {
        if (set.containsKey(o)) {
            set.put(o,set.get(o) + 1);
            return false;
        } else {
            set.put(o,1);
            return true;
        }
    }
    
    public boolean contains(Object o) {
        return set.containsKey(o);
    }
    
    public int getAddCount(Object o) {
        return set.get(o);
    }
    
    public boolean isEmpty() {
        return set.isEmpty();
    }
    
    public Iterator<E> iterator() {
        return set.keySet().iterator();
    }
    
    public boolean remove(Object o) {
        return (set.remove(o) != null);
    }
    
    public int size() {
        return set.size();
    }
    
    public List<E> orderedList(final boolean asc) {
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

    public HashMap<E,Integer> getOverThreshold(int threshold) {
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
