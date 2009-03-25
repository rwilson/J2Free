/*
 * Proxy.java
 *
 * Created on June 18, 2008
 */
package org.j2free.servlet;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.j2free.annotations.URLMapping;

/**
 * @author ryan
 * @version 1.0
 */
@URLMapping(urls = {"/proxy/*"})
public class Proxy extends HttpServlet {

    private static Log log = LogFactory.getLog(Proxy.class);

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

        HttpClient client = new HttpClient();

        GetMethod get = new GetMethod(fetchUrl);

        int httpResult;
        String result;
        try {

            httpResult   = client.executeMethod(get);
            result = get.getResponseBodyAsString();

        } catch (IOException ioe) {
            log.error("Error fetching song and tracks from server",ioe);
            return;
        } finally {
            get.releaseConnection();
        }

        PrintWriter out = response.getWriter();
        out.print(result);
        out.flush();
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
