/*
 * HttpCallFuture.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import net.jcip.annotations.ThreadSafe;


/**
 *  HttpCallFuture is thread safe by a combination of effective immutability and 
 *  monitor synchronization when accessing the only mutable field <code>result</code>.
 *
 * @author ryan
 */
@ThreadSafe
public class HttpCallFuture implements Comparable<HttpCallFuture> {

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

    public HttpCallFuture(String url) {
        this(url,Priority.DEFAULT);
    }

    public HttpCallFuture(String url, Priority priority) {
        this.url       = url;
        this.priority  = priority;
        this.created   = System.currentTimeMillis();

        this.result    = null;
    }

    public String getUrl() {
        return url;
    }

    public synchronized HttpCallResult getResult() throws InterruptedException {
        while (result == null)
            wait();
        
        return result;
    }

    public synchronized void setResult(HttpCallResult result) {
        this.result = result;
        notifyAll();
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
