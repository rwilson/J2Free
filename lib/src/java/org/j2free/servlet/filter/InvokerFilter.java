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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.annotations.URLMapping;
import org.j2free.jpa.Controller;
import org.j2free.jpa.ControllerServlet;
import org.j2free.servlet.AdminGenerator;

import org.j2free.servlet.StaticJspServlet;
import static org.j2free.util.ServletUtils.*;
import static org.j2free.util.Constants.*;

import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

/**
 *
 * @author  Ryan
 */
public class InvokerFilter implements Filter {

    private static final Log log = LogFactory.getLog(InvokerFilter.class);

    private static final String ATTR_STATIC_JSP_DIR    = "static-jsp-dir";
    private static final String ATTR_CONTROLLER_CLASS  = "controller-class";
    private static final String ATTR_KNOWN_STATIC_PATH = "static-path-regex";

    private static class Static extends HttpServlet { }

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

    private boolean benchmarkAccess  = false;
    private String extendedClassName = null;
    private String knownStaticPath   = null;

    // ConcurrentMap holds mappings from urls to classes (specifically)
    private static final ConcurrentMap<String, Class<? extends HttpServlet>> urlMap = 
            new ConcurrentHashMap<String, Class<? extends HttpServlet>>(30000,0.8f,50);

    // A list of regex's to test urls against
    private static final ConcurrentMap<String,Class<? extends HttpServlet>> regexMap =
            new ConcurrentHashMap<String,Class<? extends HttpServlet>>();

    private static final CountDownLatch initialized = new CountDownLatch(1);

    /**
     *
     * @param request The servlet request we are processing
     * @param result The servlet response we are creating
     * @param chain The filter chain we are processing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request   = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String currentPath = request.getRequestURI().replaceFirst(request.getContextPath(), "");

        long start   = System.currentTimeMillis(),  // start time
             find    = 0,                           // time after figuring out what to do
             run     = 0,                           // time after processing
             finish  = 0;                           // finish time

        // Set cache-control based on content
        if (currentPath.matches(".*?\\.(jpg|gif|png|jpeg)") && !currentPath.contains("captcha.jpg")) {
            response.setHeader("Cache-Control", "max-age=3600");
            response.setHeader("Pragma", "cache");
        } else if (currentPath.matches(".*?\\.(swf|js|css|flv)")) {
            response.setHeader("Cache-Control", "max-age=31449600");
            response.setHeader("Pragma", "cache");
        }

        // This will block all threads until the gate opens
        try {
            initialized.await();
        } catch (InterruptedException ie) {
            log.warn("Interrupted while waiting for urlMap to be initialized...");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Get the mapping
        Class klass = urlMap.get(currentPath);
        
        /*
         * Certain extensions are known to be mapped in web.xml,
         * known to never be dynamic resources (e.g. .swf), or
         * were discovered earlier to be static content, so let
         * those through.
         */
        if (klass == Static.class) {

            if (log.isDebugEnabled()) log.debug("Processing known static path: " + currentPath);

            find = System.currentTimeMillis();
            chain.doFilter(req, resp);
            run  = System.currentTimeMillis();
            
        } else if (currentPath.matches(knownStaticPath)) {

            if (log.isDebugEnabled()) log.debug("Processing known static regex-path: " + currentPath);

            // This is the same as the above, except a hash lookup is faster than a regex match, so
            // we want to store a reference.
            urlMap.putIfAbsent(currentPath, Static.class);

            find = System.currentTimeMillis();
            chain.doFilter(req, resp);
            run  = System.currentTimeMillis();
            
        } else {

            log.debug("InvokerFilter for path: " + currentPath);

            // if the exact match wasn't found, look for wildcard matches
            if (klass == null) {

                String partial;

                // If the path contains a "." then check for the *.ext patterns
                if (currentPath.indexOf(".") != -1) {
                    partial = currentPath.substring(currentPath.lastIndexOf("."));
                    klass = urlMap.get(partial);
                }

                // If the path includes a / then it's possible there is a mapping to a /* pattern
                if (klass == null && currentPath.lastIndexOf("/") > 0) {

                    partial = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);

                    // check for possible /*, /something/* patterns starting with most specific
                    // and moving down to /*
                    while (partial.lastIndexOf("/") > 0) {

                        if (log.isDebugEnabled()) log.debug("trying to find wildcard resource " + partial);

                        klass = urlMap.get(partial);

                        // if we found a match, get out of this loop asap
                        if (klass != null)
                            break;

                        // if it's only a slash and it wasn't found, get out asap
                        if (partial.equals("/"))
                            break;

                        // chop the ending '/' off
                        partial = partial.substring(0, partial.length() - 2);

                        // then set the string to be the remnants
                        partial = partial.substring(0, partial.lastIndexOf("/") + 1);
                    }
                }

                // If we still haven't found it, check if any registered regex mappings match the path
                if (klass == null) {

                    String regex;
                    Iterator<String> itr = regexMap.keySet().iterator();
                    while (itr.hasNext()) {
                        
                        regex = itr.next();

                        if (currentPath.matches(regex)) {
                            klass = regexMap.get(regex);

                            // gotta make sure the klass was still in there, since it could have been altered
                            // since we got the iterator... (although, likely it wasn't)
                            if (klass != null)
                                break;
                        }
                    }
                }

                // If we found a class in any way, register it with the currentPath for faster future lookups
                if (klass != null) {
                    urlMap.putIfAbsent(currentPath, klass);
                }
            }

            // If we didn't find it, then just pass it on
            if (klass == null) {

                if (log.isDebugEnabled()) log.debug("Dynamic resource not found for path: " + currentPath);

                // Save this path in the staticSet so we don't have to look it up next time
                urlMap.putIfAbsent(currentPath,Static.class);

                find = System.currentTimeMillis();
                chain.doFilter(req, resp);
                run  = System.currentTimeMillis();

            } else {

                Controller controller = null;

                try {

                    if (klass.getSuperclass() == ControllerServlet.class) {

                        if (extendedClassName == null) {
                            controller = new Controller();
                        } else {
                            controller = (Controller) (Class.forName(extendedClassName).newInstance());
                        }
                        
                        if (log.isDebugEnabled()) log.debug("Dynamic resource found, instance of ControllerServlet: " + klass.getName());

                        ControllerServlet servlet = (ControllerServlet) klass.newInstance();

                        find = System.currentTimeMillis();

                        controller.startTransaction();

                        servlet.setController(controller);
                        request.setAttribute("controller", controller);
                        servlet.service(request, response);

                        run  = System.currentTimeMillis();

                        controller.endTransaction();

                        if (request.getParameter("benchmark") != null) {
                            log.info(klass.getName() + " execution time: " + (System.currentTimeMillis() - start));
                        }

                    } else {

                        if (log.isDebugEnabled()) log.debug("Dynamic resource found, instance of HttpServlet, servicing with " + klass.getName());

                        HttpServlet servlet = (HttpServlet) klass.newInstance();

                        find = System.currentTimeMillis();
                        servlet.service(request, response);
                        run  = System.currentTimeMillis();

                    }

                } catch (Exception e) {

                    run = System.currentTimeMillis();

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
                        log.error("Error servicing " + currentPath, e);
                    }
                }
            }
        }

        finish = System.currentTimeMillis();

        if (benchmarkAccess) log.info(start + "\t" + (find - start) + "\t" + (run - find) + "\t" + (finish - run) + "\t" + currentPath);
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

        log.debug("Initializing InvokerFilter...");

        this.filterConfig = filterConfig;

        // This is the location of jsps that will be mapped to StaticJspServlet
        String staticJspDir = filterConfig.getInitParameter(ATTR_STATIC_JSP_DIR);
        if (!empty(staticJspDir)) {
            STATIC_JSP_DIR = staticJspDir;
        }

        // In case the user has let us know about paths that are guaranteed static
        knownStaticPath = filterConfig.getInitParameter(ATTR_KNOWN_STATIC_PATH);
        if (empty(knownStaticPath))
            knownStaticPath = DEFAULT_KNOWN_STATIC_PATH;

        // In case the application using J2Free has extended Controller...
        extendedClassName= filterConfig.getInitParameter(ATTR_CONTROLLER_CLASS);

        // Can enable benchmarking globally
        String temp = filterConfig.getInitParameter("benchmark-access");
        benchmarkAccess = temp != null && (temp.equals("true") || temp.equals("1"));

        // Do this last because mo request can go through until this runs, so it acts
        // like a semi-synchronization on extendedClassName, knownStaticPath, and
        // benchmarkAccess, which would otherwise need to be accesses synchronously above
        initURLMappings();
    }

    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString() {

        if (filterConfig == null) {
            return ("InvokerFilter()");
        }
        StringBuffer sb = new StringBuffer("InvokerFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());

    }

    /**
     *  @description Finds all classes annotated with URLMapping and maps the class to 
     *               the url specified in the annotation.  Wildcard urls are allowed in
     *               the form of *.extension or /some/path/*
     */
    private void initURLMappings() {

        // (1) Look for any classes annotated with @URLMapping
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
                            Class<? extends HttpServlet> klass = (Class<? extends HttpServlet>)Class.forName(c);
                            if (klass.isAnnotationPresent(URLMapping.class)) {
                                URLMapping anno = (URLMapping) klass.getAnnotation(URLMapping.class);

                                if (anno.urls() != null) {
                                    for (String url : anno.urls()) {
                                        if (url.matches("(^\\*[^*]*?)|([^*]*?/\\*$)")) {
                                            url = url.replaceAll("\\*", "");
                                            log.debug("Mapping servlet " + klass.getName() + " to path " + url);
                                            urlMap.putIfAbsent(url, klass);
                                        } else {
                                            urlMap.putIfAbsent(url, klass);
                                            log.debug("Mapping servlet " + klass.getName() + " to path " + url);
                                        }
                                    }
                                }

                                if (!empty(anno.regex())) {
                                    regexMap.putIfAbsent(anno.regex(), klass);
                                    log.debug("Mapping servlet " + klass.getName() + " to regex path " + anno.regex());
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            log.error("Error processing URLMapping",e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error initializing urlMappings",e);
        }

        // (2) Look for any JSPs in the static-jsp directory and map to the StaticJspServlet
        ServletContext context = filterConfig.getServletContext();
        Set<String> staticJsps = context.getResourcePaths(STATIC_JSP_DIR);
        if (staticJsps != null && !staticJsps.isEmpty()) {
            for (String jsp : staticJsps) {
                jsp = STATIC_JSP_CONTEXT_PATH + jsp.replace(STATIC_JSP_DIR, EMPTY).replaceAll("\\.jsp$", EMPTY);
                urlMap.putIfAbsent(jsp, StaticJspServlet.class);
                log.debug("Mapping StaticJspServlet.class to path " + jsp);
            }
        }

        // Open the gate
        initialized.countDown();
    }

    public static void addServletMapping(String path, Class<? extends HttpServlet> klass) {
        urlMap.putIfAbsent(path, klass);
    }
}
