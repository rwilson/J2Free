/*
 * LogoutServlet.java
 *
 * Created on June 13, 2008, 1:31 PM
 */

package org.j2free.servlet;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * @author Ryan Wilson
 * @version
 */
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        request.getSession(true).invalidate();
        response.sendRedirect("/");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        doGet(request,response);
    }
    
}
