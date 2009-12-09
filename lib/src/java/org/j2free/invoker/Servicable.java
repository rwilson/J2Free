/*
 * Servicable.java
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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.j2free.jpa.Controller;

/**
 * Interface for polymorphic associations of servicable mappings.
 * @see {@link FilterMapping}
 * @see {@link ServletMapping}
 *
 * @author Ryan Wilson
 */
interface Servicable {

    /**
     * @return <tt>true</tt> if this <tt>Servicable</tt> has requires 
     *         a {@link Controller} be open when it is <tt>service</tt>
     *         is called, otherwise <tt>false</tt>.
     */
    public boolean requiresController();

    /**
     * @return A name for the <tt>Servicable</tt>
     */
    public String getName();

    /**
     * Business implementation of this <tt>Servicable</tt>
     *
     * @param req A {@link ServletRequest}
     * @param resp A {@link ServletResponse}
     * @param chain A reference to the calling {@link ServiceChain}
     * 
     * @throws IOException
     * @throws ServletException
     */
    public void service(ServletRequest req, ServletResponse resp, ServiceChain chain)
            throws IOException, ServletException;
}
