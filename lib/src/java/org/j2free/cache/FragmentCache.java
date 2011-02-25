/*
 * FragmentCache.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
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
 * @author Ryan Wilson
 */
@ThreadSafe
public interface FragmentCache<T extends Fragment> {

    public static class Properties {

        private static final String PREFIX = "fragment.cache.";

        // FragmentCache Config
        public static final String ENABLED          = PREFIX + "enabled";
        public static final String REQUEST_TIMEOUT  = PREFIX + "request-timeout"; // In Seconds
        public static final String WARNING_DURATION = PREFIX + "warning-duration"; // In Seconds
        public static final String ENGINE_NAMES     = PREFIX + "names";
        public static final String DEFAULT_ENGINE   = PREFIX + "default";
        public static final String ENGINE_PREFIX    = PREFIX + "strategy.";

        // FragmentCache implementation config templates
        public static final String ENGINE_CLASS_TEMPLATE = ENGINE_PREFIX + "%s.class";
    }

    /**
     * Performs any resource cleanup.  This will be called when the application is
     * undeployed.
     */
    public void destroy();
    
    /**
     * Tests if the specified key is in this cache.
     * 
     * @param key possible key
     * @return true if and only if the specified key is in this cache, otherwise false.
     */
    public boolean contains(String key);

    /**
     * Gets the {@link Fragment} stored under the argument key if one exists.
     *
     * @param key The key of a {@link Fragment} to get
     * @param condition An optional condition upon creation of the {@link Fragment};
     *        if the condition supplied to tryAcquireLock does not match this
     *        condition, then the cache considers itself in need of update.
     * @param timeout The timeout for this cached {@link Fragment}
     * 
     * @return The {@link Fragment} stored via the argument key or a new instance if
     *         one did not exist.
     */
    public T getOrCreate(String key, String condition, long timeout);

    /**
     * @return statistics about the current state of the cache, or historical data
     *         if the implementation supports that.
     */
    public FragmentCacheStatistics getStatistics();

    /**
     * @return The number of fragments currently in the cache.
     */
    public int size();

}
