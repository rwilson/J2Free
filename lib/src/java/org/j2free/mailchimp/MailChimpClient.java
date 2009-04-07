/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MailChimpClient.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.mailchimp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.j2free.util.Pair;
import org.j2free.mailchimp.MailChimpTask.Method;
import org.json.JSONArray;

/**
 *
 * @author ryan
 */
public class MailChimpClient {

    private static final Log log = LogFactory.getLog(MailChimpClient.class);

    private static final String SERVER_URL = "https://api.mailchimp.com/1.2/?output=json&method=";

    private static final String LEFT_BRACKET  = "%5B";
    private static final String RIGHT_BRACKET = "%5D";

    public static final int LIST_MEMBERS_UPPER_LIMIT = 15000;

    /***************************************************************************
     *
     *  static convenience methods
     *
     */

    private static final ConcurrentMap<String,MailChimpClient> instances = new ConcurrentHashMap<String,MailChimpClient>(100,0.8f,100);

    public static void registerInstance(String key, MailChimpClient em) {
        instances.putIfAbsent(key,em);
    }

    public static MailChimpClient getInstance(String key) {
        return instances.get(key);
    }


    /***************************************************************************
     *
     *  Instance Implementation
     *
     */

    private String apiKey;

    private final String version;
    private final String username;
    private final String password;

    private final HashMap<String,String> listMap;

    public static enum SubscriptionStatus {
        subscribed("subscribed"),
        unsubscribed("unsubscribed"),
        cleaned("cleaned");

        private String name;

        SubscriptionStatus(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public MailChimpClient(String username, String password, String version) {

        this.username = username;
        this.password = password;

        this.listMap = new HashMap<String,String>();

    }

    private void process(MailChimpTask task) {
        switch (task.getMethod()) {
            case listSubscribe:
                subscribe(task.getParams());
                break;
            case listUnsubscribe:
                unsubscribe(task.getParams());
                break;
            case listUpdateMember:
                update(task.getParams());
                break;
            default:
                log.warn("MailChimpTasks are not allowed to call " + task.getMethod());
                break;
        }
    }

    private String login() {
        try {

            return call(
                       Method.login,
                       new Pair("username",username),
                       new Pair("password",password)
                   ).replaceAll("\"", "");

        } catch (Exception e) {
            return null;
        }
    }

    /**
     *
     * @return Array of <code>MailChimpList</code> objects representing MailChimp lists
     * @throws java.lang.Exception
     */
    public MailChimpList[] lists() throws Exception {

        String response = call(Method.lists);

        if (isEmpty(response))
            return new MailChimpList[0];

        JSONArray array = new JSONArray(response);

        MailChimpList[] lists = new MailChimpList[array.length()];

        for (int i = 0; i < array.length(); i++) {
            lists[i] = new MailChimpList(array.getJSONObject(i));
        }

        return lists;
    }

    /**
     *
     * @param listName - The name of the list to get
     * @return The <code>MailChimpList</code> if it is found, otherwise null.
     * @throws java.lang.Exception
     */
    public String getListId(String listName) throws Exception {

        if (listMap.containsKey(listName))
            return listMap.get(listName);

        populateListMap();

        return listMap.containsKey(listName) ? listMap.get(listName) : null;
    }

    private void populateListMap() throws Exception {

        MailChimpList[] lists = lists();

        listMap.clear();

        for (MailChimpList list : lists)
            listMap.put(list.getName(), list.getListId());
    }

    /**
     *
     * @param user - the user to subscribe
     * @param listName - the name of the list
     * @return true if the user is subscribed, otherwise false
     */
    public boolean subscribe(MailChimpParams params) {

        try {

            String listName = params.getListName();
            String listId   = getListId(listName);

            if (listId == null) {
                log.debug("Error subscribing user to list[name=" + listName + "], list not found");
                return false;
            }


            List<Pair> pairs = new LinkedList<Pair>();
            pairs.add(new Pair("email_address",(String)params.get("email")));
            pairs.add(new Pair("id",listId));
            pairs.add(new Pair("double_optin",false));

            for (Pair<String,String> mergeVar : (List<Pair>)params.get("mergeVars"))
                pairs.add(new Pair("merge_vars" + LEFT_BRACKET + mergeVar.getFirst() + RIGHT_BRACKET,mergeVar.getSecond()));

            String response = call(Method.listSubscribe,pairs.toArray(new Pair[pairs.size()]));
            
            return parseBoolean(response);

        } catch (Exception e) {
            log.error("Error calling subscribe",e);
            return false;
        }
    }

    /**
     *
     * @param email
     * @param listName
     * @param delete
     * @return
     */
    public boolean unsubscribe(MailChimpParams params) {

        String listName = params.getListName();
        String listId   = null;

        try {

            listId = getListId(listName);

        } catch (Exception e) {
            log.error("Error unsubscribing user from list[name=" + listName + "], error getting list", e);
            return false;
        }

        if (listId == null) {
            log.debug("Error unsubscribing user from list[name=" + listName + "], list not found");
            return false;
        }

        try {
            String response =
                    call(
                        Method.listUnsubscribe,
                        new Pair("email_address",params.get("email")),
                        new Pair("id",listId),
                        new Pair("delete_member",params.get("delete")),
                        new Pair("send_notify",false),
                        new Pair("send_goodbye",false)
                    );

            return parseBoolean(response);

        } catch (Exception e) {
            log.error("Error calling unsubscribe",e);
            return false;
        }
    }

    /**
     *
     * @param oldEmail
     * @param user
     * @param listName
     * @return
     */
    public boolean update(MailChimpParams params) {

        String listName = params.getListName();
        String listId   = null;

        try {

            listId = getListId(listName);

        } catch (Exception e) {
            log.error("Error updating user on list[name=" + listName + "], error getting list", e);
            return false;
        }

        if (listId == null) {
            log.debug("Error updating user on list[name=" + listName + "], list not found");
            return false;
        }

        try {
            String response =
                    call(
                        Method.listUpdateMember,
                        new Pair("email_address",params.get("oldEmail")),
                        new Pair("id",listId),
                        new Pair("merge_vars" + LEFT_BRACKET + "USERNAME" + RIGHT_BRACKET,user.getUsername()),
                        new Pair("merge_vars" + LEFT_BRACKET + "EMAIL" + RIGHT_BRACKET,user.getEmail())
                    );

            return parseBoolean(response);

        } catch (Exception e) {
            log.error("Error calling update",e);
            return false;
        }
    }

    /**
     *
     * @param listName
     * @param status
     * @param start
     * @param limit
     * @return
     */
    public JSONArray listMembers(MailChimpParams params) {

        String listName = params.getListName();
        String listId   = null;

        try {

            listId = getListId(listName);

        } catch (Exception e) {
            log.error("Error listing members from list[name=" + listName + "], error getting list", e);
            return null;
        }

        if (listId == null) {
            log.debug("Error listing members from list[name=" + listName + "], list not found");
            return null;
        }

        try {

            String response =
                    call(
                        Method.listMembers,
                        new Pair("id",listId),
                        new Pair("status",status.toString()),
                        new Pair("start",start),
                        new Pair("limit",limit)
                    );

            if (!isEmpty(response)) {
                return new JSONArray(response);
            } else {
                log.error("Received empty response to listMembers");
                return null;
            }

        } catch (Exception e) {
            log.error("Error calling listMembers",e);
            return null;
        }
    }

    /**
     * @param response
     * @return <code>true</code> if the response contains a valid boolean true, otherwise false
     */
    private static boolean parseBoolean(String response) {
        try {
            return Boolean.parseBoolean(response);
        } catch (Exception e) {
            log.error("Error parsing boolean response: " + response,e);
        }
        return false;
    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().equals("");
    }
}
