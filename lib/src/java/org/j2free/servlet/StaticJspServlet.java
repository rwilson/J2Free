/*
 * IndexServlet.java
 *
 * Created on May 5, 2009, 1:50 PM
 */

package org.j2free.servlet;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import static org.j2free.util.ServletUtils.*;
import static org.j2free.util.Constants.*;

import org.j2free.jpa.ControllerServlet;

/**
 * @author Ryan
 * @version
 */
public class StaticJspServlet extends ControllerServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        dispatchRequest(request,response, STATIC_JSP_DIR + request.getRequestURI().replaceFirst("/", EMPTY) + ".jsp");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        doGet(request,response);
    }
}
