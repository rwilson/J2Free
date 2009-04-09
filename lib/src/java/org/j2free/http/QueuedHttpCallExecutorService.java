/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * QueuedHttpCallExecutorService.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author ryan
 */
public class QueuedHttpCallExecutorService {

    private static final Log log = LogFactory.getLog(QueuedHttpCallExecutorService.class);

    private static final int HTTP_CONNECT_TIMEOUT = 30000;
    private static final int MAX_CONCURRENTS  = 20;

    private static final PriorityBlockingQueue<HttpCallFuture> queue = new PriorityBlockingQueue<HttpCallFuture>();

    private static final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    private static final HttpClient httpClient = new HttpClient(connectionManager);
    private static final Semaphore semaphore = new Semaphore(MAX_CONCURRENTS);

    private static Thread worker;

    /**
     * Start the QueuedHttpCallExecutorService.  If the executor service is already running, this will
     * do nothing.
     * 
     * @return
     */
    public synchronized static void start() {

        HttpConnectionManagerParams cmParams = connectionManager.getParams();
        cmParams.setConnectionTimeout(HTTP_CONNECT_TIMEOUT);
        cmParams.setMaxTotalConnections(MAX_CONCURRENTS);

        Runnable service = new Runnable() {
            public void run() {
                try {

                    while (true) {

                        // This will block until a permit is acquired...
                        semaphore.acquireUninterruptibly();

                        // This will also block until a future is available...
                        // Run this after a permit is acquired from semaphore so as
                        // to get the right future from the priority queue only once
                        // it can be executed.
                        HttpCallFuture future = queue.take();

                        // Hand the client and semaphore to the future, so that it can
                        // execute it's method and release the permit when complete.
                        future.initialize(httpClient,semaphore);

                        // Run the future
                        new Thread(future).start();
                    }

                } catch (InterruptedException ie) {
                    log.debug("HttpCallWorker service interrupted, re-interrupting to exit...");
                    Thread.currentThread().interrupt();
                }
            }
        };
        worker = new Thread(service);
        worker.start();
    }

    public synchronized static boolean isRunning() {
        return worker.isAlive();
    }

    public static boolean enqueue(HttpCallFuture future) {

        if (future == null)
            throw new IllegalArgumentException("future cannot be null");

        if (httpClient == null || connectionManager == null)
            log.warn("QueuedHttpCallExecutorService has not been started...");
        
        return queue.offer(future);
    }

}
