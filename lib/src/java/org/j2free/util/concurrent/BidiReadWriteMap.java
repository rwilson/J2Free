/*
 * BidiReadWriteMap.java
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.util.concurrent;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.jcip.annotations.ThreadSafe;

/**
 * A bi-directional, thread-safe, map that uses read-write locks
 * for greater concurrency.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class BidiReadWriteMap<K,V> implements ConcurrentMap<K,V> {

    private final Map<K,V> kToV;
    private final Map<V,K> vToK;

    private final ReadWriteLock lock  = new ReentrantReadWriteLock();
    private final Lock          read  = lock.readLock();
    private final Lock          write = lock.writeLock();

    public BidiReadWriteMap(Map<K,V> kToV, Map<V,K> vToK) {
        this.kToV = kToV;
        this.vToK = vToK;
    }


    /*****************************************************************
     * Map implementation
     */

    public void clear() {
        write.lock();
        try {
            kToV.clear();
            vToK.clear();
        } finally {
            write.unlock();
        }
    }

    /**
     * Atomically clears and repopulates the internal
     * maps with the entries from the provided map.
     * 
     * @param map
     */
    public void clearAndPutAll(Map<K,V> map) {
        write.lock();
        try {
            kToV.clear();
            vToK.clear();
            for (Map.Entry<K,V> entry : map.entrySet()) {
                kToV.put(entry.getKey(), entry.getValue());
                vToK.put(entry.getValue(), entry.getKey());
            }
        } finally {
            write.unlock();
        }
    }

    public boolean containsKey(Object key) {

        if (key == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null keys");

        read.lock();
        try {
            return kToV.containsKey(key);
        } finally {
            read.unlock();
        }
    }

    public boolean containsValue(Object value) {

        if (value == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null values");

        read.lock();
        try {
            return vToK.containsKey(value);
        } finally {
            read.unlock();
        }
    }

    public Set<Map.Entry<K,V>> entrySet() {
        read.lock();
        try {
            return kToV.entrySet();
        } finally {
            read.unlock();
        }
    }

    public V get(Object key) {

        if (key == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null keys");

        read.lock();
        try {
            return kToV.get(key);
        } finally {
            read.unlock();
        }
    }

    public boolean isEmpty() {
        read.lock();
        try {
            return kToV.isEmpty();
        } finally {
            read.unlock();
        }
    }

    public Set<K> keySet() {
        read.lock();
        try {
            return kToV.keySet();
        } finally {
            read.unlock();
        }
    }

    public V put(K key, V value) {

        if (key == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null keys");

        if (value == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null values");

        write.lock();
        try {
            vToK.put(value, key);
            return kToV.put(key, value);
        } finally {
            write.unlock();
        }
    }

    public void putAll(Map t) {
        write.lock();
        try {
            for (Object o : t.keySet()) {
                put((K)o, (V)t.get(o));
            }
        } finally {
            write.unlock();
        }
    }

    public V remove(Object key) {

        if (key == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null keys");

        write.lock();
        try {
            V v = kToV.remove(key);
            if (v != null) {
                vToK.remove(v);
            }
            return v;
        } finally {
            write.unlock();
        }
    }

    public int size() {
        read.lock();
        try {
            return kToV.size();
        } finally {
            read.unlock();
        }
    }

    public Collection<V> values() {
        read.lock();
        try {
            return vToK.keySet();
        } finally {
            read.unlock();
        }
    }


    /*****************************************************************
     * BidiMap implementation
     */
    
    public K getKey(V value) {

        if (value == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null values");

        read.lock();
        try {
            return vToK.get(value);
        } finally {
            read.unlock();
        }
    }

    /*****************************************************************
     * ConcurrentMap implementation
     */

    public V putIfAbsent(K key, V value) {

        if (key == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null keys");

        if (value == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null values");

        write.lock();
        try {
            if (kToV.containsKey(key)) {
                return kToV.get(key);
            }
            kToV.put(key, value);
            vToK.put(value, key);
            return null;
        } finally {
            write.unlock();
        }
    }

    public boolean remove(Object key, Object value) {

        if (key == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null keys");

        if (value == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null values");

        write.lock();
        try {
            if (kToV.containsKey(key) && kToV.get(key).equals(value)) {
                vToK.remove(kToV.remove(key));
                return true;
            }
            return false;
        } finally {
            write.unlock();
        }
    }

    public boolean replace(K key, V oldValue, V newValue) {

        if (key == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null keys");

        if (oldValue == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null values");

        if (newValue == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null values");

        write.lock();
        try {
            if (kToV.containsKey(key) && kToV.get(key).equals(oldValue)) {
                kToV.put(key, newValue); // re-map in keys-to-values
                vToK.remove(oldValue);   // remove old mapping in values-to-keys
                vToK.put(newValue, key); // new mapping in values-to-keys
                return true;
            }
            return false;
        } finally {
            write.unlock();
        }
    }

    public V replace(K key, V value) {

        if (key == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null keys");

        if (value == null)
            throw new NullPointerException("BidiReadWriteMap does not allow null values");

        write.lock();
        try {
            if (kToV.containsKey(key)) {
                V old = kToV.put(key, value); // re-map in keys-to-values and get the old value
                vToK.remove(old);             // remove the old value mapping
                vToK.put(value, key);         // create the new value mapping
                return old;
            }
            return null;
        } finally {
            write.unlock();
        }
    }
}
