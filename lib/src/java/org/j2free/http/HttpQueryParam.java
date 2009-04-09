/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * HttpQueryParam.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import net.jcip.annotations.Immutable;

/**
 *
 * @author ryan
 */
@Immutable
public class HttpQueryParam {

    private final String name;
    private final String value;

    public HttpQueryParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

}
