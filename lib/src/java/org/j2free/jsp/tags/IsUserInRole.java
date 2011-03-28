/*
 * IsUserInRole.java
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