/**
 * RedisFragment.java
 *
 * Copyright (c) 2010 FooBrew, Inc.
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
package org.j2free.cache.impl.redis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.j2free.cache.Fragment;
import redis.clients.jedis.JedisPool;

/**
 * @author Ryan Wilson
 */
@ThreadSafe
public final class RedisFragment implements Fragment
{
    protected static class Field
    {
        protected static String CONTENT   = "content";
        protected static String CONDITION = "condition";

        protected static String[] ALL = new String[] { CONTENT, CONDITION };
    }

    private final Log log = LogFactory.getLog(getClass());

    private final JedisPool pool;

    protected RedisFragment(JedisPool pool)
    {
        this.pool = pool;
    }

    /**
     * @see {@link Fragment}
     *
     * If the content of this Fragment has not yet been initialized, this method
     * will block until either (1) it is initialized, or (2) <code>waitFor</code>
     * has passed.  Otherwise, it will return content immediately, even if currently
     * locked for update.
     *
     * @param waitFor how long to wait
     * @param unit the TimeUnit to wait for
     * @return the content
     */
    public String getContent(long waitFor, TimeUnit unit) throws InterruptedException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @see {@link Fragment}
     *
     * @return true if the Fragment is locked and the lockWait has passed, otherwise false.
     */
    public boolean isLockAbandoned()
    {
    }

    /**
     * @see {@link Fragment}
     *
     * Atomic bundling of a few tasks:
     *  (a) Check if this fragment is expired
     *  (b) Check if the condition under which this fragment was created has expired
     *  (c) Check that no other Thread currently has a lock-for-update on this Fragment
     *  - If (a || b) && c then lock this Fragment for update by the calling Thread
     *    and return true; otherwise return false.
     *
     * @param The current condition (only used if original condition was set)
     * @return true if this fragment has been locked for update by the
     *         calling thread, otherwise false
     */
    public boolean tryLockForUpdate()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @see {@link Fragment}
     *
     * This try will attempt to force-lock the fragment for update
     * regardless of condition or expiration status.  It is meant to
     * be used to refresh the cache due to a manual request.
     *
     * @return true if this fragment has been locked for update by the
     *         calling thread, otherwise false
     */
    public boolean tryLockForUpdate(String condition)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @see {@link Fragment}
     *
     *  Checks that the caller Thread owns the update lock; if so
     *  updates the content and notifies other threads that may be
     *  waiting to get the content, otherwise returns false.
     *
     *  @param content the content to set
     *  @param condition the current condition
     */
    public boolean tryUpdateAndRelease(String content, String condition)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * @see {@link Fragment}
     *
     *  Attemtps to release the lock if the current thread holds it,
     *  otherwise does nothing.
     */
    public void tryRelease()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
