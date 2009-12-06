/*
 * SimpleMixpanelClient.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.mixpanel;

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.ThreadSafe;

import org.j2free.http.HttpCallResult;
import org.j2free.util.KeyValuePair;

/**
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class SimpleMixpanelClient {

    private static final AtomicReference<MixpanelClient> client = new AtomicReference<MixpanelClient>(null);

    /**
     * Initializes the SimpleMixpanelClient to use the specified mixpanel account
     * @param token A Mixpanel API token
     */
    public static void init(String token) {
        client.set(new MixpanelClient(token));
    }

    /**
     * Ensures that init("token") has been called, otherwise
     * throws an IllegalStateException
     */
    private static void ensureInitialized() {
        if (client.get() == null) {
            throw new IllegalStateException("MixpanelAPIClient has not been initialized!");
        }
    }

    /**
     * Enables debug mode on the mixpanel client
     */
    public static void setDebug(boolean debug) {
        ensureInitialized();
        client.get().setDebug(debug);
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
    public static Future<HttpCallResult> track(String event, String distinctId, String ip, KeyValuePair<String, ? extends Object>... customProps) {
        ensureInitialized();
        return client.get().track(event, distinctId, ip, customProps);
    }
    
    /**
     * Makes an API call to mixpanel to track a event
     *
     * @param event The name of the event
     * @param props All the properties to be sent to mixpanel
     * @return A Future containing the result of the API call
     */
    public static Future<HttpCallResult> track(String event, HashMap<String, String> allProps) {
        ensureInitialized();
        return client.get().track(event, allProps);
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
    public static Future<HttpCallResult> trackFunnel(String funnel, int step, String goal, String distinctId, String ip, KeyValuePair<String, ? extends Object>... customProps) {
        ensureInitialized();
        return client.get().trackFunnel(funnel, step, goal, distinctId, ip, customProps);
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
    public static Future<HttpCallResult> trackFunnel(String funnel, int step, String goal, HashMap<String, String> allProps) {
        ensureInitialized();
        return client.get().trackFunnel(funnel, step, goal, allProps);
    }
}
