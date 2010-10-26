/*
 * ProxyServlet.java
 *
 * Created on June 18, 2008
 */
package org.j2free.servlet;

import java.io.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.annotations.ServletConfig;

import org.j2free.http.HttpCallResult;
import org.j2free.http.HttpCallTask;
import org.j2free.http.SimpleHttpService;

/**
 * @author Ryan Wilson
 * @version 1.0
 */
@ServletConfig(
    requireController = false
)
public class ProxyServlet extends HttpServlet {

    private Log log = LogFactory.getLog(ProxyServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getServletPath().replaceFirst("/proxy","");
        
        if (pathInfo == null) {
            log.warn("Received request to proxy but pathInfo null!");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        String[] path = pathInfo.split("/");
        if (path.length == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String fetchUrl = "http://" + path[1];
        log.debug("Proxying " + fetchUrl);

        /*
        URL url = new URL(fetchUrl);
        URLConnection cxn = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(cxn.getInputStream()));

        String buff = "";

        String line = null;
        while ((line = in.readLine()) != null) {
            buff += line;
        }
        in.close();

        // this packs it down by removing new lines and extra whitespace
        buff = buff.replaceAll("\n", " ").replaceAll("\\s{2,}", " ");
         */

        HttpCallTask task = new HttpCallTask(fetchUrl);
        Future<HttpCallResult> future = SimpleHttpService.submit(task);
        HttpCallResult result;

        try {
            result = future.get();
        } catch (InterruptedException ie) {
            response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            return;
        } catch (ExecutionException ee) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        PrintWriter out = response.getWriter();
        out.print(result.getResponse());
        out.flush();
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
