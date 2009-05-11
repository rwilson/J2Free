/*
 * IndexServlet.java
 *
 * Created on May 5, 2009, 1:50 PM
 */

package org.j2free.servlet;

import java.io.*;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.j2free.util.ServletUtils.*;
import static org.j2free.util.Constants.*;

import org.j2free.jpa.ControllerServlet;

/**
 * @author Ryan
 * @version
 */
public class StaticJspServlet extends ControllerServlet {

    private static final Log log = LogFactory.getLog(StaticJspServlet.class);

    private final static AtomicReference<String> dir = new AtomicReference<String>(EMPTY);

    @Override
    public void init() throws ServletException {
        Configuration config = (Configuration)getServletContext().getAttribute(CONTEXT_ATTR_CONFIG);
        if (config == null) {
            try {
                config = new PropertiesConfiguration(DEFAULT_CONFIG_PATH);
            } catch (ConfigurationException ce) {
                log.error("Error configuring StaticJspServlet!",ce);
            }
        }
        dir.compareAndSet(EMPTY, config.getString("static-jsp.dir",DEFAULT_STATICJSP_DIR));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        dispatchRequest(request,response, dir.get() + request.getRequestURI().replaceFirst("/", EMPTY) + ".jsp");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        doGet(request,response);
    }
}
