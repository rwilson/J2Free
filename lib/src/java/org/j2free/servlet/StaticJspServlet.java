/*
 * IndexServlet.java
 *
 * Created on May 5, 2009, 1:50 PM
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
package org.j2free.servlet;

import java.io.IOException;

import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.annotations.ServletConfig;
import org.j2free.annotations.ServletConfig.SSLOption;

import static org.j2free.util.ServletUtils.*;
import static org.j2free.util.Constants.*;

/**
 * @author Ryan Wilson
 * @version
 */
@ServletConfig(
    ssl = SSLOption.OPTIONAL,
    requireController = true
)
public class StaticJspServlet extends HttpServlet {

    /**
     *
     */
    public static final AtomicReference<String> directory = new AtomicReference<String>(EMPTY);

    /**
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        dispatchRequest(request,response, directory.get() + request.getRequestURI().replaceFirst(request.getContextPath(), EMPTY) + ".jsp");
    }
    
    /**
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        doGet(request,response);
    }
}
