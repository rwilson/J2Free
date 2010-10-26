/*
 * Arrays.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.jsp.el;

/**
 *
 * @author Ryan Wilson
 */
public class Arrays
{
    public static int length(Object[] arr) {
        return arr == null ? 0 : arr.length;
    }
}
