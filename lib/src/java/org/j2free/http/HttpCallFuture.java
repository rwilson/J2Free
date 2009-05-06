/*
 * HttpCallFuture.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.jcip.annotations.ThreadSafe;


/**
 * Convenience class that wraps a HttpCallTask and Future
 * representing the result of the HttpCallTask.
 *
 * @author ryan
 */
@ThreadSafe
public class HttpCallFuture {

    private final HttpCallTask task;
    
    private Future<HttpCallResult> future;

    public HttpCallFuture(HttpCallTask task, Future<HttpCallResult> future) {
        this.task   = task;
        this.future = future;
    }

    /**
     * Will block until the result is available
     *
     * @return
     * @throws java.lang.InterruptedException
     */
    public HttpCallResult getResult() throws InterruptedException, ExecutionException {
        return future.get();
    }

    /**
     * Will block until the result is available or for
     * the specified amount of time.
     *
     * @return
     * @throws java.lang.InterruptedException
     */
    public HttpCallResult getResult(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout,unit);
    }

}
