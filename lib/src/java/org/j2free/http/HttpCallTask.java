/*
 * HttpCallTask.java
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.httpclient.Header;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.j2free.security.SecurityUtils;
import org.j2free.util.KeyValuePair;
import org.j2free.util.Priority;

/**
 * Thread-safe representation of a HTTP call that can be sorted
 * by priority.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class HttpCallTask implements Comparable<HttpCallTask>
{
    private final Log log = LogFactory.getLog(HttpCallTask.class);

    public static enum Method { GET, POST };

    protected final Method method;

    private final List<KeyValuePair<String,String>> queryParams;
    private final List<Header> requestHeaders;

    private String postBody;

    protected final String url;
    protected final boolean followRedirects;
    protected final Priority priority;
    protected final long created;

    /**
     * Equivalent to:
     * <pre>
     *  HttpCallTask(HttpCallTask.Method.GET, url);
     * </pre>
     */
    public HttpCallTask(String url)
    {
        this(Method.GET, url);
    }

    /**
     * Equivalent to:
     * <pre>
     *  HttpCallTask(HttpCallTask.Method.GET, url, false);
     * </pre>
     */
    public HttpCallTask(Method method, String url)
    {
        this(method, url, false);
    }

    /**
     * Equivalent to:
     * <pre>
     *  HttpCallTask(HttpCallTask.Method.GET, url, false, Priority.DEFAULT);
     * </pre>
     */
    public HttpCallTask(String url, Priority priority)
    {
        this(Method.GET, url, false, priority);
    }

    /**
     * Equivalent to:
     * <pre>
     *  HttpCallTask(HttpCallTask.Method.GET, url, false, Priority.DEFAULT);
     * </pre>
     */
    public HttpCallTask(Method method, String url, boolean followRedirects)
    {
        this(method, url, followRedirects, Priority.DEFAULT);
    }
    
    public HttpCallTask(Method method, String url, boolean followRedirects, Priority priority)
    {
        this.method          = method;
        this.url             = url;
        this.followRedirects = followRedirects;
        this.priority        = priority;
        this.created         = System.currentTimeMillis();

        this.queryParams     = new LinkedList<KeyValuePair<String,String>>();
        this.requestHeaders  = new LinkedList<Header>();

        this.postBody        = null;
    }

    public synchronized void addRequestHeader(Header header)
    {
        requestHeaders.add(header);
    }

    public synchronized void addQueryParam(String name, String value)
    {
        addQueryParam(new KeyValuePair<String, String>(name, value));
    }

    public synchronized void addQueryParam(KeyValuePair<String,String> param)
    {
        queryParams.add(param);
    }

    public synchronized void addQueryParams(Map<String, String> params)
    {
        for (Map.Entry<String,String> entry : params.entrySet())
            addQueryParam(new KeyValuePair<String, String>(entry.getKey(), entry.getValue()));
    }

    public synchronized void addQueryParams(List<KeyValuePair<String,String>> params)
    {
        queryParams.addAll(params);
    }

    /**
     * "Signs" the request with a SHA1 hash of the parameters concatenated
     * with the specified secret key.
     *
     * NOTE: This method must be called LAST, since it does not check to see
     * if this instance was already signed, and will not automatically resign
     * the request if new parameters are added later.
     * 
     * @param secretKey
     */
    public synchronized void signRequest(String secretKey)
    {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (KeyValuePair param : queryParams)
        {
            if (first)
                first = false;
            else
                sb.append("&");
            sb.append(param.key + "=" + param.value);
        }

        sb.append(secretKey);

        log.debug("Sig string: " + sb.toString());

        addQueryParam("sig", SecurityUtils.SHA1(sb.toString()));
    }

    protected synchronized List<KeyValuePair<String,String>> getQueryParams()
    {
        return Collections.unmodifiableList(queryParams);
    }

    protected synchronized List<Header> getRequestHeaders()
    {
        return Collections.unmodifiableList(requestHeaders);
    }

    public synchronized void setExplicitPostBody(String body)
    {
        this.postBody = body;
    }

    public synchronized String getExplicitPostBody()
    {
        return postBody;
    }

    /**
     * This implementation of <tt>compareTo</tt> compares <tt>HttpCallTask</tt>
     * instances first on priority of the task, then using the creation time of
     * so that tasks of equal priority will run in FIFO order.
     *
     * This method does not need to be synchronized because <tt>priority</tt>
     * is final.
     */
    public int compareTo(HttpCallTask other)
    {
        if (other == null)
            return 1;

        int c = this.priority.compareTo(other.priority);

        if (c != 0)
            return c;

        return Float.valueOf(Math.signum(other.created - this.created)).intValue();
    }

    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder(url);

        // Only append the params for GET requests
        if (method == Method.GET) {
            if (!queryParams.isEmpty())
            {
                b.append("?");
                boolean first = true;
                for (KeyValuePair param : queryParams)
                {
                    if (first)
                        first = false;
                    else
                        b.append("&");

                    b.append(param.key);
                    b.append("=");
                    b.append(param.value);
                }
            }
        }

        return b.toString();
    }
}
