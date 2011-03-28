/**
 * HttpServiceReport.java
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
