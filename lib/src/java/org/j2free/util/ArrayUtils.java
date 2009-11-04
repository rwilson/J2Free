/*
 * ArrayUtils.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Utility class for array functions I can't find elswhere...
 *
 * @author Ryan Wilson
 */
public class ArrayUtils {

    /**
     * @param a an array of T
     * @param b an array or varargs list of T
     * @return an array of T containing all of a, then all of b
     */
    public static <T> T[] asOne(T[] a, T... b) {
        ArrayList<T> list = new ArrayList<T>(a.length + b.length);
        list.addAll(Arrays.asList(a));
        list.addAll(Arrays.asList(b));
        return list.toArray(a);
    }
}
