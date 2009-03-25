package org.j2free.etc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ryan
 *
 * Basically a thread-safe wrapper around a HashMap
 * 
 */
public class MemoryCache {

    private static Map<String,Object> cache = 
            Collections.synchronizedMap(new HashMap<String,Object>(50));

    public static boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    public static Object put(String key, Object value) {
        return cache.put(key, value);
    }

    public static Object remove(String key) {
        return cache.remove(key);
    }

    public static Object get(String key) {
        return cache.get(key);
    }

    public static <T> T get(String key, Class<T> returnType) {
        return (T)cache.get(key);
    }

    public static <T> List<T> getList(String key, Class<T> returnType) {
        return (List<T>)cache.get(key);
    }

}
