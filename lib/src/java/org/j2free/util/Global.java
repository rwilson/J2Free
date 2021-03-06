/*
 * Global.java
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
package org.j2free.util;

import java.util.concurrent.ConcurrentHashMap;
import net.jcip.annotations.ThreadSafe;

/**
 * Basically, <tt>Global</tt> is just a static {@link ConcurrentHashMap}
 * for storing and getting values by key. Ergo, javadocs copied from
 * {@link ConcurrentHashMap} impl.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class Global
{
    private static final ConcurrentHashMap<String,Object> map =
            new ConcurrentHashMap<String,Object>();

    /**
     * @param key a key used to previously store an object
     * @return an object previously stored with the specified key, or
     *         null if there wasn't one
     */
    public static Object get(String key)
    {
        return map.get(key);
    }

    /**
     * Maps the specified <tt>key</tt> to the specified
     * <tt>value</tt> in this table. Neither the key nor the
     * value can be <tt>null</tt>.
     *
     * <p> The value can be retrieved by calling the <tt>get</tt> method
     * with a key that is equal to the original key.
     *
     * @param key the table key.
     * @param value the value.
     * @return the previous value of the specified key in this table,
     * or <tt>null</tt> if it did not have one.
     * @throws IllegalArgumentException if the key or value is
     * <tt>null</tt>.
     */
    public static Object put(String key, Object value)
    {
        if (key == null)
            throw new IllegalArgumentException("Cannot put a null key! [value=" + value + "]");
        else if(value == null)
            throw new IllegalArgumentException("Cannot put a null value! [key=" + key + "]");
        else
            return map.put(key, value);
    }

    /**
     * If the specified key is not already associated
     * with a value, associate it with the given value.
     * This is equivalent to
     * <pre>
     *  if (!map.containsKey(key))
     *      return map.put(key, value);
     *  else
     *      return map.get(key);
     * </pre>
     * Except that the action is performed atomically.
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     * if there was no mapping for key.
     * @throws IllegalArgumentException if the specified key or value is
     * <tt>null</tt>.
     */
    public static Object putIfAbsent(String key, Object value)
    {
        if (key == null)
            throw new IllegalArgumentException("Cannot putIfAbsent a null key! [value=" + value + "]");
        else if(value == null)
            throw new IllegalArgumentException("Cannot putIfAbsent a null value! [key=" + key + "]");
        else
            return map.putIfAbsent(key, value);
    }

    /**
     * Removes all mappings
     */
    public static void clear()
    {
        map.clear();
    }
    
    /**
     * Removes the key (and its corresponding value) from this
     * table. This method does nothing if the key is not in the table.
     *
     * @param key the key that needs to be removed.
     * @return the value to which the key had been mapped in this table,
     * or <tt>null</tt> if the key did not have a mapping.
     * @throws IllegalArgumentException if the key is
     * <tt>null</tt>.
     */
    public static Object remove(String key)
    {
        if (key == null)
            throw new IllegalArgumentException("Cannot remove a null key!");
        else
            return map.remove(key);
    }

    /**
     * Remove entry for key only if currently mapped to given value.
     * Acts as
     * <pre>
     *  if (map.get(key).equals(value)) {
     *      map.remove(key);
     *      return true;
     *  } else return false;
     * </pre>
     * except that the action is performed atomically.
     * 
     * @param key key with which the specified value is associated.
     * @param value value associated with the specified key.
     * @return true if the value was removed
     * @throws IllegalArgumentException if the specified key is <tt>null</tt>.
     */
    public static boolean remove(String key, Object value)
    {
        if (key == null)
            throw new IllegalArgumentException("Cannot remove a null key! [value=" + value + "]");
        else
            return map.remove(key, value);
    }

    /**
     * Replace entry for key only if currently mapped to some value.
     * Acts as
     * <pre>
     *  if ((map.containsKey(key)) {
     *      return map.put(key, value);
     *  } else return null;
     * </pre>
     * except that the action is performed atomically.
     * @param key key with which the specified value is associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     * if there was no mapping for key.
     * @throws IllegalArgumentException if the specified key or value is
     * <tt>null</tt>.
     */
    public static Object replace(String key, Object value)
    {
        if (key == null)
            throw new IllegalArgumentException("Cannot replace a null key! [value=" + value + "]");
        else if(value == null)
            throw new IllegalArgumentException("Cannot replace a null value! [key=" + key + "]");
        else
            return map.replace(key, value);
    }

    /**
     * Replace entry for key only if currently mapped to given value.
     * Acts as
     * <pre>
     *  if (map.get(key).equals(oldValue)) {
     *      map.put(key, newValue);
     *      return true;
     *  } else return false;
     * </pre>
     * except that the action is performed atomically.
     *
     * @param key key with which the specified value is associated.
     * @param oldValue value expected to be associated with the specified key.
     * @param newValue value to be associated with the specified key.
     * @return true if the value was replaced
     * @throws IllegalArgumentException if the specified key or values are <tt>null</tt>.
     */
    public static boolean replace(String key, Object oldValue, Object newValue)
    {
        if (key == null)
            throw new IllegalArgumentException("Cannot replace a null key! [oldValue=" + oldValue + ", newValue=" + newValue + "]");
        else if(oldValue == null)
            throw new IllegalArgumentException("Cannot replace a null key! [key=" + key + ", newValue=" + newValue + "]");
        else if(newValue == null)
            throw new IllegalArgumentException("Cannot replace a null key! [key=" + key + ", oldValue=" + oldValue + "]");
        else
            return map.replace(key, oldValue, newValue);
    }
}
