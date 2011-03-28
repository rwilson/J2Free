/**
 * FilterConfigImpl.java
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
class FilterConfigImpl implements javax.servlet.FilterConfig
{
    private String filterName;
    private ServletContext context;

    protected FilterConfigImpl(String filterName, ServletContext context)
    {
        this.filterName = filterName;
        this.context = context;
    }

    public String getFilterName()
    {
        return filterName;
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
