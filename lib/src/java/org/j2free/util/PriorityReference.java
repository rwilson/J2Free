/*
 * PriorityReference.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util;

import org.j2free.util.Priority;

/**
 * A generic reference, capable of associating an element
 * with a priority. Designed specifically for using
 * PriorityBlockingQueues with classes that do not implement
 * <tt>Comparable</tt> and that do not have a field which
 * can be used to order by priority using a <tt>Comparator</tt>
 *
 * @author ryan
 */
public class PriorityReference<E> implements Comparable<PriorityReference<E>> {

    private final Priority priority;
    private final E        element;

    public PriorityReference(E element) {
        this(element,Priority.DEFAULT);
    }

    public PriorityReference(E element, Priority priority) {
        this.element  = element;
        this.priority = priority;
    }

    public E get() {
        return element;
    }

    public int compareTo(PriorityReference<E> other) {
        if (other == null)
            return 1;

        return this.priority.compareTo(other.priority);
    }

}
