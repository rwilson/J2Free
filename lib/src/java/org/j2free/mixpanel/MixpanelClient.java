/*
 * MixpanelClient.java
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
package org.j2free.mixpanel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
 * Provides functionality for tracking mixpanel events.
 * 
 * @author Ryan Wilson
 */
@ThreadSafe
public final class MixpanelClient
{
    public static final String BASE_URL = "http://api.mixpanel.com/track/";

    /**
     * Stores the distinctId => map of properties to be included with all events
     * tracked using the distinctId.
     */
    private static final ConcurrentMap<String, ConcurrentHashMap<String, String>> userPropertiesMap
            = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();

    private final String token;

    /**
     * In test-mode, mixpanel routes requests to a high priority rate limited queue to make testing
     * easier when mixpanel is back logged with data processing.
     */
    private final AtomicBoolean test;

    public MixpanelClient(String token)
    {
        if (StringUtils.isBlank(token))
            throw new IllegalArgumentException("Invalid Mixpanel API token");
        
        this.token = token;
        this.test = new AtomicBoolean(false);
    }

    /**
     * Enables or disables debug mode (which will add a property to the API
     * call to tell mixpanel not to store the data)
     * 
     * @param debug
     */
    public void setTest(boolean debug) {
        this.test.set(debug);
    }

    /**
     * Removes any registered properties for the specified distinctId.
     * 
     * @param distinctId
     * @return true if properties were removed, otherwise false.
     */
    public boolean clearRegisteredProperties(String distinctId)
    {
        return userPropertiesMap.remove(distinctId) != null;
    }

    /**
     * Registers a single property to be included with all events tracked using the same distinctId.
     * @param distinctId
     * @param key
     * @param value
     */
    public void registerProperty(String distinctId, String key, String value)
    {
        Map<String, String> propMap = new HashMap();
        propMap.put(key, value);
        registerProperties(distinctId, propMap);
    }

    /**
     * Registers a set of properties to be included with all events tracked using the same distinctId.
     * 
     * @param distinctId
     * @param customProps
     */
    public void registerProperties(String distinctId, KeyValuePair<String, ? extends Object>... customProps)
    {
        Map<String, String> propMap = new HashMap();
        for (KeyValuePair<String, ? extends Object> pair : customProps)
            propMap.put(pair.key, pair.value.toString());
        registerProperties(distinctId, propMap);
    }

    /**
     * Registers a set of properties to be included with all events tracked using the same distinctId.
     *
     * @param distinctId
     * @param customProps
     */
    public void registerProperties(String distinctId, Map<String, String> allProps)
    {
        // Non-blocking algorithm
        ConcurrentHashMap<String, String> userMap = userPropertiesMap.get(distinctId);

        // If there wasn't a map when we checked, we need to create one, try to put it, and make sure
        // that the eventual map we add properties to is the one stored.
        if (userMap == null)
        {
            // create a temp new map
            userMap = new ConcurrentHashMap<String, String>();

            // try to put this user map into the static map
            ConcurrentHashMap<String, String> storedMap = userPropertiesMap.putIfAbsent(distinctId, userMap);

            // If another thread put a user map in the static map before we could, then putIfAbsent will
            // return that map, in which case we should use it.  Otherwise, userMap is already set to the
            // stored map.
            if (storedMap != null)
                userMap = storedMap;
        }

        // Okay, now userMap is guaranteed to be the stored map
        userMap.putAll(allProps);
    }

    /**
     * Makes an API call to mixpanel to track a event including the specified properties
     * and any registered properties for the specified distinctId.  The properties specified
     * in eventProps will override a property of the same name that was previously registered,
     * but for this method call only.
     * 
     * @param event The name of the event
     * @param distinctId A unique ID for the user
     * @param ip The IP of the user
     * @param params Custom properties to send to mixpanel
     * @return A Future containing the result of the API call
     */
    public Future<HttpCallResult> track(String event, String distinctId, String ip, KeyValuePair<String, ? extends Object>... eventProps)
    {
        Map<String, String> allProps = new HashMap();

        // Add the registered properties first
        Map<String, String> userProps = userPropertiesMap.get(distinctId);
        if (userProps != null)
            allProps.putAll(userProps);

        // add the distint ID for the user, if it was specified
        if (!StringUtils.isBlank(distinctId))
            allProps.put("distinct_id", distinctId);

        // add the IP of the user, if it was specified
        if (!StringUtils.isBlank(ip))
            allProps.put("ip", ip);

        for (KeyValuePair<String, ? extends Object> pair : eventProps)
            allProps.put(pair.key, pair.value.toString());

        return track(event, allProps);
    }

    /**
     * Makes an API call to mixpanel to track a event.
     * 
     * @param event The name of the event
     * @param props All the properties to be sent to mixpanel
     * @return A Future containing the result of the API call
     */
    private Future<HttpCallResult> track(String event, Map<String, String> allProps)
    {
        try
        {
            allProps.put("token", token);     // make sure to add the token!
            allProps.put("time", String.valueOf(System.currentTimeMillis()));
            
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
        }
        catch (JSONException e) {
            throw new IllegalArgumentException("Error creating JSON for API call", e);
        }
    }

    /**
     * Makes an API call to mixpanel to track a funnel event
     * @deprecated Create funnels on mixpanel.com from normal events.
     * 
     * @param funnel The name of the funnel
     * @param step The step in the funnel
     * @param goal The goal of the funnel
     * @param distinctId A unique ID for the user
     * @param ip The IP of the user
     * @param customProps Custom properties to send to mixpanel
     * @return A Future containing the result of the API call
     */
    @Deprecated
    public Future<HttpCallResult> trackFunnel(String funnel, int step, String goal, String distinctId, String ip, KeyValuePair<String, ? extends Object>... customProps)
    {
        HashMap<String, String> props = new HashMap();

        for (KeyValuePair<String, ? extends Object> pair : customProps)
            props.put(pair.key, pair.value.toString());

        return trackFunnel(funnel, step, goal, props);
    }

    /**
     * Makes an API call to mixpanel to track a funnel event.
     * @deprecated Create funnels on mixpanel.com from normal events.
     *
     * @param funnel The name of the funnel
     * @param step The step in the funnel
     * @param goal The goal of the funnel
     * @param props All the properties to be sent to mixpanel
     * @return A Future containing the result of the API call
     */
    @Deprecated
    public Future<HttpCallResult> trackFunnel(String funnel, int step, String goal, HashMap<String, String> allProps)
    {
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
    private Future<HttpCallResult> track(String json)
    {
        HttpCallTask task = new HttpCallTask(Method.POST, BASE_URL);
        task.addQueryParam("ip", "0"); // so mixpanel WON'T use the referrer IP as the user IP

        if (test.get())
            task.addQueryParam("test", "1");

        task.addQueryParam("data", new String(Base64.encodeBase64(json.getBytes())));

        return SimpleHttpService.submit(task);
    }
}
