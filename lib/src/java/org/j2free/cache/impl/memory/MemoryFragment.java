/*
 * MemoryFragment.java
 *
 * Copyright (c) 2011 FooBrew, Inc.
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

import org.apache.commons.lang.StringUtils;
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
public class MemoryFragment implements Fragment, Cloneable {

    private final Log log = LogFactory.getLog(getClass());

    // Max time in ms that a thread may hold the lock-for-update on this MemoryFragment
    private static final int MAX_LOCK_HOLD = 90000;

    @GuardedBy("this") private String content;
    @GuardedBy("this") private String condition;
    
    @GuardedBy("this") private long   lockedTime;
    @GuardedBy("this") private long   updateTime;

    private final long   timeout;

    private final ReentrantLock  updateLock;
    private final CountDownLatch initialized;

    /**
     * @param condition An optional condition upon creation of the MemoryFragment;
     *        if the condition supplied to tryAcquireLock does not match this
     *        condition, then the cache considers itself in need of update.
     *
     * @param timeout The timeout for this cached MemoryFragment
     */
    public MemoryFragment(String condition, long timeout) {
        this(null, condition, timeout);
    }

    /**
     * @param content An start value for the content of this MemoryFragment
     * @param condition An optional condition upon creation of the MemoryFragment;
     *        if the condition supplied to tryAcquireLock does not match this
     *        condition, then the cache considers itself in need of update.
     *
     * @param timeout The timeout for this cached MemoryFragment
     */
    public MemoryFragment(String content, String condition, long timeout) {

        this.content = content;
        this.timeout = timeout;

        // Use null instead of blank string for condition
        this.condition = StringUtils.isEmpty(condition) ? null : condition;
        
        this.updateLock  = new ReentrantLock();
        this.initialized = new CountDownLatch(1);

        this.updateTime = System.currentTimeMillis();
        this.lockedTime  = -1;
    }

    @Override
    public MemoryFragment clone() throws CloneNotSupportedException {
        return new MemoryFragment(content, condition, timeout);
    }

    /**
     * @param newCondition A condition
     * @param newTimeout A expiration
     * @return A new <tt>MemoryFragment</tt> containing the content of the current
     *         <tt>MemoryFragment</tt>, but using the specified newCondition and
     *         newTimeout
     */
    public MemoryFragment clone(String newCondition, long newTimeout) {
        return new MemoryFragment(content, newCondition, newTimeout);
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
        if (initialized.await(waitFor, unit)) {
            synchronized (this) {
                return content;
            }
        } else {
            return null;
        }
    }
    
    /**
     * @return true if the MemoryFragment is expired, locked, and the lockWait has passed,
     *         otherwise false.
     */
    public synchronized boolean isLockAbandoned() {
        return updateLock.isLocked() && (System.currentTimeMillis() - lockedTime ) >= MAX_LOCK_HOLD;
    }

    /**
     * @return true if the MemoryFragment is expired, or is locked and the lockWait has
     *         passed, otherwise false
     */
    protected synchronized boolean isExpiredOrLockAbandoned() {
        long    now    = System.currentTimeMillis();
        boolean locked = updateLock.isLocked();

        return (locked && (now - lockedTime ) >= MAX_LOCK_HOLD) ||   // lockAbandoned()
               (!locked && (now - updateTime) >= timeout);           // not locked and expired
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
    public boolean tryLockForUpdate(String curCondition) {

        // If the caller Thread is already the owner, just return true
        if (updateLock.isHeldByCurrentThread()) {
            if (log.isTraceEnabled()) log.trace("tryAcquireForUpdate: thread already holds lock, short-circuiting...");
            return true;
        }

        // If this MemoryFragment is currently locked for update by a different Thread
        // and the lock has not expired, return false.
        if (updateLock.isLocked()) {
            if (log.isTraceEnabled()) log.trace("tryAcquireForUpdate: Fragment locked by another thread, returning false...");
            return false;
        }

        boolean acuiredLock = false;

        // Do all the below in a synchronized block so that the condition cannot
        // change between the creation of condChanged and the call to tryLock()
        synchronized (this) {
            
            // Check the conditions for update
            boolean condChanged = this.condition != null && !this.condition.equals(curCondition);

            long now = System.currentTimeMillis();

            // If the content is null, the condition has changed, or the MemoryFragment is expired,
            // try to acquire the lock
            if (content == null || (now - updateTime) >= timeout || condChanged) {
                acuiredLock = updateLock.tryLock();
            }

            // If we got it, update the lock time
            if (acuiredLock) lockedTime = now;

            if (log.isTraceEnabled()) {
                log.trace(
                    String.format(
                        "tryAcquireForUpdate: success status [content==null: %b, condChanged: %b, expired: %b]",
                        content == null,
                        condChanged,
                        (now - updateTime) >= timeout
                    )
                );
            }            
        }
        
        return acuiredLock;
    }

    /**
     * This try will attempt to lock the fragment for update regardless of
     * condition or expiration status.  It is meant to be used to refresh
     * the fragment on a manual request.
     *
     * This method will return true in all cases except when another thread
     * already holds the update lock.
     *
     * @return true if this fragment has been locked for update by the
     *         calling thread, otherwise false
     */
    public boolean tryLockForUpdate() {

        // If the caller Thread is already the owner, just return true
        if (updateLock.isHeldByCurrentThread()) {
            if (log.isTraceEnabled()) log.trace("tryAcquireForUpdate: thread already holds lock, short-circuiting...");
            return true;
        }

        // If this MemoryFragment is currently locked for update by a different Thread
        // and the lock has not expired, return false.
        if (updateLock.isLocked()) {
            if (log.isTraceEnabled()) log.trace("tryAcquireForUpdate: Fragment locked by another thread, returning false...");
            return false;
        }

        boolean acuiredLock = false;

        synchronized (this) {
            // Try to acquire the lock
            acuiredLock = updateLock.tryLock();
            if (acuiredLock) lockedTime = System.currentTimeMillis();
            
        }

        return acuiredLock;
    }

    /**
     *  Checks that the caller Thread owns the update lock; if so
     *  updates the content and notifies other threads that may be
     *  waiting to get the content, otherwise returns false.
     *
     *  @param content the content to set
     *  @param condition the current condition
     */
    public boolean tryUpdateAndRelease(String newContent, String newCondition) {

        // Make sure the caller owns the lock
        if (!updateLock.isHeldByCurrentThread())
            return false;

        // Update the fields
        synchronized (this) {
            this.content    = ServletUtils.compressHTML(newContent);
            this.condition  = StringUtils.isEmpty(newCondition) ? null : newCondition;
            this.updateTime = System.currentTimeMillis();
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
