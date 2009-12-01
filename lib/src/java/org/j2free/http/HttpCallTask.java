/*
 * HttpCallTask.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import java.util.Map;
import net.jcip.annotations.ThreadSafe;

import org.apache.commons.httpclient.Header;

import org.j2free.util.KeyValuePair;
import org.j2free.util.Priority;

/**
 * Thread-safe representation of a HTTP call that can be sorted
 * by priority.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class HttpCallTask implements Comparable<HttpCallTask> {

    public static enum Method {
        GET,
        POST
    };

    public final Method method;

    private final List<KeyValuePair<String,String>> queryParams;
    private final List<Header> requestHeaders;

    public final String url;
    public final boolean followRedirects;
    public final Priority priority;
    public final Date created;

    /**
     * Equivalent to:
     * <pre>
     *  HttpCallTask(HttpCallTask.Method.GET, url);
     * </pre>
     */
    public HttpCallTask(String url) {
        this(Method.GET,url);
    }

    /**
     * Equivalent to:
     * <pre>
     *  HttpCallTask(HttpCallTask.Method.GET, url, false);
     * </pre>
     */
    public HttpCallTask(Method method, String url) {
        this(method,url,false);
    }

    /**
     * Equivalent to:
     * <pre>
     *  HttpCallTask(HttpCallTask.Method.GET, url, false, Priority.DEFAULT);
     * </pre>
     */
    public HttpCallTask(String url, Priority priority) {
        this(Method.GET, url, false, priority);
    }

    /**
     * Equivalent to:
     * <pre>
     *  HttpCallTask(HttpCallTask.Method.GET, url, false, Priority.DEFAULT);
     * </pre>
     */
    public HttpCallTask(Method method, String url, boolean followRedirects) {
        this(method,url,followRedirects,Priority.DEFAULT);
    }
    
    public HttpCallTask(Method method, String url, boolean followRedirects, Priority priority) {
        this.method          = method;
        this.url             = url;
        this.followRedirects = followRedirects;
        this.priority        = priority;
        this.created         = new Date();

        this.queryParams     = new LinkedList<KeyValuePair<String,String>>();
        this.requestHeaders  = new LinkedList<Header>();
    }

    public synchronized void addRequestHeader(Header header) {
        requestHeaders.add(header);
    }

    public synchronized void addQueryParam(String name, String value) {
        addQueryParam(new KeyValuePair<String, String>(name, value));
    }

    public synchronized void addQueryParam(KeyValuePair<String,String> param) {
        queryParams.add(param);
    }

    public synchronized void addQueryParams(Map<String, String> params) {
        for (Map.Entry<String,String> entry : params.entrySet())
            addQueryParam(new KeyValuePair<String, String>(entry.getKey(), entry.getValue()));
    }

    public synchronized void addQueryParams(List<KeyValuePair<String,String>> params) {
        queryParams.addAll(params);
    }

    public synchronized List<KeyValuePair<String,String>> getQueryParams() {
        return Collections.unmodifiableList(queryParams);
    }

    public synchronized List<Header> getRequestHeaders() {
        return Collections.unmodifiableList(requestHeaders);
    }

    /**
     * This implementation of <tt>compareTo</tt> compares <tt>HttpCallTask</tt>
     * instances first on priority of the task, then using the creation time of
     * so that tasks of equal priority will run in FIFO order.
     *
     * This method does not need to be synchronized because <tt>priority</tt>
     * is final.
     */
    public int compareTo(HttpCallTask other) {

        if (other == null)
            return 1;

        int c = this.priority.compareTo(other.priority);

        if (c != 0)
            return c;

        return this.created.compareTo(other.created);
    }
}
