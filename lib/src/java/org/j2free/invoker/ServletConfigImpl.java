/**
 * ServletConfigImpl.java
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
package org.j2free.invoker;

import java.util.Enumeration;
import javax.servlet.ServletContext;

/**
 * @author Ryan Wilson
 */
class ServletConfigImpl implements javax.servlet.ServletConfig
{
    private String servletName;
    private ServletContext context;

    protected ServletConfigImpl(String servletName, ServletContext context)
    {
        this.servletName = servletName;
        this.context = context;
    }

    public String getServletName()
    {
        return servletName;
    }

    public ServletContext getServletContext()
    {
        return context;
    }

    public String getInitParameter(String name)
    {
        return null;
    }

    public Enumeration getInitParameterNames()
    {
        return null;
    }
}
