/*
 * InvokerServletMapping.java
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
package org.j2free.servlet.filter;

import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServlet;
import org.j2free.annotations.ServletConfig;

/**
 * Convenience class for wrapping a servlet and it's
 * configuration.
 * 
 * @author Ryan Wilson
 */
final class InvokerServletMapping {

    protected final HttpServlet servlet;
    protected final ServletConfig config;

    protected final AtomicInteger uses;

    public InvokerServletMapping(HttpServlet servlet, ServletConfig config) {
        this.servlet = servlet;
        this.config  = config;
        this.uses    = new AtomicInteger(0);
    }
    
}
