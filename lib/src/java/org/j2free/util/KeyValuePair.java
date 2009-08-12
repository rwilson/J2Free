/*
 * KeyValuePair.java
 *
 * Created on November 19, 2007, 1:10 PM
 *
 */

package org.j2free.util;

import net.jcip.annotations.Immutable;

/**
 *
 * @author Ryan Wilson
 */
@Immutable
public class KeyValuePair<A,B> {
    
    public final A key;
    public final B value;
    
    public KeyValuePair(A key, B value) {
        this.key   = key;
        this.value = value;
    }

}
