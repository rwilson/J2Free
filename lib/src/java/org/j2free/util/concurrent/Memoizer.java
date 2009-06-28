/*
 * Memoizer.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import net.jcip.annotations.ThreadSafe;
import org.j2free.util.LaunderThrowable;

/**
 * A thread-safe scalable result-cache
 *
 * @author Ryan
 */
@ThreadSafe
public class Memoizer<A,V> implements Computable<A,V> {

    private final ConcurrentHashMap<A, Future<V>> cache;
    private final Computable<A,V> work;

    public Memoizer(Computable<A,V> c) {
        cache = new ConcurrentHashMap<A, Future<V>>();
        work  = c;
    }

    public V compute(final A arg) throws InterruptedException {
        while (true) {
            Future<V> f = cache.get(arg);
            if (f == null) {
                Callable<V> eval = new Callable<V>() {
                    public V call() throws InterruptedException {
                        return work.compute(arg);
                    }
                };
                FutureTask<V> ft = new FutureTask<V>(eval);
                f = cache.putIfAbsent(arg, ft);
                if (f == null) {
                    f = ft;
                    ft.run();
                }
            }
            try {
                return f.get();
            } catch (CancellationException ce) {
                cache.remove(arg, f);
            } catch (ExecutionException ee) {
                throw LaunderThrowable.launderThrowable(ee.getCause());
            }
        }
    }
}