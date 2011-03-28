/*
 * PriorityFuture.java
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

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import net.jcip.annotations.ThreadSafe;

/**
 * A generic Future capable of associating a <tt>Priority</tt>
 * with a <tt>Runnable</tt> or <tt>Callable</tt>. Designed
 * specifically for using <tt>PriorityBlockingQueue</tt> as the
 * work queue for a <tt>ThreadPoolExecutor</tt>, which can only
 * accept a <tt>PriorityBlockingQueue&lt;Runnable&gt;</tt>.
 *
 * <tt>PriorityFuture</tt> will, when sorted, be ordered first
 * by priority, then FIFO.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class PriorityFuture<V> extends FutureTask<V> implements Comparable<PriorityFuture<V>> {

    private final Priority priority;
    private final long     created;

    public PriorityFuture(Callable<V> callable) {
        this(callable, Priority.DEFAULT);
    }

    public PriorityFuture(Callable<V> callable, Priority priority) {
        super(callable);
        this.priority = priority;
        this.created  = System.currentTimeMillis();
    }

    public PriorityFuture(Runnable runnable, V result) {
        this(runnable, result, Priority.DEFAULT);
    }

    public PriorityFuture(Runnable runnable, V result, Priority priority) {
        super(runnable, result);
        this.priority = priority;
        this.created  = System.currentTimeMillis();
    }

    public Priority getPriority() {
        return priority;
    }

    public int compareTo(PriorityFuture<V> other) {
        if (other == null)
            return 1;

        int cmp = this.priority.compareTo(other.priority);
        if (cmp != 0) return cmp;

        return Float.valueOf(Math.signum(other.created - this.created)).intValue();
    }

}
