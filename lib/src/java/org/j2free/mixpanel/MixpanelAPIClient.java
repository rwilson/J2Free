/*
 * MixpanelAPIClient.java
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
package org.j2free.mixpanel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import org.j2free.http.HttpCallFuture;
import org.j2free.http.HttpCallTask;
import org.j2free.http.HttpCallTask.Method;
import org.j2free.http.QueuedHttpCallService;

import org.j2free.util.KeyValuePair;

import org.json.JSONException;
import org.json.JSONStringer;

/**
 * Provides functionality for tracking mixpanel events with both
 * a static implementation or a instance implementation.
 * The static implementation uses an internal instance corresponding
 * to the last call to init(token).  For apps where only one token
 * is used, the static implementation is more convenient.  But, in
 * apps using multiple tokens, it is recommended to construct an
 * instance per token, rather than continually call init(token) to
 * swap the token currently used by the static methods.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public final class MixpanelAPIClient {

    public static final String BASE_URL = "http://api.mixpanel.com/track/";

    /*****************************************************
     * Static Implementation
     */
    private static final AtomicReference<MixpanelAPIClient> client = new AtomicReference<MixpanelAPIClient>(null);

    public static void init(String token) {
        client.set(new MixpanelAPIClient(token));
    }

    public static void throwNotInitialized() {
        throw new IllegalStateException("MixpanelAPIClient has not been initialized!");
    }

    public static HttpCallFuture track(String event, String distinctId, String ip, KeyValuePair... params) {
        MixpanelAPIClient mpac = client.get();
        if (mpac == null) throwNotInitialized();
        return mpac.trackEvent(event, distinctId, ip, params);
    }

    /**
     * @return The instance of MixpanelAPIClient used by the static <tt>track</tt> method,
     *         or null if <tt>init(token)</tt> has not been called.
     */
    public static MixpanelAPIClient get() {
        return client.get();
    }

    /*****************************************************
     * Instance Implementation
     */
    private final String token;
    private final AtomicBoolean debug;

    public MixpanelAPIClient(String token) {

        if (StringUtils.isBlank(token))
            throw new IllegalArgumentException("Invalid Mixpanel API token");
        
        this.token = token;
        this.debug = new AtomicBoolean(false);
    }

    public void enableDebug() {
        this.debug.set(true);
    }

    public void disableDebug() {
        this.debug.set(false);
    }

    public HttpCallFuture trackEvent(String event, String distinctId, String ip, KeyValuePair<String, ? extends Object>... params) {

        try {

            JSONStringer json = new JSONStringer();
            json.object()
                .key("event").value(event)
                .key("properties")
                .object()
                    .key("token").value(token);             // make sure to add the token!

            // add the distint ID for the user, if it was specified
            if (StringUtils.isBlank(distinctId))
                json.key("distinct_id").value(distinctId);

            // add the IP of the user, if it was specified
            if (!StringUtils.isBlank(ip))
                json.key("ip").value(ip);                   // and the client IP so mixpanel doesn't use our server IP

            for (KeyValuePair<String, ? extends Object> param : params)
                json.key(param.key).value(param.value);

            json.endObject()
                .endObject();

            return trackEvent(json.toString());

        } catch (JSONException e) {
            throw new IllegalArgumentException("Error creating JSON for API call", e);
        }
    }

    private HttpCallFuture trackEvent(String json) {
        return trackEvent(Base64.encodeBase64(json.getBytes()));
    }

    private HttpCallFuture trackEvent(byte[] base64) {

        HttpCallTask task = new HttpCallTask(Method.POST, BASE_URL);
        task.addQueryParam("ip", "0");

        if (debug.get())
            task.addQueryParam("test", "1");

        task.addQueryParam("data", new String(base64));

        return QueuedHttpCallService.submit(task);
    }
}
