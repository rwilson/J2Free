/*
 * AlphaNumericSequencer.java
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

import java.util.concurrent.atomic.AtomicLong;
import net.jcip.annotations.ThreadSafe;

/**
 * A sequencer returning a String consisting of a-z, A-Z, 0-9
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public class AlphaNumericSequencer
{
    private final AtomicLong number;

    public AlphaNumericSequencer()
    {
        this(0l);
    }

    public AlphaNumericSequencer(long seed)
    {
        number = new AtomicLong(seed);
    }

    public String next()
    {
        return Long.toString(number.incrementAndGet(), Character.MAX_RADIX);
    }
}
