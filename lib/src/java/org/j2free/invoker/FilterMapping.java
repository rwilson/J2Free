/*
 * FilterMapping.java
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
package org.j2free.invoker;

import java.io.IOException;
import javax.servlet.Filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.j2free.annotations.FilterConfig;
import org.j2free.jpa.Controller;
import org.j2free.util.Constants;

/**
 *
 * @author Ryan Wilson
 */
final class FilterMapping implements Servicable, Comparable<FilterMapping> {

    protected final Filter filter;
    protected final String path;
    
    private final boolean reqCont;
    private final int depth;

    // Need a final reference to "this", to reference inside the inline FilterChain below.
    private final FilterMapping self;

    protected FilterMapping(Filter filter, FilterConfig config) {
        this(filter, config.mapping(), config.requireController());
    }

    protected FilterMapping(Filter filter, String path, boolean requireController) {
        this.filter  = filter;
        this.path    = path.replace("*", Constants.EMPTY); // trim the "*" off the end
        this.reqCont = requireController;
        this.depth   = this.path.split("/").length;

        this.self    = this;
    }

    /**
     * Orders FilterMappings their "depth"
     * @param o
     * @return
     */
    public int compareTo(FilterMapping o) {
        if (this.depth < o.depth) {
            return -1;
        } else if (this.depth > o.depth) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * @param path A URI
     * @return <tt>true</tt> if the associated <tt>Filter</tt> should
     *         be run for the specified URI, otherwise <tt>false</tt>
     */
    public boolean appliesTo(String uri) {
        return uri.startsWith(this.path);
    }

    /**
     * @return <tt>true</tt> if the associated <tt>Filter</tt> has
     *         requested a {@link Controller} be present when it is
     *         called, otherwise <tt>false</tt>.
     */
    public boolean requiresController() {
        return reqCont;
    }

    /**
     * @return A name for this FilterMapping
     */
    public String getName() {
        return filter.getClass().getName();
    }

    /**
     * Implementation of Filter servicing
     * @see {@link Servicable}.service
     */
    public void service(ServletRequest req, ServletResponse resp, ServiceChain chain)
            throws IOException, ServletException {
        
        filter.doFilter( req, resp, wrapChain(chain) );
    }

    /**
     * Wraps a call to the <tt>service</tt> method of the specified
     * chain (including this <tt>FilterMapping</tt> in the arguments)
     * in an instance of {@link FilterChain}.
     *
     * @param chain A {@link ServiceChain}
     * @return A {@link FilterChain}
     */
    private FilterChain wrapChain(final ServiceChain chain) {
        return new FilterChain() {
                public void doFilter(final ServletRequest req, final ServletResponse resp)
                        throws IOException, ServletException {
                        chain.service(req, resp, self);
                }
            };
    }
}
