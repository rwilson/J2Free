/*
 * HttpCallTask.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

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

    private final String url;
    private final Priority priority;
    private final long created;

    public HttpCallTask(String url) {
        this(url,Priority.DEFAULT);
    }

    public HttpCallTask(String url, Priority priority) {
        this.url       = url;
        this.priority  = priority;
        this.created   = System.currentTimeMillis();
    }

    public String getUrl() {
        return url;
    }

    /**
     *
     * @param other The other HttpCallFuture
     * @return 1 if this future should run first, -1 if the other future should run first, 0 if they are equal
     */
    public int compareTo(HttpCallTask other) {
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
