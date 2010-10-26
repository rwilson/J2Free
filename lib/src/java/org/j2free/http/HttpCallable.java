/*
 * HttpCallable.java
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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.util.KeyValuePair;

/**
 * Implementation of Callable that takes a HttpCallTask and,
 * when called, returns the result of the task as a HttpCallResult.
 *
 * This class is should never be access outside of <tt>QueuedHttpCallService</tt>
 */
final class HttpCallable implements Comparable<HttpCallable>, Callable<HttpCallResult>
{
    private final Log log = LogFactory.getLog(HttpCallable.class);

    private final HttpCallTask task;
    private final HttpClient client;

    protected HttpCallable(HttpCallTask task, HttpClient client)
    {
        super();
        this.task = task;
        this.client = client;
    }

    public HttpCallResult call() throws IOException
    {
        HttpMethod method;
        
        if (task.method == HttpCallTask.Method.GET)
            method = new GetMethod(task.toString());
        else
        {
            method = new PostMethod(task.url);

            String postBody = task.getExplicitPostBody();
            if (postBody != null)
            {
                
                ( (PostMethod)method ).setRequestEntity( new StringRequestEntity(postBody, "text/xml", null) );
            }
            else
            {
                List<KeyValuePair<String, String>> params = task.getQueryParams();
                NameValuePair[] data = new NameValuePair[params.size()];

                int i = 0;
                for (KeyValuePair<String, String> param : params)
                {
                    data[i] = new NameValuePair(param.key, param.value);
                    i++;
                }

                ( (PostMethod)method ).setRequestBody(data);
            }
        }

        for (Header header : task.getRequestHeaders())
        {
            method.setRequestHeader(header);
        }

        method.setFollowRedirects(task.followRedirects);
        
        try
        {
            if (log.isDebugEnabled())
                log.debug("Making HTTP call [url=" + task.toString() + "]");

            client.executeMethod(method);

            if (log.isDebugEnabled())
                log.debug("Call returned [status=" + method.getStatusCode() + "]");

            return new HttpCallResult(method);
        } 
        finally
        {
            // ALWAYS release the connection!!!
            method.releaseConnection();
        }
    }

    /**
     * Delegates compareTo to the underlying HttpCallTask
     * @param other
     * @return
     */
    public int compareTo(HttpCallable other)
    {
        return this.task.compareTo(other.task);
    }
}
