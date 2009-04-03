/*
 * FragmentCache.java
 *
 * Created on September 30, 2008, 9:12 AM
 */

package org.j2free.jsp.tags.cache;


import java.io.IOException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generated tag handler class.
 * @author  ryan
 * @version
 */

public class FragmentCache extends BodyTagSupport {
    
    private static final Log log = LogFactory.getLog(FragmentCache.class);

    private static final String ATTRIBUTE_DISABLE_GLOBALLY = "disable-html-cache";
    private static final String ATTRIBUTE_DISABLE_ONCE     = "nocache";

    private static final ConcurrentMap<String,Fragment> cache = new ConcurrentHashMap<String,Fragment>();

    // Cache Timeout
    private long timeout;
    
    // Cache Key
    private String key;
    
    // Cache Condition, used to specify a value to monitor for a change
    private String condition;

    // If true, the cache will be ignored for this request
    private boolean disable;
    
    public FragmentCache() {
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

        

        if (disable)
            return EVAL_BODY_BUFFERED;

        // See if there is a cached Fragment already
        Fragment cached = cache.get(key);

        // If not ...
        if (cached == null) {

            // Create a Fragment
            Fragment fragment = new Fragment(condition, timeout);

            // Acquire the lock on this fragment now, otherwise it could be
            // acquired by a different thread after being put in the map
            fragment.tryAcquireUpdate(condition);

            // Necessary to use putIfAbset, because the map could have changed
            // between calling cache.get(key) above, and now
            cached = cache.putIfAbsent(key, fragment);

            // If putIfAbsent(key,fragment) returned null, then there was not
            // a Fragment for this key in the map, even by this point, so
            // that means this thread should definitely render the fragment
            if (cached == null) {
                cached = fragment;
                return EVAL_BODY_BUFFERED;
            }
        }

        // Try to acquire the lock for update.  If successful, then
        // the Fragment needs to be updated and this Thread has taken
        // the responsibility to do so.
        if (cached.tryAcquireUpdate(condition))
            return EVAL_BODY_BUFFERED;

        // Try to get the content, cached.getContent() will block if
        // the content of the fragment is not yet set, so catch an
        // InterruptedException
        String response = null;
        try {
            response = cached.getContent();
        } catch (InterruptedException e) {
            log.error("Error writing Fragment.getContent() to page",e);
            response = "Caching error, please reload the page.";
        }

        // Write the response
        try {
            pageContext.getOut().write(response);
        } catch (IOException e) {
            log.error("Error writing Fragment.getContent() to page",e);
        }
        
        return SKIP_BODY;
    }

    @Override
    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

    /**
     *  Write the content to the apge, try to acquire lock and update, release lock,
     *  then return SKIP_BODY when complete
     */
    @Override
    public int doAfterBody() throws JspException {

        // Get the BodyContent, the result of the processing of the body of the tag
        BodyContent body = getBodyContent();

        // Attempt to write the body to the page
        try {
            body.writeOut(body.getEnclosingWriter());
        } catch (IOException e) {
            log.error("Error writing bodyContent to page",e);
        }

        if (!disable) {

            // Get a reference to the Fragment, then try to update it.
            Fragment cached = cache.get(key);
            cached.tryUpdateAndRelease(body.getString(), condition);
            
        }
        
        return SKIP_BODY;
    }

}
