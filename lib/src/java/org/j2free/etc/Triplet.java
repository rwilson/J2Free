/*
 * Ariplet.java
 *
 * Created on November 19, 2007, 1:12 PM
 *
 */

package org.j2free.etc;

/**
 *
 * @author ryan
 */
public class Triplet<A,B,C> {
    
    private A first;
    private B second;
    private C third;
    
    public Triplet() {
    }
    
    public Triplet(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
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

    public C getThird() {
        return third;
    }

    public void setThird(C third) {
        this.third = third;
    }
    
}
