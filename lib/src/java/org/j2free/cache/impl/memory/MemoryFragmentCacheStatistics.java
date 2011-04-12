/*
 * MemoryFragmentCacheStatistics.java
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

import net.jcip.annotations.Immutable;
import org.j2free.cache.FragmentCacheStatistics;

/**
 *
 * @author Ryan Wilson
 */
@Immutable
public class MemoryFragmentCacheStatistics implements FragmentCacheStatistics {

    private final int size;
    private final long timestamp;

    private final int  lastCleanCount;
    private final long lastCleanTimestamp;

    /**
     * 
     * @param size
     * @param lastCleanCount
     * @param lastCleanTimestamp
     */
    public MemoryFragmentCacheStatistics(int size, int lastCleanCount, long lastCleanTimestamp)
    {
        this.lastCleanCount = lastCleanCount;
        this.lastCleanTimestamp = lastCleanTimestamp;
        this.size = size;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 
     * @return
     */
    public int getCacheSize()
    {
        return size;
    }

    /**
     * 
     * @return
     */
    public long getTimestamp()
    {
        return timestamp;
    }

    /**
     * 
     * @return
     */
    public int getLastCleanCount()
    {
        return lastCleanCount;
    }

    /**
     * 
     * @return
     */
    public long getLastCleanTimestamp()
    {
        return lastCleanTimestamp;
    }
    
}
