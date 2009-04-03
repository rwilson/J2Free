package org.j2free.cache;

/**
 *
 * @author Brian Goetz and Tim Peierls
 */
public interface Computable<A, V> {
    V compute(A arg) throws InterruptedException;
}
