/*
 * QueuedHttpCallService.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import java.io.IOException;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.util.LaunderThrowable;

/**
 * <tt>QueuedExecutorService</tt> a thread-safe service with static methods
 * to execute http calls in priority order.  Internally, uses a <tt>ThreadPoolExecutor</tt>
 * that is configured to use a <tt>PriorityBlockingQueue</tt> to order tasks
 * by priority.
 *
 * @author ryan
 */
@ThreadSafe
public class QueuedHttpCallService {

    private static final Log log = LogFactory.getLog(QueuedHttpCallService.class);

    private static final int HTTP_CONNECT_TIMEOUT = 30000;
    private static final int HTTP_SOCKET_TIMEOUT  = 30000;

    private static final int MAX_POOL     = 20;
    private static final int CORE_POOL    = MAX_POOL / 4;
    private static final long THREAD_IDLE = 180;

    private static final ThreadPoolExecutor executor;
    private static final MultiThreadedHttpConnectionManager connectionManager;
    private static final HttpClient httpClient;

    static {

        executor = new ThreadPoolExecutor(
                CORE_POOL,
                MAX_POOL,
                THREAD_IDLE,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<Runnable>()
            );

        connectionManager = new MultiThreadedHttpConnectionManager();

        // Configure the ConnectionManager
        HttpConnectionManagerParams cmParams = connectionManager.getParams();
        cmParams.setConnectionTimeout(HTTP_CONNECT_TIMEOUT);
        cmParams.setSoTimeout(HTTP_SOCKET_TIMEOUT);
        cmParams.setMaxTotalConnections(MAX_POOL);

        httpClient = new HttpClient(connectionManager);
    }

    public static HttpCallFuture submit(final HttpCallTask task) {

        if (task == null)
            throw new IllegalArgumentException("HttpCallTask cannot be null");

        Future<HttpCallResult> resultFuture = executor.submit(new HttpCallable(task));

        return new HttpCallFuture(task,resultFuture);
    }

    public static boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException {
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }

    public static List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    /**
     * Implementation of Callable that takes a HttpCallTask and,
     * when called, returns the result of the task as a HttpCallResult.
     */
    private static class HttpCallable implements Callable<HttpCallResult> {

        private final HttpCallTask task;

        public HttpCallable(HttpCallTask task) {
            this.task = task;
        }

        public HttpCallResult call() {

            GetMethod method = new GetMethod(task.getUrl());
            method.setFollowRedirects(true);

            int statusCode;
            try {

                log.debug("Making HTTP call [url=" + task.getUrl() + "]");

                statusCode = httpClient.executeMethod(method);
                log.debug("Call returned [status=" + statusCode + "]");

                return new HttpCallResult(statusCode,method.getResponseBodyAsString());

            } catch (IOException e) {
                throw LaunderThrowable.launderThrowable(e);
            } finally {
                method.releaseConnection();
            }
        }
    }
}