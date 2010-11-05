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

/**
 *
 * @author Ryan Wilson
 */
final class FilterMapping implements Servicable, Comparable<FilterMapping>
{
    protected final Filter filter;
    
    protected final String match;
    protected final String exclude;
    
    private final boolean useExclude;
    
    private final boolean reqCont;
    
    private final int depth;
    private final int priority;

    protected FilterMapping(Filter filter, FilterConfig config)
    {
        this(filter, config.match(), config.exclude(), config.requireController(), config.priority());
    }

    protected FilterMapping(Filter filter, String match, String exclude, boolean requireController, int priority)
    {
        this.filter = filter;

        this.match = match.trim();
        this.exclude = exclude.trim();
        this.useExclude = this.exclude != null && !"".equals(exclude);
        
        this.reqCont = requireController;
        this.depth = this.match.split("/").length;
        this.priority = priority;
    }

    /**
     * Orders FilterMappings their "depth" first, and by their priority second.
     */
    public int compareTo(FilterMapping o)
    {
        if (this.depth < o.depth)
            return -1;
        else if (this.depth > o.depth)
            return 1;
        else if (this.priority < o.priority)
            return -1;
        else if (this.priority > o.priority)
            return 1;
        else
            return 0;
    }

    /**
     * @param path A URI
     * @return <tt>true</tt> if the associated <tt>Filter</tt> should
     *         be run for the specified URI, otherwise <tt>false</tt>
     */
    public boolean appliesTo(String uri)
    {
        if (useExclude && uri.matches(exclude))
            return false;
        else
            return uri.matches(this.match);
    }

    /**
     * @return <tt>true</tt> if the associated <tt>Filter</tt> has
     *         requested a {@link Controller} be present when it is
     *         called, otherwise <tt>false</tt>.
     */
    public boolean requiresController()
    {
        return reqCont;
    }

    /**
     * @return A name for this FilterMapping
     */
    public String getName()
    {
        return filter.getClass().getName();
    }

    /**
     * Implementation of Filter servicing
     * @see {@link Servicable}.service
     */
    public void service(ServletRequest req, ServletResponse resp, ServiceChain chain)
        throws IOException, ServletException
    {
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
    private FilterChain wrapChain(final ServiceChain chain)
    {
        return new FilterChain() {
                    public void doFilter(final ServletRequest req, final ServletResponse resp)
                        throws IOException, ServletException
                    {
                        chain.service(req, resp);
                    }
                };
    }
}
