/*
 * Pair.java
 *
 * Created on November 19, 2007, 1:10 PM
 *
 */

package org.j2free.util;

/**
 *
 * @author ryan
 */
public class Pair<A,B> {
    
    private A first;
    private B second;
    
    public Pair() {
    }
    
    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }
    
}
