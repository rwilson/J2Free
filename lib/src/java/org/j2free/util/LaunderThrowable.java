package org.j2free.util;

/**
 * StaticUtilities
 *
 * @author Brian Goetz and Tim Peierls
 *
 * From Java Concurrency in Practice
 */
public class LaunderThrowable {

    /**
     * Coerce an unchecked Throwable to a RuntimeException
     * 
     * If the Throwable is an Error, throw it; if it is a
     * RuntimeException return it, otherwise throw IllegalStateException
     */
    public static RuntimeException launderThrowable(Throwable t) {
        if (t instanceof RuntimeException)
            return (RuntimeException) t;
        else if (t instanceof Error)
            throw (Error) t;
        else
            throw new IllegalStateException("Not unchecked", t);
    }
}