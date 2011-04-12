/*
 * HoptoadContext.java
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
package org.j2free.error;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Ryan Wilson
 */
public class HoptoadContext
{
    /**
     *
     * @param req
     * @return
     */
    public static HoptoadContext parseRequest(HttpServletRequest req)
    {
        if (req == null)
            return null;

        HoptoadContext c = new HoptoadContext();
        c.url         = req.getRequestURL().toString();
        c.component   = req.getRequestURI();
        c.queryParams = req.getParameterMap();

        HttpSession session = req.getSession();
        if (session != null)
        {
            Enumeration attrNames = session.getAttributeNames();
            while (attrNames.hasMoreElements())
            {
                String key = (String)attrNames.nextElement();
                c.sessionAttrs.put(key, session.getAttribute(key));
            }
        }

        c.serverName = req.getServerName() == null ? "null" : req.getServerName();
        c.remoteAddr = req.getRemoteAddr() == null ? "null" : req.getRemoteAddr();
        c.pathInfo   = req.getPathInfo() == null ? "null" : req.getPathInfo();
        c.method     = req.getMethod() == null ? "null" : req.getMethod();
        c.userAgent  = req.getHeader("User-Agent") == null ? "null" : req.getHeader("User-Agent");

        return c;
    }

    private String url;
    private String component;

    private Map<String, String> queryParams;
    private Map<String, Object> sessionAttrs;

    // CGI-DATA
    private String serverName;
    private String remoteAddr;
    private String pathInfo;
    private String method;
    private String userAgent;

    /**
     *
     */
    public HoptoadContext()
    {
        queryParams = new HashMap<String, String>();
        sessionAttrs = new HashMap<String, Object>();
    }

    /**
     * @return the url
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @return the component
     */
    public String getComponent()
    {
        return component;
    }

    /**
     * @param component the component to set
     */
    public void setComponent(String component)
    {
        this.component = component;
    }

    /**
     * @return the queryParams
     */
    public Map getQueryParams()
    {
        return queryParams;
    }

    /**
     * @param queryParams the queryParams to set
     */
    public void setQueryParams(Map queryParams)
    {
        this.queryParams = queryParams;
    }

    /**
     * @return the sessionAttrs
     */
    public Map getSessionAttrs()
    {
        return sessionAttrs;
    }

    /**
     * @param sessionAttrs the sessionAttrs to set
     */
    public void setSessionAttrs(Map sessionAttrs)
    {
        this.sessionAttrs = sessionAttrs;
    }

    /**
     * @return the serverName
     */
    public String getServerName()
    {
        return serverName;
    }

    /**
     * @param serverName the serverName to set
     */
    public void setServerName(String serverName)
    {
        this.serverName = serverName;
    }

    /**
     * @return the remoteAddr
     */
    public String getRemoteAddr()
    {
        return remoteAddr;
    }

    /**
     * @param remoteAddr the remoteAddr to set
     */
    public void setRemoteAddr(String remoteAddr)
    {
        this.remoteAddr = remoteAddr;
    }

    /**
     * @return the pathInfo
     */
    public String getPathInfo()
    {
        return pathInfo;
    }

    /**
     * @param pathInfo the pathInfo to set
     */
    public void setPathInfo(String pathInfo)
    {
        this.pathInfo = pathInfo;
    }

    /**
     * @return the method
     */
    public String getMethod()
    {
        return method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(String method)
    {
        this.method = method;
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent()
    {
        return userAgent;
    }

    /**
     * @param userAgent the userAgent to set
     */
    public void setUserAgent(String userAgent)
    {
        this.userAgent = userAgent;
    }

}
