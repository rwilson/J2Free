/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MailChimpResult.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.mailchimp;

import net.jcip.annotations.Immutable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ryan
 */
@Immutable
public class MailChimpResult {

    private final String response;
    
    public MailChimpResult(String response) {
        this.response = response;
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
