/*
 * QueuedHttpCallService.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;


import java.io.IOException;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <tt>QueuedExecutorService</tt> a thread-safe service with static methods
 * to execute http calls in priority order.  Internally, uses a {@link ThreadPoolExecutor}
 * that is configured to use a {@link PriorityBlockingQueue} to order tasks
 * by priority.
 *
 * The {@link HttpCallTask} objects are executed in instances of the internal
 * {@link HttpCallable}, which is intentionally not published.  <tt>QueuedHttpCallService</tt>
 * is final specifically to prevent subclassing from publishing {@link HttpCallable} to
 * alien code.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public final class QueuedHttpCallService {

    private static final Log log = LogFactory.getLog(QueuedHttpCallService.class);

    private static ThreadPoolExecutor                 executor;
    private static MultiThreadedHttpConnectionManager connectionManager;
    private static HttpClient                         httpClient;

    public static HttpCallFuture submit(final HttpCallTask task) {

        if (task == null)
            throw new IllegalArgumentException("HttpCallTask cannot be null");

        if (executor == null)
            throw new IllegalStateException("Invalid operation, QueuedHttpCallService has not been started");

        HttpCallFuture future = new HttpCallFuture(task, new HttpCallable(task));
        executor.execute(future);

        return future;
    }

    public static void enable(int maxPoolSize, long threadIdle, int connectTimeout, int socketTimeout) {

        if (executor != null) {
            if (executor.isShutdown() || executor.isTerminated())
                throw new IllegalStateException("Invalid operation, QueuedHttpCallService is already shutdown");
            if (executor.isTerminating())
                throw new IllegalStateException("Invalid operation, QueuedHttpCallService is shutting down");

            throw new IllegalStateException("Invalid operation, QueuedHttpCallService is already started");
        }

        if (maxPoolSize < 0)
            maxPoolSize = Integer.MAX_VALUE;

        executor = new ThreadPoolExecutor(
                maxPoolSize,
                maxPoolSize,
                threadIdle,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<Runnable>()
            );
        //executor.allowCoreThreadTimeOut(true); // Only available in 1.6

        connectionManager = new MultiThreadedHttpConnectionManager();

        // Configure the ConnectionManager
        HttpConnectionManagerParams cmParams = connectionManager.getParams();
        cmParams.setConnectionTimeout(connectTimeout * 1000);
        cmParams.setSoTimeout(socketTimeout * 1000);
        cmParams.setMaxTotalConnections(maxPoolSize);

        httpClient = new HttpClient(connectionManager);
    }

    public static boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException {
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }

    public static List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    public static Report reportStatus() {

        if (executor == null)
            throw new IllegalStateException("Invalid operation, QueuedHttpCallService has not been started");

        return new Report(
                executor.getPoolSize(),             // current Pool size
                executor.getLargestPoolSize(),      // largest ever since startup
                executor.getMaximumPoolSize(),      // max pool size
                executor.getActiveCount(),          // threads actively executing tasks
                executor.getTaskCount(),            // num tasks submitted
                executor.getCompletedTaskCount()    // num tasks completed
            );
    }

    /**
     * Implementation of Callable that takes a HttpCallTask and,
     * when called, returns the result of the task as a HttpCallResult.
     *
     * This class is <tt>protected</tt> because it needs to be seen by <tt>HttpCallFuture</tt>
     * but should never be access outside of <tt>HttpCallFuture</tt> or <tt>QueuedHttpCallService</tt>
     */
    protected static class HttpCallable implements Callable<HttpCallResult> {

        private final HttpCallTask task;

        private HttpCallable(HttpCallTask task) {
            this.task = task;
        }

        public HttpCallResult call() throws IOException {
            
            HttpMethod method;

            List<HttpQueryParam> params = task.getQueryParams();

            if (task.method == HttpCallTask.Method.GET) {
                method = new GetMethod(task.url);
                
                StringBuilder query = new StringBuilder();
                
                boolean first = true;
                for (HttpQueryParam param : params) {
                    if (first) {
                        query.append("?");
                        first = false;
                    } else {
                        query.append("&");
                    }

                    query.append(param.name + "=" + param.value);
                }

            } else {
                
                method = new PostMethod(task.url);

                NameValuePair[] data = new NameValuePair[params.size()];
                int i = 0;
                for (HttpQueryParam param : params) {
                    data[i] = new NameValuePair(param.name, param.value);
                    i++;
                }
                
                ((PostMethod)method).setRequestBody(data);
            }

            for (Header header : task.getRequestHeaders())
                method.setRequestHeader(header);

            method.setFollowRedirects(task.followRedirects);

            try {

                if (log.isDebugEnabled()) 
                    log.debug("Making HTTP call [url=" + task.url + "]");
                
                httpClient.executeMethod(method);
                
                if (log.isDebugEnabled())
                    log.debug("Call returned [status=" + method.getStatusCode() + "]");

                return new HttpCallResult(method);

            } finally {
                method.releaseConnection();
            }
        }
    }

    /**
     * Class for holding the reporting the current state of
     * the QueueHttpCallService
     */
    public static class Report {

        private final int currentPoolSize;
        private final int largestPoolSize;
        private final int maxPoolSize;

        private final int activeThreadCount;

        private final long totalTaskCount;
        private final long completedTaskCount;

        /**
         * @param the corePoolSize
         * @param the currentPoolSize
         * @param the largestPoolSize since startup
         * @param the maxPoolSize
         * @param the activeThreadCount
         * @param the totalTaskCount - total tasks submitted since start
         * @param the completedTaskCount - total tasks completed since start
         */
        public Report(int curPS, int largestPS, int maxPS, int activeTC, long totalTC, long completedTC) {
            this.currentPoolSize    = curPS;
            this.largestPoolSize    = largestPS;
            this.maxPoolSize        = maxPS;
            this.activeThreadCount  = activeTC;
            this.totalTaskCount     = totalTC;
            this.completedTaskCount = completedTC;
        }

        /**
         * @return the currentPoolSize
         */
        public int getCurrentPoolSize() {
            return currentPoolSize;
        }

        /**
         * @return the largestPoolSize
         */
        public int getLargestPoolSize() {
            return largestPoolSize;
        }

        /**
         * @return the maxPoolSize
         */
        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        /**
         * @return the activeThreadCount
         */
        public int getActiveThreadCount() {
            return activeThreadCount;
        }

        /**
         * @return the totalTaskCount
         */
        public long getTotalTaskCount() {
            return totalTaskCount;
        }

        /**
         * @return the completedTaskCount
         */
        public long getCompletedTaskCount() {
            return completedTaskCount;
        }

    }
}