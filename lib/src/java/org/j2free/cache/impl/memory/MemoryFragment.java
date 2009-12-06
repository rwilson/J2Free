/*
 * MemoryFragment.java
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.cache.Fragment;
import org.j2free.util.ServletUtils;

/**
 * MemoryFragment may be safely accessed by multiple threads because it's thread-safety
 * is encapsulated.  MemoryFragment uses a one-time <code>CountDownLatch</code> to determine
 * whether it has been initialized yet.  It also uses a <code>ReentrantLock</code> to
 * lock-for-update, so that only a single thread may acquire the right to update this
 * MemoryFragment.  Access to the actual content of the frament is synchronized on the objects
 * internal monitor, as is the condition, and the locked and updated timestamps.
 *
 * @author Ryan Wilson
 * @version 1.0
 */
@ThreadSafe
public class MemoryFragment implements Fragment {

    private static final Log log = LogFactory.getLog(MemoryFragment.class);

    // Max time in ms that a thread may hold the lock-for-update on this MemoryFragment
    private static final int MAX_LOCK_HOLD = 90000;

    @GuardedBy("this") private String content;
    @GuardedBy("this") private String condition;
    
    @GuardedBy("this") private long   locked;
    @GuardedBy("this") private long   updated;

    private final long   timeout;

    private final ReentrantLock  updateLock;
    private final CountDownLatch initialized;

    /**
     *
     * @param condition An optional condition upon creation of the MemoryFragment;
     *        if the condition supplied to tryAcquireLock does not match this
     *        condition, then the cache considers itself in need of update.
     *
     * @param timeout The timeout for this cached MemoryFragment
     */
    public MemoryFragment(String condition, long timeout) {
        this(null,condition,timeout);
    }

    /**
     *
     * @param content An start value for the content of this MemoryFragment
     * @param condition An optional condition upon creation of the MemoryFragment;
     *        if the condition supplied to tryAcquireLock does not match this
     *        condition, then the cache considers itself in need of update.
     *
     * @param timeout The timeout for this cached MemoryFragment
     */
    public MemoryFragment(MemoryFragment oldFragment, String condition, long timeout) {

        this.content   = oldFragment != null ? oldFragment.content : null;
        this.condition = (condition != null && condition.equals("")) ? null : condition;
        this.timeout   = timeout;
        
        this.updateLock  = new ReentrantLock();
        this.initialized = new CountDownLatch(1);

        this.updated = System.currentTimeMillis();
        this.locked  = -1;
    }

    /**
     * If the content of this MemoryFragment has not yet been initialized, this method
     * will block until it has.  Otherwise, it returns content immediately.
     *
     * @return the content
     */
    public String get() throws InterruptedException {
        initialized.await();
        synchronized (this) {
            return content;
        }
    }
    
    /**
     * If the content of this MemoryFragment has not yet been initialized, this method
     * will block until either (1) it is initialized, or (2) <code>waitFor</code>
     * has passed.  Otherwise, it will return content immediately, even if currently
     * locked for update.
     *
     * @param waitFor how long to wait
     * @param unit the TimeUnit to wait for
     * @return the content
     */
    public String get(long waitFor, TimeUnit unit) throws InterruptedException {
        initialized.await(waitFor, unit);
        synchronized (this) {
            return content;
        }
    }
    
    /**
     * @return true if the MemoryFragment is expired, locked, and the lockWait has passed,
     *         otherwise false.
     */
    public synchronized boolean isExpiredAndAbandoned() {
        final long now = System.currentTimeMillis();
        return (now - updated) >= timeout &&                              // expired
               (now - locked ) >= MAX_LOCK_HOLD && updateLock.isLocked(); // Abandoned
    }

    /**
     * @return true if the MemoryFragment is expired and unlocked, or if the fragment
     *         abandoned (has been locked for > than lockWaitTimeout)
     */
    public synchronized boolean isExpiredUnlockedOrAbandoned() {
        final long now = System.currentTimeMillis();
        final boolean isLocked = updateLock.isLocked();
        return (!isLocked && (now - updated) >= timeout ) ||
               ( isLocked && (now - locked ) >= MAX_LOCK_HOLD);
    }

    /**
     * Atomic bundling of a few tasks:
     *  (a) Check if this fragment is expired
     *  (b) Check if the condition under which this fragment was created has expired
     *  (c) Check that no other Thread currently has a lock-for-update on this MemoryFragment
     *  - If (a || b) && c then lock this MemoryFragment for update by the calling Thread
     *    and return true; otherwise return false.
     *
     * @param The current condition
     * @return true if this fragment has been locked for update by the
     *         calling thread, otherwise false
     */
    public boolean tryAcquireForUpdate(final String condition) {

        // If the caller Thread is already the owner, just return true
        if (updateLock.isHeldByCurrentThread()) {
            if (log.isTraceEnabled()) log.trace("tryAcquireForUpdate: current thread already has the lock, short-circuiting...");
            return true;
        }

        // If this MemoryFragment is currently locked for update by a different Thread
        // and the lock has not expired, return false.
        if (updateLock.isLocked()) {
            if (log.isTraceEnabled()) log.trace("tryAcquireForUpdate: Fragment currently locked by another thread, returning false...");
            return false;
        }

        boolean success = false;

        synchronized (this) {
            
            // Check the conditions for update
            boolean condChanged = this.condition != null && !condition.equals(this.condition);

            // If the content is null, the condition has changed, or the MemoryFragment is expired, try to acquire the lock
            success = (content == null || (System.currentTimeMillis() - updated) >= timeout || condChanged) ? updateLock.tryLock() : false;

            if (log.isTraceEnabled()) {
                log.trace("tryAcquireForUpdate: success status [content == null ? " + (content == null) + ", condChanged = " + condChanged + ", expired = " + ((System.currentTimeMillis() - updated) >= timeout) + "]");
            }

            if (success)
                locked = System.currentTimeMillis();
            
        }
        
        return success;
    }

    /**
     * This try will attempt to force-lock the fragment for update
     * regardless of condition or expiration status.  It is meant to
     * be used to refresh the cache due to a manual request.
     *
     * @return true if this fragment has been locked for update by the
     *         calling thread, otherwise false
     */
    public boolean tryAcquireForUpdate() {

        // If the caller Thread is already the owner, just return true
        if (updateLock.isHeldByCurrentThread()) {
            if (log.isTraceEnabled()) log.trace("tryAcquireForUpdate: current thread already has the lock, short-circuiting...");
            return true;
        }

        // If this MemoryFragment is currently locked for update by a different Thread
        // and the lock has not expired, return false.
        if (updateLock.isLocked()) {
            if (log.isTraceEnabled()) log.trace("tryAcquireForUpdate: Fragment currently locked by another thread, returning false...");
            return false;
        }

        boolean success = false;

        synchronized (this) {

            // If the content is null, the condition has changed, or the MemoryFragment is expired, try to acquire the lock
            success = updateLock.tryLock();

            if (success)
                locked = System.currentTimeMillis();
        }

        return success;
    }

    /**
     *  Checks that the caller Thread owns the update lock; if so
     *  updates the content and notifies other threads that may be
     *  waiting to get the content, otherwise returns false.
     *
     *  @param content the content to set
     *  @param condition the current condition
     */
    public boolean tryUpdateAndRelease(String content, String condition) {

        // Make sure the caller owns the lock
        if (!updateLock.isHeldByCurrentThread())
            return false;

        // Update the fields
        synchronized (this) {
            this.content   = ServletUtils.compressHTML(content);
            this.condition = (condition != null && condition.equals("")) ? null : condition;
            this.updated   = System.currentTimeMillis();
        }

        // Unlock the lock for update
        updateLock.unlock();

        // initialized guards content from being returned until this method has been
        // called at least once. So, count down the latch.
        initialized.countDown();

        return true;
    }

    /**
     *  Attemtps to release the lock if the current thread holds it,
     *  otherwise does nothing.
     */
    public void tryRelease() {

        if (!updateLock.isHeldByCurrentThread())
            return;

        updateLock.unlock();
    }
}
