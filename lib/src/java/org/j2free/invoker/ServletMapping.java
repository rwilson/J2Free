/*
 * ServletMapping.java
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

import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.j2free.annotations.ServletConfig;

/**
 *
 * @author Ryan Wilson
 */
final class ServletMapping implements Servicable {

    protected final HttpServlet servlet;
    protected final ServletConfig config;
    
    private final AtomicInteger uses;

    protected ServletMapping(HttpServlet servlet, ServletConfig config) {
        this.servlet = servlet;
        this.config = config;
        
        this.uses = new AtomicInteger(0);
    }

    public int incrementUses() {
        return uses.incrementAndGet();
    }

    /**
     * @return <tt>true</tt> if the associated <tt>Servlet</tt> has
     *         requested a {@link Controller} be present when it is
     *         called, otherwise <tt>false</tt>.
     */
    public boolean requiresController() {
        return config.requireController();
    }

    /**
     * @return A name for this ServletMapping
     */
    public String getName() {
        return servlet.getClass().getName();
    }

    /**
     * Implementation of HttpServlet servicing.
     * @see {@link Servicable}.service
     */
    public void service(ServletRequest req, ServletResponse resp, ServiceChain chain) 
            throws IOException, ServletException {
        servlet.service(req, resp);
    }

}
