/*
 * InvokerFilter.java
 *
 * Created on June 13th, 2008, 12:54 PM
 *
 */
package org.j2free.servlet.filter;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.annotations.URLMapping;
import org.j2free.jpa.Controller;
import org.j2free.jpa.ControllerServlet;
import org.j2free.servlet.AdminGenerator;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

/**
 *
 * @author  Ryan
 */
public class InvokerFilter implements Filter {

    private static Log log = LogFactory.getLog(InvokerFilter.class);

    /* Ignored User-Agents
        panscient.com
        Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)
        nutchsearch/Nutch-0.9 (Nutch Search 1.0; herceg_novi at yahoo dot com)
        Yahoo! Slurp
        Powerset
        Ask Jeeves/Teoma
        msnbot
        Mozilla/5.0 (compatible; MJ12bot/v1.2.1; http://www.majestic12.co.uk/bot.php?+)
        Mozilla/5.0 (Twiceler-0.9 http://www.cuill.com/twiceler/robot.html)
        Gigabot/3.0 (http://www.gigablast.com/spider.html)
        Mozilla/5.0 (compatible; attributor/1.13.2 +http://www.attributor.com)
        lwp-request/2.07
    */
    private static final String IGNORED_AGENTS =
            ".*?(" +
                "(spider|robot|bot)\\.[a-z]*?|" +
                "panscient\\.com|" +
                "Googlebot|" +
                "nutchsearch|" +
                "Yahoo! Slurp|" +
                "powerset|" +
                "Ask Jeeves/Teoma|" +
                "msnbot|" +
                "Twiceler|" +
                "attributor\\.com|" +
            ").*?";

    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured.
    private FilterConfig filterConfig = null;
    private static HashMap<String, Class> urlMap = null;

    public InvokerFilter() {
    }

    /**
     *
     * @param request The servlet request we are processing
     * @param result The servlet response we are creating
     * @param chain The filter chain we are processing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String currentPath = request.getRequestURI().replaceFirst(request.getContextPath(), "");

        long start = System.currentTimeMillis();

        // Set cache-control based on content
        if (currentPath.matches(".*?\\.(jpg|gif|png|jpeg)") && !currentPath.contains("captcha.jpg")) {
            response.setHeader("Cache-Control","max-age=3600");
            response.setHeader("Pragma","cache");
        } else if (currentPath.matches(".*?\\.(swf|js|css)")) {
            response.setHeader("Cache-Control","max-age=31449600");
            response.setHeader("Pragma","cache");
        }
        
        // Certain extensions are known to be mapped in web.xml (e.g. .pack), 
        // or known to never be dynamic resources (e.g. .swf), so let them through
        if (currentPath.matches(".*?\\.swf")) {
            chain.doFilter(req, resp);
            log.info((System.currentTimeMillis() - start) + "ms\t\t" + currentPath);
            return;
        }

        if (urlMap == null) {
            log.error("urlMap is null! Initializing...");
            initAnnotatedURLMappings();
        }

        // Check for static first, since this is a constant look and regex aren't
        Class klass = urlMap.get(currentPath);

        log.debug("InvokerFilter for path: " + currentPath);

        // if the exact match wasn't found, look for wildcard matches
        if (klass == null) {
            String partial;

            // If the path contains a "." then check for the *.ext patterns
            if (currentPath.indexOf(".") != -1) {
                partial = currentPath.substring(currentPath.lastIndexOf("."));
                klass = urlMap.get(partial);
            }

            if (klass == null && currentPath.lastIndexOf("/") > 0) {
                partial = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
                // check for possible /*, /something/* patterns starting with most specific
                // and moving down to /*
                while (partial.lastIndexOf("/") > 0) {
                    //log.debug("trying to find wildcard resource " + partial);

                    klass = urlMap.get(partial);

                    // if we found a match, get out of this loop asap
                    if (klass != null) {
                        // Register the klass with the currentPath for future use.
                        urlMap.put(currentPath, klass);
                        break;
                    }

                    // if it's only a slash and it wasn't found, get out asap
                    if (partial.equals("/")) {
                        break;
                    }

                    // chop the ending '/' off 
                    partial = partial.substring(0, partial.length() - 2);

                    // then set the string to be the remnants
                    partial = partial.substring(0, partial.lastIndexOf("/") + 1);
                }
            }
        }

        ServletException problem = null;

        // If we didn't find it, then just pass it on
        if (klass == null) {

            log.debug("Dynamic resource not found for path: " + currentPath);
            chain.doFilter(req, resp);

        } else {

            Controller controller = null;

            try {

                String extendedClassName = filterConfig.getInitParameter("controllerClass");

                if (extendedClassName == null) {
                    controller = new Controller();
                } else {
                    controller = (Controller) (Class.forName(extendedClassName).newInstance());
                }

                if (klass.getSuperclass() == ControllerServlet.class) {

                    log.debug("Dynamic resource found, instance of ControllerServlet, servicing with " + klass.getName());

                    ControllerServlet servlet = (ControllerServlet) klass.newInstance();

                    long startTime = System.currentTimeMillis();

                    controller.startTransaction();
                    servlet.setController(controller);
                    request.setAttribute("controller", controller);
                    servlet.service(request, response);
                    controller.endTransaction();

                    if (request.getParameter("benchmark") != null) {
                        log.info(klass.getName() + " execution time: " + (System.currentTimeMillis() - startTime));
                    }

                } else {

                    log.debug("Dynamic resource found, instance of HttpServlet, servicing with " + klass.getName());
                    HttpServlet servlet = (HttpServlet) klass.newInstance();
                    servlet.service(request, response);

                }

            } catch (Exception e) {

                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && !userAgent.matches(IGNORED_AGENTS)) {
                    /*
                    Emailer mailer = Emailer.getInstance();
                    String exceptionReport = describeRequest(request) + "\n\nStack Trace:\n" + ServletUtils.throwableToString(e);
                    try {
                        mailer.sendPlain("ryan@foobrew.com,arjun@foobrew.com","Exception in FilterChain " + new Date().toString(),exceptionReport);
                    } catch (Exception e0) {
                        log.fatal("Error sending Exception report email. Content follows:\n\n" + exceptionReport);
                    }
                    */
                    log.error("Error servicing " + currentPath,e);
                    problem = new ServletException(e);
                }
            }
        }

        log.info("\t" + (System.currentTimeMillis() - start) + "\t" + currentPath);

        if (problem != null)
            throw problem;
    }

    /**
     * Return the filter configuration object for this filter.
     */
    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }

    /**
     * Set the filter configuration object for this filter.
     *
     * @param filterConfig The filter configuration object
     */
    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /**
     * Destroy method for this filter
     *
     */
    public void destroy() {
    }

    /**
     * Init method for this filter
     *
     */
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;

        if (urlMap == null) {
            initAnnotatedURLMappings();
        }
    }

    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {

        if (filterConfig == null) {
            return ("UrlRedirectorFilter()");
        }
        StringBuffer sb = new StringBuffer("UrlRedirectorFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());

    }

    /**
     *  @description Finds all classes annotated with URLMapping and maps the class to 
     *               the url specified in the annotation.  Wildcard urls are allowed in
     *               the form of *.extension or /some/path/*
     */
    private void initAnnotatedURLMappings() {
        urlMap = new HashMap<String, Class>();

        try {
            LinkedList<URL> urlList = new LinkedList<URL>();
            urlList.addAll(Arrays.asList(ClasspathUrlFinder.findResourceBases("")));
            urlList.add(ClasspathUrlFinder.findClassBase(AdminGenerator.class));

            URL[] urls = new URL[urlList.size()];
            urls = urlList.toArray(urls);

            AnnotationDB aDB = new AnnotationDB();
            aDB.setScanClassAnnotations(true);
            aDB.setScanFieldAnnotations(false);
            aDB.setScanMethodAnnotations(false);
            aDB.setScanParameterAnnotations(false);
            aDB.scanArchives(urls);

            HashMap<String, Set<String>> annotationIndex = (HashMap<String, Set<String>>) aDB.getAnnotationIndex();
            if (annotationIndex != null && !annotationIndex.isEmpty()) {
                Set<String> classNames = annotationIndex.get("org.j2free.annotations.URLMapping");
                if (classNames != null) {
                    for (String c : classNames) {
                        try {
                            Class klass = Class.forName(c);
                            if (klass.isAnnotationPresent(URLMapping.class)) {
                                URLMapping anno = (URLMapping) klass.getAnnotation(URLMapping.class);

                                if (anno.urls() != null) {
                                    for (String url : anno.urls()) {
                                        if (url.matches("(^\\*[^*]*?)|([^*]*?/\\*$)")) {
                                            url = url.replaceAll("\\*", "");
                                            log.debug("Mapping servlet " + klass.getName() + " to path " + url);
                                            urlMap.put(url, klass);
                                        } else {
                                            urlMap.put(url, klass);
                                            log.debug("Mapping servlet " + klass.getName() + " to path " + url);
                                        }
                                    }
                                }
                            }
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
