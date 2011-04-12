/*
 * Memoizer.java
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
 * @param <A> 
 * @param <V>
 * @author Ryan Wilson
 */
@ThreadSafe
public class Memoizer<A,V> implements Computable<A,V> {

    private final ConcurrentHashMap<A, Future<V>> cache;
    private final Computable<A,V> work;

    /**
     * 
     * @param c
     */
    public Memoizer(Computable<A,V> c)
    {
        cache = new ConcurrentHashMap<A, Future<V>>();
        work  = c;
    }

    /**
     * 
     * @param arg
     * @return
     * @throws InterruptedException
     */
    public V compute(final A arg) throws InterruptedException
    {
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