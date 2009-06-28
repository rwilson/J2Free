/*
 * Computable.java
 *
 * Created on June 27, 2009
 *
 */

package org.j2free.util.concurrent;

/**
 *
 * @author Ryan
 */
public interface Computable<A,V> {

    public V compute(A arg) throws InterruptedException;

}
