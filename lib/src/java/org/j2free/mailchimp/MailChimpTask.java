/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MailChimpTask.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.mailchimp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.j2free.util.Pair;


/**
 *
 * @author ryan
 */
public class MailChimpTask {

    public static enum Method {
        listSubscribe("listSubscribe"),
        listUnsubscribe("listUnsubscribe"),
        lists("lists"),
        listUpdateMember("listUpdateMember"),
        listMembers("listMembers"),
        login("login");

        private String name;

        Method(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    };
    
    private final Method method;
    private final MailChimpParams params;

    private volatile MailChimpResult result;

    private MailChimpTask(Method method, MailChimpParams params) {
        this.method   = method;
        this.params   = params;
        this.result   = new MailChimpResult();
    }

    public Method getMethod() {
        return method;
    }

    public MailChimpParams getParams() {
        return params;
    }

    public synchronized void setResult(MailChimpResult result) {
        this.result = result;
    }

    public MailChimpResult get() throws InterruptedException, ExecutionException {
        while (result == null)
            wait();

        return result;
    }

    public MailChimpResult get(long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        while (result == null)
            wait(timeout,0);

        return result;
    }

    public boolean isDone() {
        return result != null;
    }

    /*****************************************************************/
    // Static Methods for constructing tasks with the correct parameters

    /**
     *
     * @param listName The list to subscribe a user to
     * @param email The e-mail of the user
     * @param mergeVars A List of Pairs containing any mergeVars desired
     * @return a MailChimpTask to do so
     */
    public static MailChimpTask subscribe(String listName, String email, List<Pair> mergeVars) {

        Map<String,Object> params = new HashMap<String,Object>(1 + mergeVars.size(),1f);
        params.put("email",email);
        params.put("mergeVars",mergeVars);

        MailChimpParams mcParams = new MailChimpParams(listName, params);
        
        return new MailChimpTask(Method.listSubscribe,mcParams);
    }

    public static MailChimpTask unsubscribe(String listName, String email, boolean delete) {

        Map<String,Object> params = new HashMap<String,Object>(2,1f);
        params.put("email",email);
        params.put("delete",delete);

        MailChimpParams mcParams = new MailChimpParams(listName, params);
        
        return new MailChimpTask(Method.listUnsubscribe,mcParams);
        
    }
}
