package org.j2free.http;

import net.jcip.annotations.Immutable;

/**
 * Class for holding the reporting the current state of
 * the QueueHttpCallService
 *
 * @author Ryan Wilson
 */
@Immutable
public class HttpServiceReport {

    private final int currentPoolSize;
    private final int largestPoolSize;
    private final int maxPoolSize;
    private final int activeThreadCount;
    
    private final long totalTaskCount;
    private final long completedTaskCount;

    public HttpServiceReport(int curPS, int largestPS, int maxPS, int activeTC, long totalTC, long completedTC) {
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
