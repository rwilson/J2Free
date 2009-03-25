/*
 * Logout.java
 *
 * Created on June 13, 2008, 1:31 PM
 */

package org.j2free.servlet;

import java.io.*;
import java.net.*;

import javax.servlet.*;
import javax.servlet.http.*;
import org.j2free.annotations.URLMapping;

/**
 *
 * @author ryan
 * @version
 */
@URLMapping(urls={"/logout"})
public class Logout extends HttpServlet {
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        request.getSession(true).invalidate();
        response.sendRedirect("/");
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        doGet(request,response);
    }
    
}
