/*
 * QueuedHttpCallExecutorService.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import java.io.IOException;

import java.util.List;
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
 * <code>QueuedExecutorService</code> is thread safe because shared state management is
 * delegated to the <code>ThreadPoolExecutor</code>, which in turn uses a <code>PriorityBlockingQueue</code>.
 *
 * @author ryan
 */
@ThreadSafe
public class QueuedHttpCallExecutorService {

    private static final Log log = LogFactory.getLog(QueuedHttpCallExecutorService.class);

    private static final int HTTP_CONNECT_TIMEOUT = 30000;
    private static final int HTTP_SOCKET_TIMEOUT  = 30000;

    private static final int MAX_POOL     = 20;
    private static final int CORE_POOL    = MAX_POOL / 4;
    private static final long THREAD_IDLE = 180;

    private final ThreadPoolExecutor executor;

    private final MultiThreadedHttpConnectionManager connectionManager;
    private final HttpClient httpClient;

    public QueuedHttpCallExecutorService() {
        // Using a PriorityBlockingQueue for the underlying queue means this executor will
        // order queued tasks by the natural ordering of the tasks, in this case HttpCallFutures.
        executor = new ThreadPoolExecutor(CORE_POOL,MAX_POOL,THREAD_IDLE, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());

        connectionManager = new MultiThreadedHttpConnectionManager();

        // Configure the ConnectionManager
        HttpConnectionManagerParams cmParams = connectionManager.getParams();
        cmParams.setConnectionTimeout(HTTP_CONNECT_TIMEOUT);
        cmParams.setSoTimeout(HTTP_SOCKET_TIMEOUT);
        cmParams.setMaxTotalConnections(MAX_POOL);

        // Create the HttpClient
        httpClient = new HttpClient(connectionManager);
    }

    public void submit(final HttpCallFuture task) {

        if (task == null)
            throw new IllegalArgumentException("HttpCallFuture cannot be null");

        executor.execute(new Runnable() {
            public void run() {
                
                GetMethod method = new GetMethod(task.getUrl());
                method.setFollowRedirects(true);

                int statusCode;
                try {

                    statusCode = httpClient.executeMethod(method);
                    task.setResult(new HttpCallResult(statusCode,method.getResponseBodyAsString()));

                } catch (IOException e) {
                    throw LaunderThrowable.launderThrowable(e);
                } finally {
                    method.releaseConnection();
                }
            }
        });
    }

    public boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException {
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }

    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }
}