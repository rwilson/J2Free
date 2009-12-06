/**
 * HttpCallService.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.j2free.http;

import java.util.List;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Ryan Wilson
 */
public interface HttpCallService {

    public boolean isEnabled();

    /**
     * @return The status of this service
     */
    HttpServiceReport reportStatus();

    /**
     * Attempts to shutdown the service in an ordely fashion, allowing all
     * queued tasks to finish executing but not accepting any new tasks.
     *
     * @param timeout how long to wait
     * @param unit
     * @return true if the service is shutdown as a result of this call, otherwise false
     * @throws InterruptedException if the current thread is interrupted while waiting
     * for the executor to shutdown.
     */
    boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Shuts down the service immediately, terminating any running tasks
     * @return a list of tasks running or waiting to be run
     */
    List<Runnable> shutdownNow();

    public Future<HttpCallResult> submit(final HttpCallTask task);

}
