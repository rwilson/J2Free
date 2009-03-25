/*
 * RequireLoginServlet.java
 *
 * Created on December 6, 2008, 3:18 PM
 *
 * Copyright (c) 2008 FooBrew, Inc.
 */

package org.j2free.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.j2free.annotations.URLMapping;

import static org.j2free.etc.ServletUtils.*;

/**
 * @author Ryan Wilson (http://blog.augmentedfragments.com)
 */
@URLMapping(urls = {
    "/secure/*"
})
public class RequireLoginServlet extends HttpServlet {
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        
        if (empty(uri)) {
            response.sendRedirect("/");
        } else {
            response.sendRedirect(uri.replaceFirst("secure/",""));
        }
        
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        doGet(request,response);
    }
    
}
