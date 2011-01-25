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
public class BitInt
{
    public static enum OnFlag
    {
        ZERO(0),
        ONE(1);

        final int bitValue;

        OnFlag(int bitValue) {
            this.bitValue = bitValue;
        }
    };

    private int value;
    private final OnFlag flag;

    public BitInt()
    {
        this(0);
    }

    public BitInt(int initialValue)
    {
        this(initialValue, OnFlag.ONE);
    }

    public BitInt(int initialValue, OnFlag flag)
    {
        this.value = initialValue;
        this.flag = flag;
    }

    /**
     * @param pos should be an int with only 1 bit set
     */
    public synchronized boolean isSet(int pos)
    {
        if (flag == OnFlag.ONE)
            return (value & pos) == pos;
        else
            return (value & pos) == 0;
    }

    /**
     * @param pos should be an int with only 1 bit set
     */
    public synchronized void set(int pos, boolean on)
    {
        if (isSet(pos) != on) // if ((isSet(pos) && !on) || (!isSet(pos) && on))
            toggle(pos);
    }

    /**
     * @param pos should be an int with only 1 bit set
     */
    public synchronized void set(int pos, int desired)
    {
        if (numberOfSetBitsIn(pos) != 1)
            throw new IllegalArgumentException("pos must have only 1 bit set!");
        
        if (desired != 0 && desired != 1)
            throw new IllegalArgumentException("A BitInt position cannot be: " + desired);
        
        if ((value & pos) != desired)
            value ^= pos;
    }

    /**
     * @param pos should be an int with only 1 bit set
     */
    public synchronized void toggle(int pos)
    {
        value ^= pos;
    }

    public synchronized int intValue()
    {
        return value;
    }

    private int numberOfSetBitsIn(int i)
    {
        i = i - ((i >> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
        return ((i + (i >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;
    }
}
