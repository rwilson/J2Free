/*
 * BoundedDiscardingQueue.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util.concurrent;

import java.util.LinkedList;
import java.util.List;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 *  A bounded list meant to hold up to a certain number of elements.
 *  Inserting an element when the list is full does not block, as a
 *  bounded queue would, but rather atomically discards the oldest 
 *  element and inserts the new element.
 *
 *  Unlike what the name may suggest, this structure does not implement
 *  List or Collection.
 *
 *  The size is set at construction, and cannot be modified.
 *
 *  Items returned by asList are stored in FIFO-order.
 *
 * @author ryan
 */
@ThreadSafe
public class DiscardingArrayList<E> {

    @GuardedBy("this") private final E[] buf;
    @GuardedBy("this") private int head;
    @GuardedBy("this") private int count;

    public DiscardingArrayList(int capacity) {
        buf = (E[]) new Object[capacity];
        head  = 0;
        count = 0;
    }

    public synchronized void clear() {
        for (int i = 0; i < buf.length; i++) {
            buf[i] = null;
        }
        head  = 0;
        count = 0;
    }

    public synchronized int size() {
        return count;
    }

    public synchronized E add(E o) {
        E previous = buf[head];

        buf[head] = o;
        
        if (++head == buf.length)
            head = 0;

        if (count < buf.length)
            count++;
        
        return previous;
    }

    public synchronized List<E> asList() {
        List<E> list = new LinkedList<E>();

        // If we haven't filled up the buffer, just get the elements starting at 0
        if (count < buf.length) {
            for (int i = 0; i < count; i++) {
                list.add(buf[i]);
            }
        } else {
            // If the buffer is full, then the oldest item is head (since head is incremented AFTER each set)
            for (int i = 0, j = head; i < count; i++, j = (++j == buf.length) ? 0 : j) {
                list.add(buf[j]);
            }
        }
        return list;
    }

    // Testing method...
    /* 
    public static void main(String[] args) {
        DiscardingArrayList<Integer> bdl = new DiscardingArrayList<Integer>(10);

        for (int i = 0; i < 50; i++) {
            bdl.add(i);
            if (i % 5 == 0) {
                List<Integer> l = bdl.asList();
                System.out.println();
                System.out.print(i + ": ");
                for (int j : l)
                    System.out.print(j + " ");
            }
        }
    }
    */
}
