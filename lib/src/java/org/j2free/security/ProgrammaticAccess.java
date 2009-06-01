/*
 * ProgrammaticAccess.java
 *
 * Created on June 19, 2008, 3:19 AM
 */

package org.j2free.security;


import java.util.concurrent.ConcurrentHashMap;
import net.jcip.annotations.ThreadSafe;

import static org.j2free.security.SecurityUtils.*;

/**
 * Basically, a utility class for storing an allowed
 * reference to some Object.  Granting access will
 * return a unique key which can be used to retrieve
 * access to that object later on.  Calling <tt>get</tt>
 * or <tt>expire</tt> with a key will retrieve the object
 * to which access was granted.
 *
 * @author Ryan Wilson
 * @version
 */
@ThreadSafe
public class ProgrammaticAccess {
    
    private static final ConcurrentHashMap<String,Object> allowedUsers = new ConcurrentHashMap<String,Object>();
    
    public static String allow(Object obj) {
        try {
            String hash = MD5("" + obj.hashCode());
            allowedUsers.put(hash,obj);
            return hash;
        } catch (Exception e) { }
        
        return null;
    }
    
    public static Object get(String key) {
        return allowedUsers.get(key);
    }
    
    public static Object expire(String key) {
        return allowedUsers.remove(key);
    }
}
