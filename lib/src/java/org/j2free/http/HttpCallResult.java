/*
 * HttpCallResult.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import net.jcip.annotations.Immutable;

import java.io.IOException;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

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
public class HttpCallResult {

    protected final HttpMethod method;
    protected final String response;
    protected final byte[] bytes;

    protected final StatusLine status;

    protected final ConcurrentHashMap<String,Header> requestHeaders;
    protected final ConcurrentHashMap<String,Header> responseHeaders;

    public HttpCallResult(HttpMethod method) throws IOException {
        this.method     = method;
        this.response   = method.getResponseBodyAsString();
        this.bytes      = method.getResponseBody();

        requestHeaders  = new ConcurrentHashMap<String,Header>();
        responseHeaders = new ConcurrentHashMap<String, Header>();

        Header[] headers = method.getRequestHeaders();
        for (Header header : headers)
            requestHeaders.putIfAbsent(header.getName(), header);

        headers = method.getResponseHeaders();
        for (Header header : headers)
            responseHeaders.putIfAbsent(header.getName(), header);

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
        return requestHeaders.values();
    }

    public Collection<Header> getResponseHeaders() {
        return responseHeaders.values();
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
