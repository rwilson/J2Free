/*
 * FragmentCache.java
 *
 * Created on September 30, 2008, 9:12 AM
 */

package org.j2free.jsp.tags.cache;

import java.io.IOException;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.cache.Computable;
import org.j2free.cache.Memoizer;
import static org.j2free.etc.ServletUtils.*;

/**
 * Generated tag handler class.
 * @author  ryan
 * @version
 */

public class FragmentCache extends BodyTagSupport {
    
    private static final Log log = LogFactory.getLog(FragmentCache.class);

    private static final String ATTRIBUTE_DISABLE_GLOBALLY = "disable-html-cache";
    private static final String ATTRIBUTE_DISABLE_ONCE     = "nocache";

    private static final Computable<Fragment,String>  cache           = new Memoizer<Fragment, String>();

    /**
     *  @TODO implement cron expression based expiration to allow for easy definition
     *        of expiration times in absolute terms
     */
    
    // Cache Timeout
    // -1 will force expiration and the result will not be cached
    private long timeout;
    
    // Cache Key
    private String key;
    
    // Cache Condition, used to specify a value to monitor for a change
    private String condition;
    
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
    
    @Override
    public int doStartTag() throws JspException {

        // Construct the Fragment to evaluate
        Fragment fragment = new Fragment(key,condition,timeout,disable);

        // Check for a globalDisableFlat
        String globalDisableFlag = (String)pageContext.getServletContext().getAttribute(ATTRIBUTE_DISABLE_GLOBALLY);

        // If the globalDisableFlag is set and the fragment would otherwise evaluate, override the disable value for the fragment
        if (globalDisableFlag != null && !fragment.isDisabled()) {
            log.trace("globalDisableFlag = " + globalDisableFlag);
            try {
                fragment.setDisable(Boolean.parseBoolean(globalDisableFlag));
            } catch (Exception e) {
                log.warn("Invalid value for " + ATTRIBUTE_DISABLE_GLOBALLY + " context-param: " + globalDisableFlag + ", expected [true|false]");
            }
        }

        while (true) {
            Future<String> futureResult = cache.get(key);
            if (futureResult == null) {
                
            }
            try {
                return futureResult.get();
            } catch (CancellationException e) {
                cache.remove(key,futureResult);
            } catch (ExecutionException e) {
                throw new JspException(e);
            }
        }

        /** 
         * Reasons to evaluate the body:
         *  1. Nothing is cached under the key
         *  2. The nocache attribute is set
         *  3. the disable attribute is set
         */
        if (!cache.containsKey(key) || pageContext.getAttribute(ATTRIBUTE_DISABLE_ONCE) != null || disable) {
            if (log.isTraceEnabled()) {
                log.trace("Evaluating body: [key=" + key + ",containsKey=" + cache.containsKey(key) + ",nocache=" + (pageContext.getAttribute(ATTRIBUTE_DISABLE_ONCE) != null) + ",disable=" + disable + "]");
            }
            log.debug("Evaluating body for key: " + key);
            return EVAL_BODY_BUFFERED;
        }
        
        long now = System.currentTimeMillis();
        long exp = 0;
        if (cacheTimestamps != null && !cacheTimestamps.isEmpty() && cacheTimestamps.containsKey(key)) {
            exp = cacheTimestamps.get(key) + timeout;
        }
        
        /**
         * Reasons to evaluate the body:
         *  1. The cache timeout has occurred
         *  2. There is a cacheCondition saved, the condition attribute is set, and the condition attribute does not match the saved condition
         */
        if (exp <= now || (cacheConditions.containsKey(key) && !empty(condition) && !cacheConditions.get(key).equals(condition))) {
            if (log.isTraceEnabled()) {
                log.trace("Evaluating body: [key=" + key + ",exp=" + exp + ",now=" + now + ",cacheCondition.containsKey=" + cacheConditions.containsKey(key) + ",!empty(condition)=" + !empty(condition) + "]");
                if (cacheConditions.containsKey(key) && !empty(condition)) {
                    log.trace("cacheConditions.get(" + key + ").equals(" + condition + ") = " + cacheConditions.get(key).equals(condition));
                }
            }
            log.debug("Evaluating body for key: " + key);
            return EVAL_BODY_BUFFERED;
        }

        try {
            log.debug("Writing cached body for key: " + key);
            pageContext.getOut().print(cache.get(key));
        } catch (IOException ex) {
            throw new JspException(ex);
        }
        
        return SKIP_BODY;
    }

    @Override
    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

    @Override
    public int doAfterBody() throws JspException {
        
        // Get the BodyContent object
        BodyContent bc = getBodyContent();
        try {

            // Write the output to the page
            bc.writeOut(bc.getEnclosingWriter());
            
        } catch (IOException ex) {
            throw new JspException(ex);
        }
        
        if (disable)
            return SKIP_BODY;
        
        if (timeout == -1) {
            
            // Clear any cached value
            cache.remove(key);
            cacheTimestamps.remove(key);
            cacheConditions.remove(key);

            log.debug("Clearing cached body for key: " + key);
            
        } else {
            
            // Cache the result
            cache.put(key,bc.getString());
            cacheTimestamps.put(key,System.currentTimeMillis());
            
            if (!empty(condition)) {
                if (log.isTraceEnabled()) {
                    log.trace("Cache condition exists, adding to map [key=" + key + ",condition=" + condition + "]");
                }
                log.debug("Caching body for key: " + key);
                cacheConditions.put(key,condition);
            }
            
        }
        
        return SKIP_BODY;
    }

}
