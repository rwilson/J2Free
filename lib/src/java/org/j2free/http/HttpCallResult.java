/*
 * HttpCallResult.java
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

import net.jcip.annotations.Immutable;

import java.io.IOException;

import java.util.Collection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

import org.apache.commons.httpclient.StatusLine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Ryan Wilson
 */
@Immutable
public final class HttpCallResult {

    protected final HttpMethod method;
    protected final String response;
    protected final byte[] bytes;

    protected final StatusLine status;

    protected final HashMap<String,Header> requestHeaders;
    protected final HashMap<String,Header> responseHeaders;

    public HttpCallResult(HttpMethod method) throws IOException {
        
        this.method     = method;
        this.response   = method.getResponseBodyAsString();
        this.bytes      = method.getResponseBody();

        requestHeaders  = new HashMap();
        responseHeaders = new HashMap();

        Header[] headers = method.getRequestHeaders();
        for (Header header : headers) {
            requestHeaders.put(header.getName(), header);
        }

        headers = method.getResponseHeaders();
        for (Header header : headers) {
            responseHeaders.put(header.getName(), header);
        }

        status = method.getStatusLine();
    }

    public int getStatusCode() {
        return status.getStatusCode();
    }

    public String getResponse() {
        return response;
    }

    public byte[] getResponseBytes() {
        return bytes;
    }

    public Header getRequestHeader(String header) {
        return requestHeaders.get(header);
    }

    public Header getResponseHeader(String header) {
        return responseHeaders.get(header);
    }

    public Collection<Header> getRequestHeaders() {
        return Collections.unmodifiableCollection(requestHeaders.values());
    }

    public Collection<Header> getResponseHeaders() {
        return Collections.unmodifiableCollection(responseHeaders.values());
    }

    public Map<String, Header> getRequestHeaderMap() {
        return Collections.unmodifiableMap(requestHeaders);
    }

    public Map<String, Header> getResponseHeaderMap() {
        return Collections.unmodifiableMap(responseHeaders);
    }

    public StatusLine getStatusLine() {
        return status;
    }

    public JSONObject getResponseAsJSONObject() throws JSONException {
        return new JSONObject(response);
    }

    public JSONArray getResponseAsJSONArray() throws JSONException {
        return new JSONArray(response);
    }
}
