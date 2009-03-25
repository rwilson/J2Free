/*
 * ProgrammaticAccess.java
 *
 * Created on June 19, 2008, 3:19 AM
 */

package org.j2free.security;

import java.util.HashMap;

import static org.j2free.security.SecurityUtils.*;

/**
 *
 * @author ryan
 * @version
 */
public class ProgrammaticAccess {
    
    private static HashMap<String,Object> allowedUsers;
    
    static {
        allowedUsers = new HashMap<String,Object>();
    }
    
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
