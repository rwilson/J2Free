/*
 * SecureServlet.java
 *
 * Created on December 6, 2008, 3:18 PM
 *
 * Copyright (c) 2008 FooBrew, Inc.
 */

package org.j2free.servlet;

import java.io.IOException;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.j2free.util.ServletUtils.*;
import static org.j2free.util.Constants.*;

/**
 * @author Ryan Wilson 
 */
public class SecureServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(SecureServlet.class);

    public static final AtomicReference<String> path = new AtomicReference(EMPTY);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        
        String uri = request.getRequestURI();

        if (empty(uri)) {
            response.sendRedirect("/");
        } else {
            response.sendRedirect(uri.replaceFirst(path.get(),EMPTY));
        }
        
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        doGet(request,response);
    }
    
}
