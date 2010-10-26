/*
 * ConcurrentHashSet.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util.concurrent;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the <tt>Set</tt> interface, backed by a ConcurrentHashMap instance.
 *
 * @author Matt Tucker
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> implements Set<E> {

    private transient ConcurrentHashMap<E, Object> map;

    // Dummy value to associate with an Object in the backing Map
    private final Object PRESENT = new Object();

    /**
     * Constructs a new, empty set; the backing <tt>ConcurrentHashMap</tt> instance has
     * default initial capacity (16) and load factor (0.75).
     */
    public ConcurrentHashSet() {
        map = new ConcurrentHashMap<E, Object>();
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The <tt>ConcurrentHashMap</tt> is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set.
     * @throws NullPointerException   if the specified collection is null.
     */
    public ConcurrentHashSet(Collection<? extends E> c) {
        map = new ConcurrentHashMap<E, Object>(Math.max((int) (c.size() / .75f) + 1, 16));
        addAll(c);
    }

    /**
     * Constructs a new, empty set; the backing <tt>ConcurrentHashMap</tt> instance has
     * the specified initial capacity and the specified load factor.
     *
     * @param initialCapacity the initial capacity of the hash map.
     * @param loadFactor the load factor of the hash map.
     * @throws IllegalArgumentException if the initial capacity is less
     *      than zero, or if the load factor is nonpositive.
     */
    public ConcurrentHashSet(int initialCapacity, float loadFactor) {
        map = new ConcurrentHashMap<E, Object>(initialCapacity, loadFactor, 16);
    }

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and default load factor, which is
     * <tt>0.75</tt>.
     *
     * @param      initialCapacity   the initial capacity of the hash table.
     * @throws     IllegalArgumentException if the initial capacity is less
     *             than zero.
     */
    public ConcurrentHashSet(int initialCapacity) {
        map = new ConcurrentHashMap<E, Object>(initialCapacity);
    }

    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E o) {
        return map.put(o, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    @Override
    public void clear() {
        map.clear();
    }

}