/*
 * FragmentCacheTag.java
 *
 * Created on April 2nd, 2009
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
package org.j2free.jsp.tags;

import java.io.IOException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.cache.Fragment;
import org.j2free.cache.FragmentCache;

import static org.j2free.util.ServletUtils.*;

/**
 *  Implementation of a fragment cache as a custom tag.  This fragment cache
 *  guarantees that only a single thread may trigger an update
 * 
 * @author  Ryan Wilson
 */
public class FragmentCacheTag extends BodyTagSupport {
    
    private static final Log log = LogFactory.getLog(FragmentCacheTag.class);

    /***********************************************************************
     * Static Implementation
     */
    private static final String ATTRIBUTE_FORCE_REFRESH = "nocache";

    // The possible FragmentCaches
    private static final ConcurrentHashMap<String, FragmentCache> caches
            = new ConcurrentHashMap<String, FragmentCache>(5, 1.0f, 5);

    // The default FragmentCache
    private static final AtomicReference<FragmentCache> defaultCache
            = new AtomicReference<FragmentCache>(null);

    /**
     * @return The underlying {@link FragmentCache} associated with the specified
     *         strategy name; useful for code looking to evict a cache {@link Fragment}
     *         outside of the FragmentCacheTag.
     */
    public static FragmentCache getCache(String strategy) {
        return caches.get(strategy);
    }

    /**
     * @return The underlying default {@link FragmentCache}, useful for code looking
     *         to evict a cache {@link Fragment} outside of the FragmentCacheTag.
     */
    public static FragmentCache getDefaultCache() {
        return defaultCache.get();
    }

    /**
     * Registers the specified cache under the specified cache strategy name.
     * If this is the first cache strategy to be registered, it will also
     * become the default.
     * 
     * @param name The name by which to reference this cache strategy
     * @param cache An instance of FragmentCache
     */
    public static void registerStrategy(String name, FragmentCache cache) {
        caches.put(name, cache);
        if (defaultCache.get() == null) {
            defaultCache.compareAndSet(null, cache);
        }
    }

    /**
     * Globally turn the cache on or off (off by default)
     */
    private static final AtomicBoolean enabled = new AtomicBoolean(false);

    /**
     * Enables fragment caching
     */
    public static void enable() {
        enabled.set(true);
    }

    /**
     * Disables fragment caching, clears any cached fragments
     */
    public static void disable() {
        enabled.set(false);
        
        for (FragmentCache fc : caches.values()) {
            fc.clear();
        }
    }

    // This is the max amount of time a thread will wait() on another thread
    // that is currently updating the Fragment.  If the updating thread does
    // not complete the update within REQUEST_TIMEOUT, the waiting thread
    // will print a message to refresh the page.
    private static final AtomicLong REQUEST_TIMEOUT = new AtomicLong(20);
    public static void setRequestTimeout(long timeout) {
        REQUEST_TIMEOUT.set(timeout);
    }

    private static final AtomicLong WARNING_COMPUTE_DURATION = new AtomicLong(10000);
    public static void setWarningComputeDuration(long duration) {
        WARNING_COMPUTE_DURATION.set(duration);
    }

    /***********************************************************************
     * Tag Instance Implementation
     */

    // Used to hold a reference to a particular cache b/t start and end tags
    private FragmentCache cache;

    // Cache Timeout
    private long timeout;
    
    // Cache Key
    private String key;
    
    // Cache Condition, used to specify a value to monitor for a change
    private String condition;

    // Cache strategy, if there are multiple registered, this is used
    // to specify which to use
    private String strategy;

    // If true, the cache will be ignored for this request
    private boolean disable;

    // The time unit of the expiration
    private String unit;
    
    public FragmentCacheTag() {
        super();
        disable = false;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    private long start;

    /**
     *  To evaluate the BODY and have control passed to doAfterBody, return EVAL_BODY_BUFFERED
     *  To use the cached content, writed the fragment content to the page, then return SKIP_BODY
     *
     *  Rules:
     *
     *    - Conditionts under which the body should be evaluated:
     *      - Caching is disabled
     *      - Caching is enabled, but no cached fragment exists
     *      - Caching is enabled, but the cache has expired and no other thread is currently refreshing the cache
     *      - Caching is enabled, but the condition has changed, and no other thread is currently refreshing the cache
     *
     *    - Conditions under which the body should NOT be evaluated:
     *      - Caching is enabled and a cached version exists
     *      - Caching is enabled, the cache has expired, but another thread is refreshing the cache
     *      - Caching is enabled, the condition has changed, but another thread is refreshing the cache
     *
     */
    @Override
    public int doStartTag() throws JspException {

        start = System.currentTimeMillis();

        // If the cache isn't enabled, make sure disable is set
        disable |= !enabled.get();

        // If disable is set, either by the tag, the request attribute, or the global flag, then ignore the cache
        if (disable) {
            if (log.isTraceEnabled()) log.trace("Cache disabled for " + key);
            return EVAL_BODY_BUFFERED;
        }

        // timeout is assumed to be on milliseconds, but there
        // is the additional parameter
        if (!empty(unit)) {
            try {
                TimeUnit timeUnit = TimeUnit.valueOf(unit);
                if (timeUnit != TimeUnit.MILLISECONDS) {
                    log.debug("Converting " + timeout + " " + timeUnit.name() + " to ms");
                    timeout = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
                }
            } catch (Exception e) {
                log.warn("Unable to interpret timeout unit");
            }
        }

        // If the user specified a cache, try to get that one
        if (!empty(strategy)) {
            cache = caches.get(strategy);
            if (cache == null) {
                log.warn("Failed to find specified cache: " + strategy);
                cache = defaultCache.get();
            }
        } else {
            // Otherwise use the default
            cache = defaultCache.get();
        }

        if (cache == null) {
            log.error("Failed to find any cache for fragment! [key=" + key + "]");
            return EVAL_BODY_BUFFERED;
        }

        // See if there is a cached Fragment already
        Fragment fragment = cache.get(key);

        // If not ...
        if (fragment == null) {

            if (log.isTraceEnabled()) log.trace("cached == null [key: " + key + "]");

            // If the fragment didn't exist, store a new one...
            // Necessary to use putIfAbset, because the map could have changed
            // since calling cache.get(key) above.  In either case, the implementation
            // of FragmentCache guarantees that the retruned value of this function will
            // be the current Fragment stored in the cache by this key.
            fragment = cache.putIfAbsent(key, cache.createFragment(condition, timeout));
            
        } else if (fragment != null && fragment.isExpiredAndAbandoned()) {
            // Or if it exists but has expired and is abandoned (meaning its' lock-for-update
            // has been held past its' lock wait timeout.
            
            // If we're here, it probably means that a thread hit an exception while updating the old fragment and was never
            // able to unlock the fragment.  So, remove the old fragment, then create a new one starting with the old content
            log.warn("Found unmodifiable Fragment, replacing [key: " + key + "], with a duplicate (but modifiable) Fragment");
            fragment = cache.replace(key, fragment, cache.createFragment(fragment, condition, timeout));
        }

        // Try to acquire the lock for update.  If successful, then
        // the Fragment needs to be updated and this Thread has taken
        // the responsibility to do so.
        if (fragment.tryAcquireForUpdate(condition)) {
            if (log.isTraceEnabled()) log.trace("successfully acquired lock-for-update [key: " + key + "]");
            return EVAL_BODY_BUFFERED;
        }

        boolean forceRefresh = pageContext.getAttribute(ATTRIBUTE_FORCE_REFRESH) != null;

        // If the force-refresh attribute is set, then try to acquire
        // the lock regardless of condition of expiration.  Doing so
        // only return false if another thread is already refreshing it
        // which is fine.
        if (forceRefresh) {
            if (fragment.tryAcquireForUpdate()) {
                if (log.isTraceEnabled()) log.trace("successfully acquired lock-for-update [key: " + key + "]");
                return EVAL_BODY_BUFFERED;
            }
        }

        if (log.isTraceEnabled()) log.trace("denied lock-for-update [key: " + key + ", force-refresh: " + forceRefresh + "]");

        // Try to get the content, cached.get() will block if
        // the content of the fragment is not yet set, so catch an
        // InterruptedException
        String response = null;
        try {
            if (log.isTraceEnabled()) log.trace("calling cached.getContent() [key: " + key + "]");
            response = fragment.get(REQUEST_TIMEOUT.get(), TimeUnit.SECONDS);
            if (log.isTraceEnabled()) log.trace("cached.getContent() returned [key: " + key + "]");
        } catch (InterruptedException e) {
            log.error("Error writing Fragment.getContent() to page",e);
            response = "Sorry, fetching from the cache took longer than we expected.  Please refresh the page.";
        }

        // Write the response
        try {
            if (log.isTraceEnabled()) log.trace("writing output [key: " + key + "]");
            pageContext.getOut().write(response);
        } catch (IOException e) {
            log.error("Error writing Fragment.getContent() to page",e);
        }
        
        long duration = System.currentTimeMillis() - start;

        if (duration > WARNING_COMPUTE_DURATION.get()) {
            log.warn("Warning: slow FragmentCache, " + duration + "ms [key: " + key + "]");
        } else if (log.isTraceEnabled()) {
            log.trace("FragmentCache completed in " + duration + "ms [key: " + key + "]");
        }
        
        return SKIP_BODY;
    }

    /**
     *  Write the content to the apge, try to acquire lock and update, release lock,
     *  then return SKIP_BODY when complete
     */
    @Override
    public int doAfterBody() throws JspException {

        // Get the BodyContent, the result of the processing of the body of the tag
        BodyContent body = getBodyContent();
        String content   = body.getString();

        // Make sure we had a cache
        if (cache != null) {

            Fragment fragment = cache.get(key);

            // Update the Fragment, doing this before writing to the page will release
            // waiting threads sooner.
            if (!disable) {
                if (log.isTraceEnabled()) log.trace("doAfterBody Getting Fragment [key: " + key + "]");

                // Get a reference to the Fragment, then try to update it.
                if (fragment == null) {
                    log.warn("Fragment for key[" + key + "] became null between doStartTag and doAfterBody, probably removed by cleaner");
                } else {
                    if (!fragment.tryUpdateAndRelease(content, condition)) {
                        log.warn("Failed to update Fragment[key=" + key + "], unable to obtain lock.  Fragment must have changed since doStartTag.");
                    } else if (log.isTraceEnabled()) {
                        log.trace("Cache updated and lock released [key: " + key + "]");
                    }
                }

            } else if (log.isTraceEnabled()) {
                log.trace("Disable set, not caching [key: " + key + "]");
            }
        }
        
        // Attempt to write the body to the page
        try {
            if (log.isTraceEnabled()) log.trace("Writing to page [key: " + key + "]");
            body.getEnclosingWriter().write(content);
            if (log.isTraceEnabled()) log.trace("BodyContent rendered [key: " + key + "]");
        } catch (IOException e) {
            log.error("Error writing bodyContent to page",e);
        }

        return SKIP_BODY;
    }

    @Override
    public int doEndTag() throws JspException {
        // Make sure we had a cache
        if (cache != null) {
            Fragment fragment = cache.get(key);
            if (fragment != null) {
                fragment.tryRelease();
            }
        }
        return EVAL_PAGE;
    }

    @Override
    public void release() {
        super.release();
        disable = false;
        cache   = null;
    }

}
