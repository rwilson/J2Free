/*
 * NumericSequencer.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author ryan
 */
public class NumericSequencer {

    private final AtomicInteger number;

    public NumericSequencer() {
        number = new AtomicInteger(0);
    }

    public int next() {
        return number.getAndIncrement();
    }

}
