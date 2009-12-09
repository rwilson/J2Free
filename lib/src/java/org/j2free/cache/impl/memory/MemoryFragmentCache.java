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
public class MemoryFragmentCache implements FragmentCache<MemoryFragment> {

    private final Log log = LogFactory.getLog(getClass());

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
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // A ScheduledFuture representing the cleaner task
    private static volatile ScheduledFuture cleanerFuture = null;

    public MemoryFragmentCache(Configuration config) {
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
        this.map     = new ConcurrentHashMap<String,MemoryFragment>(initialSize);
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
    public MemoryFragmentCache(int initialSize, float loadFactor, int concurrencyLevel) {
        this.map     = new ConcurrentHashMap<String,MemoryFragment>(initialSize, loadFactor, concurrencyLevel);
        this.cleaner = new MemoryFragmentCleaner(this);
    }

    public void destroy() {
        clear();
        if (cleanerFuture != null) {
            cleanerFuture.cancel(true);
        }
    }

    public int clear() {
        int size = map.size();
        map.clear();
        return size;
    }

    public boolean contains(String key) {
        return map.containsKey(key);
    }

    public MemoryFragment createFragment(String condition, long timeout) {
        return new MemoryFragment(condition, timeout);
    }

    public MemoryFragment createFragment(MemoryFragment base, String condition, long timeout) {
        return base.clone(condition, timeout);
    }

    public MemoryFragment evict(String key) {
        return map.remove(key);
    }

    public boolean evict(String key, MemoryFragment expected) {
        return map.remove(key, expected);
    }

    public MemoryFragment get(String key) {
        return map.get(key);
    }

    public Iterator<String> keyIterator() {
        return map.keySet().iterator();
    }

    public MemoryFragment put(String key, MemoryFragment fragment) {
        return map.put(key, fragment);
    }

    public MemoryFragment putIfAbsent(String key, MemoryFragment fragment) {
        MemoryFragment cached = map.putIfAbsent(key, fragment);
        return cached == null ? fragment : cached;
    }

    public MemoryFragment replace(String key, MemoryFragment expected, MemoryFragment replacement) {
        return map.replace(key, expected, replacement) ? replacement : map.get(key);
    }

    public int size() {
        return map.size();
    }

    public FragmentCacheStatistics getStatistics() {
        return new MemoryFragmentCacheStatistics(map.size(), cleaner.getLastCleanCount(), cleaner.getLastCleanTimestamp());
    }
    
    /**
     * Modifies the schedule the {@FragmentCacheCleaner} runs on.
     * Non-blocking method, so two threads could theoretically schedule
     * the cleaner at the same time and not interfere with each other.
     *
     * @param interval The time interval
     * @param unit The time unit the interval is in
     */
    public void scheduleCleaner(long interval, TimeUnit unit, boolean runNow) {

        if (cleanerFuture != null)
            cleanerFuture.cancel(false);

        if (runNow)
            cleaner.run();

        log.info("Scheduling cleaner task every " + interval + " " + unit.name());
        cleanerFuture = executor.scheduleAtFixedRate(cleaner, interval, interval, unit);
    }

}
