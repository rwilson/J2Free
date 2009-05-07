/*
 * HttpCallTask.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import java.util.Date;
import net.jcip.annotations.Immutable;

/**
 * Immutable representation of a HTTP call that can be sorted
 * by priority.
 *
 * @author ryan
 */
@Immutable
public class HttpCallTask implements Comparable<HttpCallTask> {

    public enum Priority {
        LOW,
        DEFAULT,
        HIGH,
        YESTERDAY;
    }

    public final String url;
    public final boolean followRedirects;
    public final Priority priority;
    public final Date created;

    /**
     * Returns a new <tt>HttpCallTask</tt> to the provided url
     * using <tt>HttpCallTask.Priority.DEFAULT</tt> that will
     * not follow redirects.
     */
    public HttpCallTask(String url) {
        this(url,false,Priority.DEFAULT);
    }

    public HttpCallTask(String url, boolean followRedirects) {
        this(url,followRedirects,Priority.DEFAULT);
    }
    
    public HttpCallTask(String url, boolean followRedirects, Priority priority) {
        this.url = url;
        this.followRedirects = followRedirects;
        this.priority = priority;
        this.created = new Date();
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
