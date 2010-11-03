/*
 * IsUserInRole.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.jsp.tags;

import javax.servlet.http.HttpServletRequest;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * @author Ryan Wilson
 */
public class IsUserInRole extends BodyTagSupport
{
    private String role;

    public void setRole(String role)
    {
        this.role = role;
    }

    public IsUserInRole()
    {
        super();
        init();
    }

    public final void init()
    {
        this.role = null;
    }

    @Override
    public void release()
    {
        super.release();
        init();
    }

    @Override
    public int doStartTag() throws JspException
    {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

        // This is a little convoluted, but Glassfish was occasionally throwing NPEs in
        // the security manager on calls to isUserInRole while undeploying.  This should
        // catch and ignore those.
        int ret = SKIP_BODY;
        try {
            if (request != null && request.isUserInRole(role)) ret = EVAL_BODY_INCLUDE;
        } catch (Exception e) { }
        return ret;
    }

    @Override
    public int doEndTag() throws JspException
    {
        return EVAL_PAGE;
    }

}