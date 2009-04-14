/*
 * FragmentCacheTag.java
 *
 * Created on September 30, 2008, 9:12 AM
 */

package org.j2free.jsp.tags.cache;

import java.io.IOException;

import java.util.Calendar;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author  Ryan
 */
public class FragmentCacheTag extends BodyTagSupport {
    
    private static final Log log = LogFactory.getLog(FragmentCacheTag.class);

    private static final long WARNING_COMPUTE_DURATION = 5000;

    // This is the max amount of time a thread will wait() on another thread
    // that is currently updating the Fragment.  If the updating thread does
    // not complete the update within REQUEST_WAIT_TIMEOUT, the waiting thread
    // will print a message to refresh the page.
    private static final long REQUEST_WAIT_TIMEOUT     = 20 * 1000;

    // This is specified in seconds
    private static final long CLEANER_INTERVAL         = 30 * 60;

    private static final String ATTRIBUTE_DISABLE_GLOBALLY = "disable-html-cache";
    private static final String ATTRIBUTE_DISABLE_ONCE     = "nocache";

    // A single-threaded executor to run the cleaner task
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // The backing map
    private static final ConcurrentMap<String,Fragment> cache = new ConcurrentHashMap<String,Fragment>(2000,0.8f,32);

    // Schedule the cleaner task.  NOTE: This method of determining the delay until the first execution based on
    // the current time ONLY works when CLEANER_INTERVAL is less than 24 * 60 * 60
    static {
        Calendar cal = Calendar.getInstance();
        long seconds = (cal.get(Calendar.HOUR_OF_DAY) * 60 * 60) + (cal.get(Calendar.MINUTE) * 60) + cal.get(Calendar.SECOND);
        long delay   = CLEANER_INTERVAL - (seconds % CLEANER_INTERVAL);
        executor.scheduleAtFixedRate(new FragmentCleaner(cache), delay, CLEANER_INTERVAL, TimeUnit.SECONDS);
    }

    // Cache Timeout
    private long timeout;
    
    // Cache Key
    private String key;
    
    // Cache Condition, used to specify a value to monitor for a change
    private String condition;

    // If true, the cache will be ignored for this request
    private boolean disable;
    
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

        // If the tag isn't set to disable, check for other things that might disable this ...
        if (!disable) {

            // Check for the global disable flag
            String globalDisableFlag = (String)pageContext.getServletContext().getAttribute(ATTRIBUTE_DISABLE_GLOBALLY);
            if (globalDisableFlag != null) {
                try {
                    disable = Boolean.parseBoolean(globalDisableFlag);
                } catch (Exception e) {
                    log.warn("Invalid value for " + ATTRIBUTE_DISABLE_GLOBALLY + " context-param: " + globalDisableFlag + ", expected [true|false]");
                }
            }

            // Check for the request attribute
            if (pageContext.getAttribute(ATTRIBUTE_DISABLE_ONCE) != null)
                disable = true;
            
        }

        // If disable is set, either by the tag, the request attribute, or the global flag, then ignore the cache
        if (disable) {
            if (log.isTraceEnabled()) log.trace("Cache disabled for " + key);
            return EVAL_BODY_BUFFERED;
        }

        // See if there is a cached Fragment already
        Fragment cached = cache.get(key);

        // If not ...
        // Or if it exists but has expired and is abandoned (meaning its' lock-for-update
        // has been held past its' lock wait timeout.
        if (cached == null || (cached != null && cached.isExpiredAndAbandoned())) {

            Fragment fragment;

            if (cached == null) {
                // Create a Fragment, this will lock the Fragment to the current Thread
                if (log.isTraceEnabled()) log.trace("cached == null [key: " + key + "]");
                fragment = new Fragment(condition, timeout);
            } else {
                // Remove the old Fragment, then create a new one starting with the old content
                log.warn("Found unmodifiable Fragment, removing [key: " + key + "], then recreating");
                cache.remove(key, cached);
                fragment = new Fragment(cached,condition,timeout);
            }


            // Necessary to use putIfAbset, because the map could have changed
            // between calling cache.get(key) above, and now
            cached = cache.putIfAbsent(key, fragment);

            // If putIfAbsent(key,fragment) returned null, then there was not
            // a Fragment for this key in the map, even by this point, so
            // that means this thread should definitely render the fragment
            if (cached == null) {
                if (log.isTraceEnabled()) log.trace("cached == null, this thread taking responsibility [key: " + key + "]");
                cached = fragment;
                return EVAL_BODY_BUFFERED;
            }
        }

        // Try to acquire the lock for update.  If successful, then
        // the Fragment needs to be updated and this Thread has taken
        // the responsibility to do so.  If it is not successful, check
        // to see if the cache is expired and the cache has been locked
        // for too long.  If so, then the previous execution probably
        // ended in an execution and the lock was never released.  Since
        // there isn't a way to unlock the fragment, we need to replace
        // the fragment with a new one.
        if (cached.tryAcquire(condition)) {
            if (log.isTraceEnabled()) log.trace("successfully acquired lock-for-update [key: " + key + "]");
            return EVAL_BODY_BUFFERED;
        }

        if (log.isTraceEnabled()) log.trace("denied lock-for-update [key: " + key + "]");

        // Try to get the content, cached.get() will block if
        // the content of the fragment is not yet set, so catch an
        // InterruptedException
        String response = null;
        try {
            if (log.isTraceEnabled()) log.trace("calling cached.getContent() [key: " + key + "]");
            response = cached.get(REQUEST_WAIT_TIMEOUT);
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

        if (duration > WARNING_COMPUTE_DURATION) {
            log.debug("FragmentCache completed in " + duration + "ms [key: " + key + "]");
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

        if (log.isTraceEnabled()) log.trace("BodyContent rendered [key: " + key + "]");

        // Attempt to write the body to the page
        try {
            if (log.isTraceEnabled()) log.trace("Writing to page [key: " + key + "]");
            body.writeOut(body.getEnclosingWriter());
        } catch (IOException e) {
            log.error("Error writing bodyContent to page",e);
        }

        if (!disable) {

            if (log.isTraceEnabled()) log.trace("doAfterBody Getting Fragment [key: " + key + "]");

            // Get a reference to the Fragment, then try to update it.
            Fragment cached = cache.get(key);
            if (cached == null) {
                log.warn("Fragment for key[" + key + "] became null between doStartTag and doAfterBody, probably removed by cleaner");
            } else {
                if (!cached.tryUpdateAndRelease(body.getString(), condition)) {
                    log.warn("Failed to update Fragment, unable to obtain lock.  Fragment must have changed since doStartTag.");
                } else if (log.isTraceEnabled()) {
                    log.trace("Cache updated and lock released [key: " + key + "]");
                }
            }
            
        } else if (log.isTraceEnabled()) {
            log.trace("Disable set, not caching [key: " + key + "]");
        }
        
        return SKIP_BODY;
    }

    @Override
    public int doEndTag() throws JspException {
        Fragment fragment = cache.get(key);
        if (fragment != null) {
            fragment.tryRelease();
        }
        return EVAL_PAGE;
    }
}
