/*
 * SimpleHttpService.java
 *
 * Copyright (c) 2011 FooBrew, Inc.
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
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.ThreadSafe;

/**
 * Static instance of QueuedHttpCallService.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class SimpleHttpService
{
    // Static convenience IMPL
    public static AtomicReference<HttpCallService> instance = new AtomicReference(null);

    /**
     * Enables this instance.
     *
     * @param maxPoolSize The max number of threads
     * @param threadIdle How long a thread can be idle before terminating it
     * @param connectTimeout How long to wait for a connection
     * @param socketTimeout How long to wait for an operation
     * @throws IllegalStateException if called when the instance is already running
     */
    public static void init(int corePoolSize, int maxPoolSize, long threadIdle, int connectTimeout, int socketTimeout)
    {
        instance.set(new QueuedHttpCallService(corePoolSize, maxPoolSize, threadIdle, connectTimeout, socketTimeout));
    }

    public static boolean isEnabled()
    {
        return instance.get() != null;
    }

    public static void ensureEnabled()
    {
        if (!isEnabled())
            throw new IllegalStateException("SimpleHttpService has not been initialized!");
    }

    /**
     * @return The status of this instance
     */
    public static HttpServiceReport reportStatus()
    {
        ensureEnabled();
        return instance.get().reportStatus();
    }

    /**
     * Attempts to shutdown the instance in an ordely fashion, allowing all
     * queued tasks to finish executing but not accepting any new tasks.
     *
     * @param timeout how long to wait
     * @param unit
     * @return true if the instance is shutdown as a result of this call, otherwise false
     * @throws InterruptedException if the current thread is interrupted while waiting
     * for the executor to shutdown.
     */
    public static boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException
    {
        ensureEnabled();
        HttpCallService service = instance.get();
        boolean success = service.shutdown(timeout, unit);
        if (success)
            instance.compareAndSet(service, null); // so it shows up as not enabled after shutdown
        return success;
    }

    /**
     * Shuts down the instance immediately, terminating any running tasks
     * @return a list of tasks running or waiting to be run
     */
    public static List<Runnable> shutdown()
    {
        ensureEnabled();
        HttpCallService service = instance.get();
        List<Runnable> queue = service.shutdown();
        instance.compareAndSet(service, null); // so it shows up as not enabled after shutdown
        return queue;
    }

    public static Future<HttpCallResult> submit(final HttpCallTask task)
    {
        ensureEnabled();
        return instance.get().submit(task);
    }
}
