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
package org.j2free.cache.impl.memory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.configuration.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.j2free.cache.FragmentCache;
import org.j2free.cache.FragmentCacheStatistics;

/**
 * Implements {@link FragmentCache} using an in-process Fragment storage
 * built on a {@link ConcurrentHashMap}.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public final class MemoryFragmentCache implements FragmentCache<MemoryFragment>
{
    // Config Properties
    private static final String PROP_SIZE           = Properties.ENGINE_PREFIX + "memory.size";
    private static final String PROP_LOAD_FACTOR    = Properties.ENGINE_PREFIX + "memory.load-factor";
    private static final String PROP_CONCURRENCY    = Properties.ENGINE_PREFIX + "memory.concurrency";
    private static final String PROP_CLEAN_INTERVAL = Properties.ENGINE_PREFIX + "memory.cleaner-interval";

    private static final int   DEFAULT_SIZE        = 10000;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int   DEFAULT_CONCURRENCY = 16;

    // The backing ConcurrentMap
    private final ConcurrentMap<String,MemoryFragment> map;

    // The cleaner instance
    private MemoryFragmentCleaner cleaner;

    // A single-threaded executor to run the cleaner task
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // A ScheduledFuture representing the cleaner task
    private volatile ScheduledFuture cleanerFuture = null;

    private final Log log = LogFactory.getLog(getClass());

    public MemoryFragmentCache(Configuration config)
    {
        this(
            config.getInt(PROP_SIZE, DEFAULT_SIZE),
            config.getFloat(PROP_LOAD_FACTOR, DEFAULT_LOAD_FACTOR),
            config.getInt(PROP_CONCURRENCY, DEFAULT_CONCURRENCY)
        );

        scheduleCleaner(
                config.getInt(PROP_CLEAN_INTERVAL),
                TimeUnit.SECONDS,
                false
            );
    }

    public MemoryFragmentCache(int initialSize) {
        this.map = new ConcurrentHashMap<String,MemoryFragment>(initialSize);
        this.cleaner = new MemoryFragmentCleaner(this);
    }

    /**
     * Since this implementation is built on a {@link ConcurrentHashMap}, this constructor
     * provides a way to configure the underlying map.
     * 
     * @param initialSize
     * @param loadFactor
     * @param concurrencyLevel
     */
    public MemoryFragmentCache(int initialSize, float loadFactor, int concurrencyLevel)
    {
        this.map = new ConcurrentHashMap<String,MemoryFragment>(initialSize, loadFactor, concurrencyLevel);
        this.cleaner = new MemoryFragmentCleaner(this);
    }

    /**
     * Performs any resource cleanup.  This will be called when the application is
     * undeployed.
     */
    public void destroy()
    {
        if (cleanerFuture != null) {
            cleanerFuture.cancel(true);
        }
        map.clear();
    }

    /**
     * Tests if the specified key is in this cache.
     *
     * @param key possible key
     * @return true if and only if the specified key is in this cache, otherwise false.
     */
    public boolean contains(String key)
    {
        return map.containsKey(key);
    }

    /**
     * Removes the {@link Fragment} stored under the argument key if one exists and it equals
     * the {@link Fragment} referenced by the argument {@link Fragment}.
     *
     * @param key The key of a {@link Fragment} to evict.
     * @param expected The {@link Fragment} expected to be removed.
     *
     * @return true if the argument key was mapped to the argument {@link Fragment}, otherwise false.
     */

    protected boolean evict(String key, MemoryFragment expected) {
        return map.remove(key, expected);
    }

    /**
     * Gets the {@link Fragment} stored under the argument key if one exists.
     *
     * @param key The key of a {@link Fragment} to get
     * @return The {@link Fragment} stored via the argument key otherwise null.
     */
    public MemoryFragment get(String key) {
        return map.get(key);
    }

    /**
     * Gets the {@link Fragment} stored under the argument key if one exists.
     *
     * @param key The key of a {@link Fragment} to get
     * @param condition An optional condition upon creation of the {@link Fragment};
     *        if the condition supplied to tryAcquireLock does not match this
     *        condition, then the cache considers itself in need of update.
     * @param timeout The timeout for this cached {@link Fragment}
     *
     * @return The {@link Fragment} stored via the argument key otherwise null.
     */
    public MemoryFragment getOrCreate(String key, String condition, long timeout)
    {
        // Try to get an existing Fragment
        MemoryFragment fragment = get(key);

        // If not ...
        if (fragment == null)
        {
            if (log.isTraceEnabled()) log.trace(key + ": NOT FOUND, creating...");

            // If the fragment didn't exist, store a new one...
            // Necessary to use putIfAbset, because the cache could have changed
            // since calling get(key) above. If the call to putIfAbsent returns
            // non-null, then there was already a MemoryFragment in the cache, so
            // use the response value.
            fragment = new MemoryFragment(condition, timeout);
            MemoryFragment cached = map.putIfAbsent(key, fragment);
            if (cached != null) fragment = cached;
        }
        else if (fragment != null && fragment.isLockAbandoned())
        {
            // Or if it exists but is abandoned (lock-for-update has been held > lock wait timeout).

            // If we're here, it probably means that a thread hit an exception while updating the
            // old fragment and was never able to unlock the it.  So, remove the old fragment, then
            // create a new one starting with the old content.

            log.warn(key + ": DIRTY, replacing");
            MemoryFragment clone = fragment.clone(condition, timeout);
            fragment = map.replace(key, fragment, clone) ? clone : map.get(key);
        }
        else if (log.isTraceEnabled())
            log.trace(key + ": FOUND");

        return fragment;
    }

    /**
     * @return An {@link Iterator} for the keys in the cache.
     */
    protected Iterator<String> keyIterator()
    {
        return map.keySet().iterator();
    }

    /**
     * @return The number of fragments currently in the cache.
     */
    public int size() {
        return map.size();
    }

    /**
     * @return statistics about the current state of the cache, or historical data
     *         if the implementation supports that.
     */
    public FragmentCacheStatistics getStatistics()
    {
        return new MemoryFragmentCacheStatistics(
                        map.size(),
                        cleaner.getLastCleanCount(),
                        cleaner.getLastCleanTimestamp()
                    );
    }
    
    /**
     * Modifies the schedule the {@FragmentCacheCleaner} runs on.
     * Non-blocking method, so two threads could theoretically schedule
     * the cleaner at the same time and not interfere with each other.
     *
     * @param interval The time interval
     * @param unit The time unit the interval is in
     */
    public final void scheduleCleaner(long interval, TimeUnit unit, boolean runNow)
    {
        if (cleanerFuture != null)
            cleanerFuture.cancel(false);

        if (runNow)
            cleaner.run();

        log.info("Scheduling cleaner task every " + interval + " " + unit.name());
        cleanerFuture = executor.scheduleAtFixedRate(cleaner, interval, interval, unit);
    }
}
