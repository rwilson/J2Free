/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * Fragment.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.jsp.tags.cache;

/**
 *
 * @author ryan
 */
public class Fragment {

    private String  key;
    private String  condition;

    private long    timeout;

    private boolean disable;

    public Fragment(String key, String condition, long timeout, boolean disable) {
        this.key       = key;
        this.condition = condition;
        this.timeout   = timeout;
        this.disable   = disable;
    }

    public String getKey() {
        return key;
    }

    public String getCondition() {
        return condition;
    }

    public long getTimeout() {
        return timeout;
    }

    public boolean isDisabled() {
        return disable;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

}
