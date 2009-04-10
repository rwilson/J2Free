/*
 *  FragmentCleaner.java
 *
 *  Created April 10th, 2009 5:03 AM
 */
package org.j2free.jsp.tags.cache;

import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author  Ryan
 */
public class FragmentCleaner implements Runnable {

    private static final Log log = LogFactory.getLog(FragmentCleaner.class);

    private final ConcurrentMap<String,Fragment> cache;

    public FragmentCleaner(ConcurrentMap<String,Fragment> cache) {
        super();
        this.cache = cache;
    }

    public void run() {

        long start = System.currentTimeMillis();

        Iterator<String> iterator = cache.keySet().iterator();

        int num = 0;
        
        String key;
        Fragment fragment;
        while (iterator.hasNext()) {

            key      = iterator.next();
            fragment = cache.get(key);
            
            if (fragment != null && fragment.isExpiredUnlockedOrAbandoned()) {
                if (cache.remove(key, fragment))
                    num++;
            }
        }
        
        log.info("FragmentCleaner complete [" + (System.currentTimeMillis() - start) + "ms, " + num + " cleaned]");
    }
}
