/*
 *  MemoryFragmentCleaner.java
 *
 *  Created April 10th, 2009 5:03 AM
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

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * FragmentCache cleaner
 *
 * @author  Ryan Wilson
 */
@ThreadSafe
public final class MemoryFragmentCleaner implements Runnable {

    private final Log log = LogFactory.getLog(MemoryFragmentCleaner.class);

    private final MemoryFragmentCache cache;

    @GuardedBy("this") private long lastCleanTimestamp;
    @GuardedBy("this") private int lastCleanCount;

    public MemoryFragmentCleaner(MemoryFragmentCache cache)
    {
        super();
        this.cache = cache;

        this.lastCleanTimestamp = -1;
        this.lastCleanCount     = -1;
    }

    public void run()
    {
        long start = System.currentTimeMillis();

        Iterator<String> iterator = cache.keyIterator();

        int count = 0;
        
        synchronized (this) {

            lastCleanCount = 0;

            String key;
            MemoryFragment fragment;
            while (iterator.hasNext())
            {
                key = iterator.next();
                fragment = cache.get(key);

                // @TODO remove race-condition where fragment could be locked b/t calls to isExpiredOrLockAbandoned and evict
                if (fragment != null && fragment.isExpiredOrLockAbandoned()) {
                    if (cache.evict(key, fragment))
                        lastCleanCount++;
                }

                count++;
            }

            lastCleanTimestamp = System.currentTimeMillis();
        }
        
        log.info("FragmentCleaner complete [" + (System.currentTimeMillis() - start) + "ms, " + lastCleanCount + " of " + count + " cleaned]");
    }

    public synchronized long getLastCleanTimestamp() {
        return lastCleanTimestamp;
    }

    public synchronized int getLastCleanCount() {
        return lastCleanCount;
    }
}
