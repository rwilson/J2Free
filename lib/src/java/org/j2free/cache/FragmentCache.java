/*
 * FragmentCache.java
 *
 * Copyright (c) 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.cache;

import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;

/**
 * Interface defining a FragmentCache, so that the FragmentCacheTag doesn't 
 * have to deal with the semantics of how the cache is implemented.  This
 * allows for multiple implementations; e.g. an in-memory cache, or a cache 
 * that uses memcached as a distributed store for Fragments.
 *
 * ALL implementations MUST be thread-safe and provide a constructor taking
 * a {@link org.apache.commons.configuration.Configuration} in order to be
 * configuratble via properties.
 *
 * Alternatively, implementations that do not need properties configuration
 * can provide a no-args constructor and be created via properties config
 * as well.
 *
 * @param <T>
 * @author Ryan Wilson
 */
@ThreadSafe
public interface FragmentCache<T extends Fragment> {

    /**
     * 
     */
    public static class Properties
    {

        private static final String PREFIX = "fragment.cache.";

        // FragmentCache Config
        /**
         *
         */
        public static final String ENABLED          = PREFIX + "enabled";
        /**
         *
         */
        public static final String REQUEST_TIMEOUT  = PREFIX + "request-timeout"; // In Seconds
        /**
         *
         */
        public static final String WARNING_DURATION = PREFIX + "warning-duration"; // In Seconds
        /**
         *
         */
        public static final String ENGINE_NAMES     = PREFIX + "names";
        /**
         *
         */
        public static final String DEFAULT_ENGINE   = PREFIX + "default";
        /**
         *
         */
        public static final String ENGINE_PREFIX    = PREFIX + "strategy.";

        // FragmentCache implementation config templates
        /**
         *
         */
        public static final String ENGINE_CLASS_TEMPLATE = ENGINE_PREFIX + "%s.class";
    }

    /**
     * Performs any resource cleanup
     */
    public void destroy();
    
    /**
     * Removes all {@link Fragment} instances from the <tt>FragmentCache</tt>.
     *
     * @return the number of {@link Fragment} instances the cache held before the clear.
     */
    public int clear();

    /**
     * Tests if the specified key is in this cache.
     * 
     * @param key possible key
     * @return true if and only if the specified key is in this cache, otherwise false.
     */
    public boolean contains(String key);

    /**
     * Creates a new {@link Fragment} of the default type supported by this implementation.
     * 
     * @param condition An optional condition upon creation of the {@link Fragment};
     *        if the condition supplied to tryAcquireLock does not match this
     *        condition, then the cache considers itself in need of update.
     * @param timeout The timeout for this cached {@link Fragment}
     * @return the new {@link Fragment}
     */
    public T createFragment(String condition, long timeout);

    /**
     * Creates a new {@link Fragment} of the default type supported by this implementation.
     *
     * @param content An start value for the content of this {@link Fragment}
     * @param condition An optional condition upon creation of the {@link Fragment};
     *        if the condition supplied to tryAcquireLock does not match this
     *        condition, then the cache considers itself in need of update.
     * @param timeout The timeout for this cached {@link Fragment}
     * @return the new {@link Fragment}
     */
    public T createFragment(T content, String condition, long timeout);

    /**
     * Gets the {@link Fragment} stored under the argument key if one exists.
     *
     * @param key The key of a {@link Fragment} to get
     * @return The {@link Fragment} stored via the argument key otherwise null.
     */
    public T get(String key);

    /**
     * @return statistics about the current state of the cache, or historical data
     *         if the implementation supports that.
     */
    public FragmentCacheStatistics getStatistics();

    /**
     * Removes the {@link Fragment} stored under the argument key if one exists.
     *
     * @param key The key of a {@link Fragment} to evict.
     * @return The {@link Fragment} if it was found, otherwise null.
     */
    public T evict(String key);

    /**
     * Removes the {@link Fragment} stored under the argument key if one exists and it equals
     * the {@link Fragment} referenced by the argument {@link Fragment}.
     *
     * @param key The key of a {@link Fragment} to evict.
     * @param expected The {@link Fragment} expected to be removed.
     *
     * @return true if the argument key was mapped to the argument {@link Fragment}, otherwise false.
     */
    public boolean evict(String key, T expected);

    /**
     * @return An {@link Iterator} for the keys in the cache.
     */
    public Iterator<String> keyIterator();

    /**
     * Associates the specified key to the specified {@link Fragment} in this cache.
     * Neither the key nor the {@link Fragment} can be null. The {@link Fragment} can
     * be retrieved by calling the get method with a key that is equal to the original
     * key.
     * 
     * @param key the key
     * @param fragment the {@link Fragment}
     * @return Previous {@link Fragment} of the specified key in this cache, or null if
     *         it did not have one.
     */
    public T put(String key, T fragment);

    /**
     * Associates the specified {@link Fragment} with the specified key if there is not one already.
     * This is equivalent to
     * <pre>
     *      if (!cache.contains(key)) {
     *          cache.put(key, fragment);
     *      }
     *      return cache.get(key);
     * </pre>
     * except that the action is performed atomically.
     * 
     * @param key key with which the specified {@link Fragment} is to be associated
     * @param fragment {@link Fragment} to be associated with the specified key
     * @return Current {@link Fragment} associated with specified key.  If the cache previously
     *         associated a {@link Fragment} with the specified key, the old {@link Fragment} is
     *         returned, otherwise the argument {@link Fragment} is returned.
     */
    public T putIfAbsent(String key, T fragment);

    /**
     * Replace {@link Fragment} for key only if currently mapped to some value. Acts as
     * <pre>
     *      if (cache.get(key).equals(expected)) {
     *          cache.put(key, replacement);
     *          return replacement;
     *      } else return cache.get(key);
     * </pre>
     * except that the action is performed atomically.
     * 
     * @param key key with which the specified {@link Fragment} is associated
     * @param expected
     * @param replacement
     * @return Current {@link Fragment} associated with the specified key. If the previous
     *         {@link Fragment} equaled the expected, then the replacement {@link Fragment}
     *         is returned.
     */
    public T replace(String key, T expected, T replacement);

    /**
     * @return The number of fragments currently in the cache.
     */
    public int size();

}
