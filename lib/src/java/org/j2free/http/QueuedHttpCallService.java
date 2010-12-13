/*
 * QueuedHttpCallService.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.http;

import java.util.List;

import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <tt>QueuedExecutorService</tt> a thread-safe service with methods
 * to execute http calls in priority order.  Internally, uses a {@link ThreadPoolExecutor}
 * that is configured to use a {@link PriorityBlockingQueue} to order tasks
 * by priority.
 *
 * The {@link HttpCallTask} objects are executed in instances of  {@link HttpCallable}.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public final class QueuedHttpCallService implements HttpCallService
{
    private final Log log = LogFactory.getLog(QueuedHttpCallService.class);

    private final ThreadPoolExecutor                 executor;
    private final MultiThreadedHttpConnectionManager connectionManager;
    private final HttpClient                         httpClient;

    /**
     * Enables this service.
     *
     * @param maxPoolSize The max number of threads
     * @param threadIdle How long a thread can be idle before terminating it
     * @param connectTimeout How long to wait for a connection
     * @param socketTimeout How long to wait for an operation
     * @throws IllegalStateException if called when the service is already running
     */
    public QueuedHttpCallService(int corePoolSize,  int maxPoolSize, long threadIdle,
                                 int connectTimeout, int socketTimeout)
    {
        if (maxPoolSize < 0)
            maxPoolSize = Integer.MAX_VALUE;

        executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                threadIdle,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<Runnable>(100)
            );

        executor.allowCoreThreadTimeOut(true);  // allow the threads to timeout if unused
        executor.prestartCoreThread();          // start up just one thread

        connectionManager = new MultiThreadedHttpConnectionManager();

        // Configure the ConnectionManager
        HttpConnectionManagerParams cmParams = connectionManager.getParams();
        cmParams.setConnectionTimeout(connectTimeout * 1000);
        cmParams.setSoTimeout(socketTimeout * 1000);
        cmParams.setMaxTotalConnections(maxPoolSize);

        httpClient = new HttpClient(connectionManager);
    }

    /**
     * Submits an HttpCallTask for execution
     * 
     * @param task The task to execute
     * @return a HttpCallFuture representing the result of executing
     *         the specified task
     */
    public Future<HttpCallResult> submit(final HttpCallTask task)
    {
        if (task == null)
            throw new IllegalArgumentException("HttpCallTask cannot be null");

        /**
         * Why is this all necessary?
         *  - Only tasks submitted with execute will end up in the PriorityBlockingQueue
         *  - ThreadPoolExecutor.execute will only accept Runnables
         *  - To return a result, we need to submit a Callable
         *  - So we wrap a Callable in a FutureTask, but FutureTask is not Comparable
         *  - So, we use a HttpCallFuture, which extends FutureTask to implement Comparable
         *  - But, we don't want the user to be able to arbitrarily execute the FutureTask
         *    which they could if we returned the HttpCallFuture, so we just return it as
         *    a Future.
         */

        HttpCallFuture future = new HttpCallFuture(task, httpClient);
        executor.execute(future);
        return future;
    }

    /**
     * @return true of this service is currently accepting new tasks
     */
    public boolean isEnabled()
    {
        return !executor.isShutdown() &&
               !executor.isTerminated() &&
               !executor.isTerminating();
    }

    /**
     * Attempts to shutdown the service in an ordely fashion, allowing all
     * queued tasks to finish executing but not accepting any new tasks.
     *
     * @param timeout how long to wait
     * @param unit
     * @return true if the service is shutdown as a result of this call, otherwise false
     * @throws InterruptedException if the current thread is interrupted while waiting
     *         for the executor to shutdown.
     */
    public boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException
    {
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }

    /**
     * Shuts down the service immediately, terminating any running tasks
     * @return a list of tasks running or waiting to be run
     */
    public List<Runnable> shutdownNow()
    {
        return executor.shutdownNow();
    }

    /**
     * @return The status of this service
     */
    public HttpServiceReport reportStatus()
    {
        return new HttpServiceReport(
                executor.getPoolSize(),             // current Pool size
                executor.getLargestPoolSize(),      // largest ever since startup
                executor.getMaximumPoolSize(),      // max pool size
                executor.getActiveCount(),          // threads actively executing tasks
                executor.getTaskCount(),            // num tasks submitted
                executor.getCompletedTaskCount()    // num tasks completed
            );
    }
}