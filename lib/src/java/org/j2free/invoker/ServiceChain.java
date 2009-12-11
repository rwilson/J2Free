/*
 * ServiceChain.java
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


import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.j2free.jpa.Controller;

/**
 *
 * @author Ryan Wilson
 */
final class ServiceChain {

    private final Log log = LogFactory.getLog(getClass());

    private final Iterator<FilterMapping> mappings;
    private final String path;
    private final ServletMapping endPoint;

    public ServiceChain(Iterator<FilterMapping> filters, String path, ServletMapping endPoint) {
        this.mappings = filters;
        this.path     = path;
        this.endPoint = endPoint;
    }

    /**
     * Equivalent to:
     * <pre>
     *      chain.service(request, response, null);
     * </pre>
     *
     * @param request a Servlet request
     * @param response a Servlet response
     *
     * @throws IOException
     * @throws ServletException
     */
    public void service(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.trace("ServiceChain [path=" + path +", endPoint=" + endPoint.getName() + "]");
        }
        service(request, response, null);
    }

    /**
     * Recursively processes any filters until arriving at, and processing
     * the end-point.
     *
     * @param request a Servlet request
     * @param response a Servlet response
     * @param mapping The last FilterMaping to be processes, may be null to start
     *
     * @throws IOException
     * @throws ServletException
     */
    protected void service(ServletRequest request, ServletResponse response, FilterMapping last)
            throws IOException, ServletException {
        
        // Try to get the next filter on the current depth (we're overwriting
        // the param, since we only need it in the call to mappings.ceiting(mapping).
        FilterMapping next = mappings.hasNext() ? mappings.next() : null;

        // If we found one, skip past any that don't apply to this path
        while (next != null && !next.appliesTo(path)) {
            if (log.isTraceEnabled()) {
                log.trace("Skipping filter [name=" + next.getName() + ", path=" + path + "]");
            }
            next = mappings.hasNext() ? mappings.next() : null;
        }

        // Set the link to be either the found filter or the endPoint,
        // if there were no more filters remaining, since the two are
        // polymorphically referencable as a Servicable.
        final Servicable link = next == null ? endPoint : next;

        boolean release = false;                            // Holds whether we need to release the Controller here

        try {

            if (link.requiresController()) {                    // If the next Servicable requires a Controller

                log.trace("Next link requires Controller");

                Controller controller = Controller.get(false);  // Try to get an existing one

                if (controller == null) {                       // If there wasn't one already
                    log.trace("No Controller associated with Thread, creating...");
                    controller = Controller.get();              // Create a new one
                    release = true;                             // And take responsibility for closing it
                }

                // If the Servicable requires a Controller and we don't have one,
                // then blow up loudly because all resources futher down the chain
                // will be expecting a Controller and we can't provide that.
                if (controller == null)
                    throw new ServletException("Error provied required Controller to " + link.getName());
                else
                    request.setAttribute(Controller.ATTRIBUTE_KEY, controller); // But if we got it, set it as a req attribute
            }

            if (log.isTraceEnabled()) {
                log.trace("Servicing link [name=" + link.getName() +"]");
            }
            link.service(request, response, this);

        } catch (Exception e) {
            throw new ServletException("Error servicing chain", e); // Wrap any exceptions as ServletException
        } finally {
            // If this Servicable has the responsibility to release a Controller
            // Don't need to check requiresController() again, since release can ONLY
            // be true if requiresController() was true.
            if (release) {
                log.trace("Releasing Controller");
                Controller.release();
            }
        }
    }
}
