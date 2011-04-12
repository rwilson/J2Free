/*
 * Triplet.java
 *
 * Created on November 19, 2007, 1:12 PM
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.util;

/**
 *
 * @param <A> 
 * @param <B>
 * @param <C>
 * @author Ryan Wilson
 */
public class Triplet<A,B,C> {
    
    private A first;
    private B second;
    private C third;
    
    /**
     * 
     */
    public Triplet()
    {
    }
    
    /**
     * 
     * @param first
     * @param second
     * @param third
     */
    public Triplet(A first, B second, C third)
    {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /**
     * 
     * @return
     */
    public A getFirst()
    {
        return first;
    }

    /**
     * 
     * @param first
     */
    public void setFirst(A first)
    {
        this.first = first;
    }

    /**
     * 
     * @return
     */
    public B getSecond()
    {
        return second;
    }

    /**
     * 
     * @param second
     */
    public void setSecond(B second)
    {
        this.second = second;
    }

    /**
     * 
     * @return
     */
    public C getThird()
    {
        return third;
    }

    /**
     * 
     * @param third
     */
    public void setThird(C third)
    {
        this.third = third;
    }
    
}
