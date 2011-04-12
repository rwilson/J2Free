/*
 * ProgrammaticAccess.java
 *
 * Created on June 19, 2008, 3:19 AM
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @deprecated Use memcached instead.
 */
@ThreadSafe
@Deprecated
public class ProgrammaticAccess {
    
    private static final ConcurrentHashMap<String,Object> allowedUsers = new ConcurrentHashMap<String,Object>();
    
    /**
     * 
     * @param obj
     * @return
     */
    public static String allow(Object obj)
    {
        try {
            String hash = MD5("" + obj.hashCode());
            allowedUsers.put(hash,obj);
            return hash;
        } catch (Exception e) { }
        
        return null;
    }
    
    /**
     * 
     * @param key
     * @return
     */
    public static Object get(String key)
    {
        return allowedUsers.get(key);
    }
    
    /**
     * 
     * @param key
     * @return
     */
    public static Object expire(String key)
    {
        return allowedUsers.remove(key);
    }
}
