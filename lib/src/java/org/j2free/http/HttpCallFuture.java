/*
 * HttpCallFuture.java
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

import java.util.concurrent.FutureTask;

import net.jcip.annotations.ThreadSafe;
import org.apache.commons.httpclient.HttpClient;

/**
 * Extends FutureTask so that instances of HttpCallTask can be ordered by
 * priority in a PriorityBlockingQueue in the QueuedHttpCallService.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
final class HttpCallFuture extends FutureTask<HttpCallResult> implements Comparable<HttpCallFuture> {

    private final HttpCallTask task;

    protected HttpCallFuture(HttpCallTask task, HttpClient httpClient) {
        super(new HttpCallable(task, httpClient));
        this.task = task;
    }

    /**
     * This implementation of comparable compares <tt>HttpCallFuture</tt> instances
     * calling <tt>compareTo</tt> on the underlying <tt>HttpCallTask</tt> objects.
     */
    public int compareTo(HttpCallFuture other) {
        return task.compareTo(other.task);
    }
}
