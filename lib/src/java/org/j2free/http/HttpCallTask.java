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
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.j2free.util.Priority;

/**
 * Immutable representation of a HTTP call that can be sorted
 * by priority.
 *
 * @author ryan
 */
@ThreadSafe
public class HttpCallTask implements Comparable<HttpCallTask> {

    public static enum Method {
        GET,
        POST
    };

    public final Method method;

    public final HttpMethodParams params;
    public final List<Header>     requestHeaders;

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

        this.params          = new HttpMethodParams();
        this.requestHeaders         = Collections.synchronizedList(new LinkedList<Header>());
    }

    /**
     * This implementation of <tt>compareTo</tt> compares <tt>HttpCallTask</tt>
     * instances first on priority of the task, then using the creation time of
     * so that tasks of equal priority will run in FIFO order.
     */
    public int compareTo(HttpCallTask other) {

        int c = this.priority.compareTo(other.priority);

        if (c != 0)
            return c;

        return this.created.compareTo(other.created);
    }
}
