/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MailChimpParams.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.mailchimp;

import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 *
 * @author ryan
 */
@Immutable
public class MailChimpParams {

    private final String listName;
    private final Map<String,Object> params;

    public MailChimpParams(String listName, Map<String,Object> params) {
        this.listName = listName;
        this.params   = params;
    }

    public String getListName() {
        return listName;
    }

    public Object get(String paramName) {
        return params.get(paramName);
    }

}
