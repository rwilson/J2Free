package org.j2free.invoker;

import java.util.Enumeration;
import javax.servlet.ServletContext;

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
