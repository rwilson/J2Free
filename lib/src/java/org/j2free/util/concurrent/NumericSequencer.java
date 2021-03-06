/**
 * NumericSequencer.java
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
package org.j2free.util.concurrent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A basic numeric sequencer built on an AtomicInteger with the distinction
 * that it supports the concept of "releasing" a number, which can then be
 * handed out to another caller at the next invocation of <tt>next()</tt>.
 *
 * @author Ryan Wilson
 */
public class NumericSequencer
{
    private final AtomicInteger number;
    private final ConcurrentLinkedQueue<Integer> returned;

    /**
     *
     */
    public NumericSequencer()
    {
        number   = new AtomicInteger(0);
        returned = new ConcurrentLinkedQueue<Integer>();
    }

    /**
     *
     * @return
     */
    public int next()
    {
        Integer n = returned.poll();
        return n == null ? number.getAndIncrement() : n;
    }

    /**
     * 
     * @param num
     */
    public void release(final int num)
    {
        returned.offer(num);
    }
}
