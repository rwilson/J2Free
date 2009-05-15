/*
 * NumericSequencer.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util.concurrent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A basic numeric sequencer built on an AtomicInteger with the distinction
 * that it supports the concept of "releasing" a number, which can then be
 * handed out to another caller at the next invocation of <tt>next()</tt>.
 *
 * @author Ryan Wilson
 */
public class NumericSequencer {

    private final AtomicInteger number;
    private final ConcurrentLinkedQueue<Integer> returned;

    public NumericSequencer() {
        number   = new AtomicInteger(0);
        returned = new ConcurrentLinkedQueue<Integer>();
    }

    public int next() {
        Integer n = returned.poll();
        return n == null ? number.getAndIncrement() : n;
    }

    public void release(final int num) {
        returned.offer(num);
    }
}
