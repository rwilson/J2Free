/*
 * HttpQueryParam.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.http;

import net.jcip.annotations.Immutable;

/**
 * Thread safe because it is immutable.
 *
 * @author Ryan Wilson
 */
@Immutable
public class HttpQueryParam {

    public final String name;
    public final String value;

    public HttpQueryParam(String name, String value) {
        this.name  = name;
        this.value = value;
    }
    
}
