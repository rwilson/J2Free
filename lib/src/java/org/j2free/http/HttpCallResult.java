/*
 * HttpCallResult.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import net.jcip.annotations.Immutable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ryan
 */
@Immutable
public class HttpCallResult {

    private final int statusCode;
    private final String response;

    public HttpCallResult(int statusCode, String response) {
        this.statusCode = statusCode;
        this.response   = response;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponse() {
        return response;
    }

    public JSONObject getResponseAsJSONObject() throws JSONException {
        return new JSONObject(response);
    }

    public JSONArray getResponseAsJSONArray() throws JSONException {
        return new JSONArray(response);
    }
}
