/*
 * PriorityFuture.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util;

import java.util.Date;
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
 * by priority, and second by ths time of the <tt>PriorityFuture</tt>
 * creation, which it maintains internally.
 *
 * @author ryan
 */
@ThreadSafe
public class PriorityFuture<V> extends FutureTask<V> implements Comparable<PriorityFuture<V>> {

    private final Priority priority;
    private final Date     created;

    public PriorityFuture(Callable<V> callable) {
        this(callable,Priority.DEFAULT);
    }

    public PriorityFuture(Callable<V> callable, Priority priority) {
        super(callable);
        this.priority = priority;
        this.created  = new Date();
    }

    public PriorityFuture(Runnable runnable, V result) {
        this(runnable,result,Priority.DEFAULT);
    }

    public PriorityFuture(Runnable runnable, V result, Priority priority) {
        super(runnable, result);
        this.priority = priority;
        this.created  = new Date();
    }

    public Priority getPriority() {
        return priority;
    }

    public int compareTo(PriorityFuture<V> other) {
        if (other == null)
            return 1;

        int cmp = this.priority.compareTo(other.priority);

        return cmp == 0 ? this.created.compareTo(other.created) : cmp;
    }

}
