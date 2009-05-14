/*
 * SerialTranscoder.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util;

import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

/**
 * <tt>SerialTranscoder</tt> is really just a wrapper around a standard
 * SerializingTranscoder providing an auto-casting casting of the response
 * to <tt>T</tt>.
 *
 * @author Ryan Wilson
 */
public class SerialTranscoder<T> implements Transcoder<T> {

    private SerializingTranscoder serializer;
    
    public SerialTranscoder() {
        serializer = new SerializingTranscoder();
    }

    public CachedData encode(T score) {
        return serializer.encode(score);
    }

    public T decode(CachedData data) {
        return (T)serializer.decode(data);
    }

    public int getMaxSize() {
        return serializer.getMaxSize();
    }

}
