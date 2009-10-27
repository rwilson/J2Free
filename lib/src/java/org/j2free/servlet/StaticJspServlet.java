/*
 * IndexServlet.java
 *
 * Created on May 5, 2009, 1:50 PM
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
import org.j2free.annotations.ServletConfig.ControllerOption;

import static org.j2free.util.ServletUtils.*;
import static org.j2free.util.Constants.*;

/**
 * @author Ryan
 * @version
 */
@ServletConfig(
    ssl        = SSLOption.OPTIONAL,
    controller = ControllerOption.REQUIRE_OPEN
)
public class StaticJspServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(StaticJspServlet.class);

    public static final AtomicReference<String> directory = new AtomicReference<String>(EMPTY);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        dispatchRequest(request,response, directory.get() + request.getRequestURI().replaceFirst("/", EMPTY) + ".jsp");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        doGet(request,response);
    }
}
