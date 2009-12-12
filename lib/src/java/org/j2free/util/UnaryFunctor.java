package org.j2free.util;

/**
 * This is just a shell which can be instantiated
 * and then defined inline. e.g.
 * 
 * @author Ryan Wilson
 */
public interface UnaryFunctor<A> {

    public <R> R run(A arg);

}