/*
 * ServletConfig.java
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
 * @author Arjun
 * @author Ryan
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServletConfig {

    public static enum SSLOption {
        DENY,       // Disallow SSL processing by this servlet
        OPTIONAL,   // Optionally allow SSL processing by this servlet
        REQUIRE     // Require SSL connections to this servlet
    };

    public String[] mappings() default {};

    public String regex() default "";

    public SSLOption ssl() default SSLOption.DENY;

    public boolean requireController() default true;

    /**
     * If true, ServletConfigs that use regex or wildcard mappings
     * will only resolve each possible variation once, after which
     * a direct mapping to the resolved servlet will be stored,
     * enabling O(1) lookups on future requests to the same path.
     *
     * It is not recommended that Servlets modify this value but,
     * if memory is a concern, it could be disabled for servlets
     * that match to a large number of URLs.
     */
    public boolean preferDirectLookups() default true;

    /**
     * Overrides the global value of filter.invoker.servlet-max-uses
     * for this servlet only.  Useful for disabling servlet reloading
     * for a particular servlet.
     */
    public int maxUses() default -1;
}
