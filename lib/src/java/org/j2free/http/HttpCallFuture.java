/*
 * HttpCallFuture.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import java.util.concurrent.FutureTask;

import net.jcip.annotations.ThreadSafe;
import org.j2free.http.QueuedHttpCallService.HttpCallable;

/**
 * Extends FutureTask so that instances of HttpCallTask can be ordered by
 * priority in a PriorityBlockingQueue in the QueuedHttpCallService.
 *
 * @author ryan
 */
@ThreadSafe
public final class HttpCallFuture extends FutureTask<HttpCallResult> implements Comparable<HttpCallFuture> {

    private final HttpCallTask task;

    public HttpCallFuture(HttpCallTask task, HttpCallable callable) {
        super(callable);
        this.task = task;
    }

    /**
     * This implementation of comparable compares <tt>HttpCallFuture</tt> instances
     * calling <tt>compareTo</tt> on the underlying <tt>HttpCallTask</tt> objects.
     */
    public int compareTo(HttpCallFuture other) {
        return task.compareTo(other.task);
    }
}
