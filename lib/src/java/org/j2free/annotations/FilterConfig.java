/*
 * FilterConfig.java
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
package org.j2free.annotations;

import java.lang.annotation.*;

/**
 *
 * @author Ryan Wilson
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterConfig {

    public String mapping();

    /**
     * Whether the Filter expects an open Controller. Default is false.
     */
    public boolean requireController() default false;

    // @TODO implement regex filter maappings (this is going to suck...)
    //public String regex() default "";

    /**
     * Filters are ordered by their "depth", which is the number of "/" they have
     * in their mapping.  The lesser the depth, the earlier in the filter chain a
     * filter is processed.
     *
     * In the event that two Filters have the same depth, the priority field is
     * used as a tie-breaker.  The default of 0 is the max priority, so Filters
     * that do not specify a priority and have a depth conflict will be ordered
     * by the path alphabetically (e.g. "/a/b" will preceed "/b/c" in the filter
     * chain.
     *
     * It is HIGHLY recommended to specify this field!
     */
    public int priority() default 0;
}
