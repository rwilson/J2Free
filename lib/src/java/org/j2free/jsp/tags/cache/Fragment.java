/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * Fragment.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.jsp.tags.cache;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author ryan
 */
public class Fragment {

    private String content;
    private String condition;
    
    private long   timeout;
    private long   updated;

    private final ReentrantLock lock;

    public Fragment(String condition, long timeout) {
        this.condition = condition;
        this.timeout   = timeout;

        this.content   = null;
        this.updated   = System.currentTimeMillis();
        this.lock      = new ReentrantLock();
    }

    /**
     * If content is null, this method will block until content is available.
     * Otherwise, it will return content immediately, even if currently locked
     * for update.
     *
     * @return the content
     */
    public synchronized String getContent() throws InterruptedException {
        while (content == null)
            wait();

        return content;
    }

    /**
     *  Checks that the caller Thread owns the update lock; if so
     *  updates the content and notifies other threads that may be
     *  waiting to get the content, otherwise returns false.
     *
     *  @param content the content to set
     */
    public synchronized boolean tryUpdateAndRelease(String content, String condition) {

        // Make sure the caller owns the lock
        if (!lock.isHeldByCurrentThread())
            return false;

        this.content   = content;
        this.condition = condition;
        this.updated   = System.currentTimeMillis();

        // Notify all, because Threads may be waiting on getContent()
        notifyAll();

        // Unlock the lock for update
        lock.unlock();

        return true;
    }

    /**
     * Atomic bundling of a few tasks:
     *  (a) Check if this fragment is expired
     *  (b) Check if the condition under which this fragment was created has expired
     *  (c) Check that no other Thread currently has a lock-for-update on this Fragment
     *  - If (a || b) && c then lock this Fragment for update by the calling Thread
     *    and return true; otherwise return false.
     *
     * @param The current condition
     * @return true if this fragment has been locked for update by the
     *         calling thread, otherwise false
     */
    public synchronized boolean tryAcquireUpdate(final String condition) {

        // If the caller Thread is already the owner, just return true
        if (lock.isHeldByCurrentThread())
            return true;

        // If this Fragment is currently locked for update by a different Thread
        // and the lock has not expired, return false.
        if (lock.isLocked())
            return false;

        // Check the conditions for update
        boolean expired     = (System.currentTimeMillis() - updated) > timeout;
        boolean condChanged = !condition.equals(this.condition);

        // If either of the conditions have changed, lock the lock
        if (expired || condChanged) {
            lock.lock();
            return true;
        }

        return false;
    }

}
