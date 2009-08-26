/*
 * BitInt.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util;

import net.jcip.annotations.ThreadSafe;

/**
 * A wrapper to use around objects to set or unset
 * flag bits. Usage:
 * <pre>
 *      BitInt bits = new BitInt(entity.getBitPreferences());
 *
 *      // flag any bit positions
 *      bits.set(PREFERENCE_X, true);
 *      bits.set(PREFERENCE_Y, false);
 *
 *      // get the result
 *      entity.setBitPreferences(bits.intValue());
 * </pre>
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class BitInt {

    public static enum OnFlag {
        ZERO(0),
        ONE(1);

        final int bit;

        OnFlag(int bit) {
            this.bit = bit;
        }
    };

    private int value;
    private final OnFlag flag;

    public BitInt() {
        this(0);
    }

    public BitInt(int i) {
        this(i,OnFlag.ONE);
    }

    public BitInt(int i, OnFlag flag) {
        this.value = i;
        this.flag = flag;
    }

    public synchronized boolean isSet(int pos) {
        return (value & pos) == flag.bit;
    }

    public synchronized void set(int pos, boolean on) {
        if (!isSet(pos) == on)
            value ^= pos;
    }

    public synchronized void set(int pos, int on) {
        if ((value & pos) != on)
            value ^= pos;
    }

    public synchronized void toggle(int pos) {
        value ^= pos;
    }

    public synchronized int intValue() {
        return value;
    }
}
