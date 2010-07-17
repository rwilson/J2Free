/*
 * AlphaNumericSequencer.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util.concurrent;

import java.util.concurrent.atomic.AtomicLong;
import net.jcip.annotations.ThreadSafe;

/**
 * A sequencer returning a String consisting of a-z, A-Z, 0-9
 *
 *
 * @author ryan
 */
@ThreadSafe
public class AlphaNumericSequencer
{
    private final AtomicLong number;

    public AlphaNumericSequencer()
    {
        this(0l);
    }

    public AlphaNumericSequencer(long seed)
    {
        number = new AtomicLong(seed);
    }

    public String next()
    {
        return Long.toString(number.incrementAndGet(), Character.MAX_RADIX);
    }
}
