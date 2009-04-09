/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * HttpCallFuture.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import java.io.IOException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;


import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

/**
 *
 * @author ryan
 */
public class HttpCallFuture implements Runnable, Comparable<HttpCallFuture> {

    private static final int HTTP_SOCKET_TIMEOUT  = 30000;
    
    public enum Priority {
        LOW,
        DEFAULT,
        HIGH,
        YESTERDAY;
    }

    private final String       url;
    private final Priority     priority;
    private final long         created;

    private HttpCallResult     result;
    private Exception          exception;

    private volatile int       success;

    // These two fields will be set by the executor when this future is to be run
    // The client is the instance of HttpClient that this future should use to execute
    // itself, and the semaphore is the instance this future should call release() on
    // when it has completed.
    private HttpClient client;
    private Semaphore semaphore;

    public HttpCallFuture(String url) {
        this(url,Priority.DEFAULT);
    }

    public HttpCallFuture(String url, Priority priority) {
        this.url       = url;
        this.priority  = priority;
        this.created   = System.currentTimeMillis();

        this.result    = null;
        this.exception = null;
        this.success   = 0;
    }

    public void initialize(HttpClient client, Semaphore semaphore) {
        this.client    = client;
        this.semaphore = semaphore;
    }

    public boolean success() throws InterruptedException, ExecutionException {
        while (success == 0)
            wait();

        return success == 1;
    }

    public HttpCallResult get() {
        return result;
    }

    public void run() {

        HttpMethodParams methodParams = new HttpMethodParams();
        methodParams.setSoTimeout(HTTP_SOCKET_TIMEOUT);

        GetMethod method = new GetMethod(url);
        method.setFollowRedirects(true);
        method.setParams(methodParams);

        int statusCode;

        try {

            statusCode = client.executeMethod(method);
            result     = new HttpCallResult(statusCode,method.getResponseBodyAsString());

        } catch (IOException e) {
            exception = e;
        } finally { // No matter what happens, always...

            // Release the connection so it can be used again
            method.releaseConnection();

            // Then release the semaphore 
            semaphore.release();
            notifyAll();
        }
    }

    /**
     *
     * @param other The other HttpCallFuture
     * @return 1 if this future should run first, -1 if the other future should run first, 0 if they are equal
     */
    public int compareTo(HttpCallFuture other) {
        if (other == null)
            return 1;

        int thisP  = this.priority.ordinal();
        int otherP = other.priority.ordinal();

        if (thisP > otherP)
            return 1;

        if (thisP < otherP)
            return -1;

        if (this.created < other.created)
            return 1;

        if (this.created > other.created)
            return -1;

        return 0;
    }

}
