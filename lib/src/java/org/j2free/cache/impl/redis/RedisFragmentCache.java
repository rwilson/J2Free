/**
 * RedisFragmentCache.java
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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import org.j2free.cache.Fragment;
import org.j2free.cache.FragmentCache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 *
 * @author Ryan Wilson
 */
public final class RedisFragmentCache implements FragmentCache<RedisFragment>
{
    private static final String PROP_REDIS_HOST = Properties.ENGINE_PREFIX + "redis.host";

    private final Log log = LogFactory.getLog(getClass());

    /**
     * Using a JedisPool because Jedis is not thread-safe.
     */
    private final JedisPool pool;

    public RedisFragmentCache(Configuration config)
    {
        this(config.getString(PROP_REDIS_HOST, "localhost"));
    }

    public RedisFragmentCache(String redisHost)
    {
        // Configure a JedisPool for redis access
        GenericObjectPool.Config redisPoolConfig = new GenericObjectPool.Config();
        redisPoolConfig.testOnBorrow = true;
        redisPoolConfig.testWhileIdle = true;
        redisPoolConfig.minEvictableIdleTimeMillis = 60000;    // every connection is elligible for eviction at 60s
        redisPoolConfig.timeBetweenEvictionRunsMillis = 30000; // less than 300s, which is when redis closes connections
        redisPoolConfig.numTestsPerEvictionRun = -1;           // examine all objects each run

        pool = new JedisPool(redisPoolConfig, redisHost);
    }

    /**
     * Performs any resource cleanup.
     */
    public void destroy()
    {
        pool.destroy();
    }

    /**
     * Tests if the specified key is in this cache.
     *
     * @param cacheKey possible key
     * @return true if and only if the specified key is in this cache, otherwise false.
     */
    public boolean contains(String cacheKey)
    {
        Jedis jedis = null;
        boolean broken = false;
        try
        {
            jedis = pool.getResource();
            return jedis.exists(getHashKey(cacheKey));
        }
        catch (JedisException e)
        {
            log.error("Error getting fragment for [key="+cacheKey+"]", e);
            broken = true;
        }
        finally
        {
            if (jedis != null)  // make sure to free resources!
            {
                if (broken)
                    pool.returnBrokenResource(jedis);
                else
                    pool.returnResource(jedis);
            }
        }
        return false;
    }

    /**
     * Gets the {@link Fragment} stored under the argument key if one exists.
     *
     * @param key The key of a {@link Fragment} to get
     * @param condition An optional condition upon creation of the {@link Fragment};
     *        if the condition supplied to tryAcquireLock does not match this
     *        condition, then the cache considers itself in need of update.
     * @param timeout The timeout for this cached {@link Fragment}
     *
     * @return The {@link Fragment} stored via the argument key otherwise null.
     */
    public RedisFragment getOrCreate(String key, String condition, long timeout)
    {
        return new RedisFragment(pool);
    }

    /**
     * @return statistics about the current state of the cache, or historical data
     *         if the implementation supports that.
     */
    public RedisFragmentCacheStatistics getStatistics()
    {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    /**
     * @return The number of fragments currently in the cache.
     */
    public int size()
    {
        throw new UnsupportedOperationException("Method not yet implemented.");
    }

    // ----------------------------------------------------------- //
    //    Private Helper Methods Specific to RedisFragmentCache    //
    // ----------------------------------------------------------- //

    private static final String HASH_KEY_PREFIX = "fragment:";

    private String getHashKey(String cacheKey) {
        return HASH_KEY_PREFIX + cacheKey;
    }
}
