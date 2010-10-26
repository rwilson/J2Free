package org.j2free.invoker;

import java.util.Enumeration;
import javax.servlet.ServletContext;

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
