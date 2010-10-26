/*
 * MixpanelClient.java
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

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import org.j2free.http.HttpCallResult;
import org.j2free.http.HttpCallTask;
import org.j2free.http.HttpCallTask.Method;
import org.j2free.http.SimpleHttpService;

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
public final class MixpanelClient {

    public final String BASE_URL = "http://api.mixpanel.com/track/";

    private final String token;
    private final AtomicBoolean debug;

    public MixpanelClient(String token) {

        if (StringUtils.isBlank(token))
            throw new IllegalArgumentException("Invalid Mixpanel API token");
        
        this.token = token;
        this.debug = new AtomicBoolean(false);
    }

    /**
     * Enables or disables debug mode (which will add a property to the API
     * call to tell mixpanel not to store the data)
     * 
     * @param debug
     */
    public void setDebug(boolean debug) {
        this.debug.set(debug);
    }

    /**
     * Makes an API call to mixpanel to track a event
     * 
     * @param event The name of the event
     * @param distinctId A unique ID for the user
     * @param ip The IP of the user
     * @param params Custom properties to send to mixpanel
     * @return A Future containing the result of the API call
     */
    public Future<HttpCallResult> track(String event, String distinctId, String ip, KeyValuePair<String, ? extends Object>... customProps) {

        HashMap<String, String> props = new HashMap();

        // add the distint ID for the user, if it was specified
        if (!StringUtils.isBlank(distinctId))
            props.put("distinctId", distinctId);

        // add the IP of the user, if it was specified
        if (!StringUtils.isBlank(ip))
            props.put("ip", ip);

        for (KeyValuePair<String, ? extends Object> pair : customProps) {
            props.put(pair.key, pair.value.toString());
        }

        return track(event, props);
    }

    /**
     * Makes an API call to mixpanel to track a event
     * 
     * @param event The name of the event
     * @param props All the properties to be sent to mixpanel
     * @return A Future containing the result of the API call
     */
    public Future<HttpCallResult> track(String event, HashMap<String, String> allProps) {
        try {

            allProps.put("token", token);     // make sure to add the token!
            
            JSONStringer json = new JSONStringer();
            json.object()
                .key("event").value(event)
                .key("properties")
                .object();

            for (String key : allProps.keySet())
                json.key(key).value(allProps.get(key));

            json.endObject()
                .endObject();

            return track(json.toString());

        } catch (JSONException e) {
            throw new IllegalArgumentException("Error creating JSON for API call", e);
        }
    }

    /**
     * Makes an API call to mixpanel to track a funnel event
     * 
     * @param funnel The name of the funnel
     * @param step The step in the funnel
     * @param goal The goal of the funnel
     * @param distinctId A unique ID for the user
     * @param ip The IP of the user
     * @param customProps Custom properties to send to mixpanel
     * @return A Future containing the result of the API call
     */
    public Future<HttpCallResult> trackFunnel(String funnel, int step, String goal, String distinctId, String ip, KeyValuePair<String, ? extends Object>... customProps) {

        HashMap<String, String> props = new HashMap();
        for (KeyValuePair<String, ? extends Object> pair : customProps) {
            props.put(pair.key, pair.value.toString());
        }

        return trackFunnel(funnel, step, goal, props);
    }

    /**
     * Makes an API call to mixpanel to track a funnel event
     *
     * @param funnel The name of the funnel
     * @param step The step in the funnel
     * @param goal The goal of the funnel
     * @param props All the properties to be sent to mixpanel
     * @return A Future containing the result of the API call
     */
    public Future<HttpCallResult> trackFunnel(String funnel, int step, String goal, HashMap<String, String> allProps) {

        allProps.put("funnel", funnel);
        allProps.put("step", String.format("%d", step));
        allProps.put("goal", goal);
        
        return track("mp_funnel", allProps);
    }

    /**
     * Submits a HttpCallTask to be executed
     *
     * @param base64 A base64 encoded string containing the data to send to mipanel
     * @return A Future containing the result of the API call
     */
    private Future<HttpCallResult> track(String json) {

        HttpCallTask task = new HttpCallTask(Method.POST, BASE_URL);
        task.addQueryParam("ip", "0"); // so mixpanel WON'T use the referrer IP as the user IP

        if (debug.get())
            task.addQueryParam("test", "1");

        task.addQueryParam("data", new String(Base64.encodeBase64(json.getBytes())));

        return SimpleHttpService.submit(task);
    }
}
