/*
 * ServletConfig.java
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
package org.j2free.annotations;

import java.lang.annotation.*; 

/**
 * @author Arjun Lall
 * @author Ryan Wilson
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServletConfig {

    /**
     * 
     */
    public static enum SSLOption
    {
        /**
         * Disallow SSL processing by this servlet
         */
        DENY,
        /**
         * Optionally allow SSL processing by this servlet.
         */
        OPTIONAL,
        /**
         * Require SSL connections to this servlet.
         */
        REQUIRE,
        /**
         * Unspecified, delegates to the InvokerFilters runtime config
         */
        UNSPECIFIED
    };

    /**
     * @return The paths for which this configuration should indicate the endpoint.
     */
    public String[] mappings() default
    {};

    /**
     * @return A regex to match against paths.
     */
    public String regex() default "";

    /**
     * @return The SSLOption in effect
     */
    public SSLOption ssl() default SSLOption.UNSPECIFIED;

    /**
     * @return true if the Servlet requires a Controller, otherwise false.
     */
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
     * 
     * @return true if regex or wildcard mappings that resolve to this
     *         endpoint should be cached.  Enabling this allows for
     *         lookups in O(1) time after the first match but uses
     *         more memory.
     */
    public boolean preferDirectLookups() default true;

    /**
     * Overrides the global value of filter.invoker.servlet-max-uses
     * for this servlet only.  Useful for disabling servlet reloading
     * for a particular servlet.
     *
     * @return Indicates how many requests a Servlet instance may serve
     *         before it is discarded and a new instance is constructed.
     *         -1 indicates that the value is unset and the default,
     *         specified by the InvokerFilter config, should be used. To
     *         disable reloading of instances of this Servlet, return 0.
     */
    public int maxUses() default -1;
}
