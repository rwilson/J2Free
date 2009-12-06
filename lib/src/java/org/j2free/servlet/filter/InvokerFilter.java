/*
 * InvokerFilter.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.servlet.filter;

import java.io.*;
import java.net.URL;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.annotations.InitParam;
import org.j2free.annotations.ServletConfig;
import org.j2free.annotations.ServletConfig.SSLOption;

import org.j2free.jpa.Controller;
import org.j2free.util.ServletUtils;
import org.j2free.util.UncaughtServletExceptionHandler;

import static org.j2free.util.ServletUtils.*;
import static org.j2free.util.Constants.*;

import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.scannotation.WarUrlFinder;

/**
 *
 * @author  Ryan Wilson
 */
public class InvokerFilter implements Filter {

    private static final Log log = LogFactory.getLog(InvokerFilter.class);

    // Convenience class for grouping info about a servlet mapping, avoids multiple maps
    private static class ServletMapping {

        private final HttpServlet   servlet;
        private final ServletConfig config;

        public ServletMapping(HttpServlet servlet, ServletConfig config) {
            this.servlet = servlet;
            this.config  = config;
        }
    }

    // Convenience class for setting javax.servlet.ServletConfig
    private static class ServletConfigImpl implements javax.servlet.ServletConfig {

        private final String name;
        private final ServletContext context;

        private final HashMap<String, String> initParams;

        public ServletConfigImpl(String name, ServletContext context, InitParam[] initParams) {
            this.name       = name;
            this.context    = context;

            this.initParams = new HashMap<String, String>();
            for (InitParam param : initParams) {
                this.initParams.put(param.name(), param.value());
            }
        }

        public String getServletName() {
            return name;
        }

        public ServletContext getServletContext() {
            return context;
        }

        public String getInitParameter(String key) {
            return initParams.get(key);
        }

        // UGLY HACK: ServletConfig requires getInitParameterNames return an Enumeration, which
        //            is only implemented by StringTokenizer.  Anticipating that we won't ever
        //            actually call this function, this is probably okay, but it's nasty.
        public Enumeration getInitParameterNames() {
            return new StringTokenizer(ServletUtils.join(initParams.keySet(), ","), ",");
        }

    }

    // Convenience class for referencing static resources
    private static final class Static extends HttpServlet { }

    private static final AtomicBoolean benchmark                     = new AtomicBoolean(false);
    private static final AtomicInteger sslRedirectPort               = new AtomicInteger(-1);
    private static final AtomicInteger nonSslRedirectPort            = new AtomicInteger(-1);

    private static final AtomicReference<String> bypassPath          = new AtomicReference(EMPTY);

    // For accessing the servlet context in static methods
    private static final AtomicReference<ServletContext> servletContext = new AtomicReference(null);

    // For error handling, a handler and redirect location
    private static final AtomicReference<UncaughtServletExceptionHandler> exceptionHandler = new AtomicReference(null);

    // ConcurrentMap holds mappings from URLs to classes (specifically)
    private static final ConcurrentMap<String, Class<? extends HttpServlet>> urlMap
            = new ConcurrentHashMap(10000, 0.8f, 50);

    // A map of regex's to test mappings against
    private static final ConcurrentMap<String, Class<? extends HttpServlet>> regexMap = new ConcurrentHashMap();

    // A map of HttpServlet classes to mappings for that class
    private static final ConcurrentHashMap<Class<? extends HttpServlet>, ServletMapping> servletMap 
            = new ConcurrentHashMap(100, 0.75f, 50);

    // Used to queue requests during (re)configuration.  Fairness is enabled,
    // otherwise writes could wait indefinitely because of the high contention
    // on this filter.
    private static final ReadWriteLock lock  = new ReentrantReadWriteLock(true);
    private static final Lock          read  = lock.readLock();
    private static final Lock          write = lock.writeLock();

    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured.
    private FilterConfig filterConfig = null;

    public InvokerFilter() {
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
        try {

            // Clear the mappings
            urlMap.clear();
            regexMap.clear();

            // Destroy the servlets used in the mappings
            for (ServletMapping mapping : servletMap.values()) {
                mapping.servlet.destroy();
            }

            // Clear the ServletMappings
            servletMap.clear();

        } finally {
            write.unlock();
        }
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
            response.setHeader("Pragma", "cache");
            response.setHeader("Cache-Control", "max-age=21600");
        } else if (currentPath.matches(".*?\\.(swf|js|css|flv)")) {
            response.setHeader("Pragma", "cache");
            response.setHeader("Cache-Control", "max-age=31536000");
        }

        // This will block requests during configuration
        try {
            read.lock();

            // Get the mapping
            Class<? extends HttpServlet> klass = urlMap.get(currentPath);

            /*
             * If we don't already have an exact match for this path,
             * try to break it down.
             *
             * Certain extensions are known to be mapped in web.xml,
             * known to never be dynamic resources (e.g. .swf), or
             * were discovered earlier to be static content, so don't
             * process those.
             */
            if (klass == null && !currentPath.matches(bypassPath.get())) {

                if (log.isTraceEnabled())
                    log.trace("InvokerFilter for path: " + currentPath);

                // (1) If the exact match wasn't found, look for wildcard matches

                String partial;

                // If the path contains a "." then check for the *.ext patterns
                if (currentPath.indexOf(".") != -1) {
                    partial = currentPath.substring(currentPath.lastIndexOf("."));
                    klass = urlMap.get(partial);
                }

                // (2) If we still haven't found it, check if any
                // registered regex mappings against the path
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

                // (3) If we didn't find a .* pattern and if the path includes
                // a / then it's possible there is a mapping to a /* pattern
                if (klass == null && currentPath.lastIndexOf("/") > 0) {

                    partial = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);

                    // check for possible /*, /something/* patterns starting with most specific
                    // and moving down to /*
                    while (partial.lastIndexOf("/") > 0) {

                        if (log.isTraceEnabled()) log.trace("trying to find wildcard resource " + partial);

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

                // (4) If we found a class in any way, register it with the currentPath for faster future lookups
                if (klass != null) {
                    if (log.isTraceEnabled())
                        log.trace("Matched path " + currentPath + " to " + klass.getName());

                    urlMap.putIfAbsent(currentPath, klass);
                }
            }

            // If we didn't find it, then just pass it on
            if (klass == null) {

                if (log.isTraceEnabled()) log.trace("Dynamic resource not found for path: " + currentPath);

                // Save this path in the staticSet so we don't have to look it up next time
                urlMap.putIfAbsent(currentPath, Static.class);

                find = System.currentTimeMillis();
                chain.doFilter(req, resp);
                run  = System.currentTimeMillis();

            } else if (klass == Static.class) {

                // If it's known to be static, then pass it on
                if (log.isTraceEnabled())
                    log.trace("Processing known static path: " + currentPath);

                find = System.currentTimeMillis();
                chain.doFilter(req, resp);
                run  = System.currentTimeMillis();

            } else {

                ServletMapping mapping = servletMap.get(klass);
                ServletConfig  config  = mapping.config;

                // If the klass requires SSL, make sure we're on an SSL connection
                SSLOption sslOpt = config.ssl();
                boolean isSsl    = request.isSecure();
                if (sslOpt == SSLOption.REQUIRE && !isSsl) {
                    log.debug("Redirecting over SSL: " + currentPath);
                    redirectOverSSL(request, response, sslRedirectPort.get());
                    return;
                } else if (sslOpt == SSLOption.DENY && isSsl) {
                    log.debug("Redirecting off SSL: " + currentPath);
                    redirectOverNonSSL(request, response, nonSslRedirectPort.get());
                    return;
                }

                try {

                    if (log.isTraceEnabled())
                        log.trace("Dynamic resource found, servicing with " + klass.getName());

                    // Get the time after finding the resource
                    find = System.currentTimeMillis();

                    Controller controller;

                    // Check the controller requirements of the configuration
                    switch (config.controller()) {

                        /*********************************************************/
                        // NONE: The servlet will manage it's own Controller, if any
                        case NONE:
                            mapping.servlet.service(request, response);
                            break;

                        /*********************************************************/
                        // REQUIRE: The servlet wants a Controller associated with the thread and request,
                        //          but does not want the transaction to be open
                        case REQUIRE:
                            controller = Controller.get(true, false);
                            request.setAttribute(Controller.ATTRIBUTE_KEY, controller);

                            try {
                                mapping.servlet.service(request, response);     // Service the request
                            } finally {
                                Controller.release(controller);                 // Since this filter opened it, make sure to release it
                            }
                            break;

                        /*********************************************************/
                        // REQUIRE_OPEN: The servlet wants a Controller associated with the thread and request,
                        //               with an open trannsaction.
                        case REQUIRE_OPEN:
                            controller = Controller.get();
                            request.setAttribute(Controller.ATTRIBUTE_KEY, controller);

                            try {
                                mapping.servlet.service(request, response);     // Service the request
                            } finally {
                                Controller.release(controller);                 // Since this filter opened it, make sure to release it
                            }
                            break;
                    }

                    // Get the time after running
                    run  = System.currentTimeMillis();

                    if (request.getParameter("benchmark") != null) {
                        log.info(klass.getName() + " execution time: " + (System.currentTimeMillis() - start));
                    }

                } catch (Exception e) {
                    run = System.currentTimeMillis();
                    if (exceptionHandler.get() != null) {
                        exceptionHandler.get().handleException(req, resp, e);
                    } else {
                        throw new ServletException(e);
                    }
                }
            }

            finish = System.currentTimeMillis();

            if (benchmark.get()) {
                log.info(start + "\t" + (find - start) + "\t" + (run - find) + "\t" + (finish - run) + "\t" + currentPath);
            }
            
        } finally {
            read.unlock(); // Make sure to release the read lock
        }
    }

    /**
     * This is a hack to create a servlet config to
     * properly initialize the servlet.
     */
    public javax.servlet.ServletConfig getServletConfig(Class<? extends HttpServlet> klass) {

        final String className = klass.getSimpleName();
        
        return new javax.servlet.ServletConfig() {

            public String getServletName() {
                return className;
            }

            public ServletContext getServletContext() {
                return filterConfig.getServletContext();
            }

            public String getInitParameter(String arg0) {
                return null;
            }

            public Enumeration getInitParameterNames() {
                return null;
            }
        };
    }

    public void init(FilterConfig filterConfig) {
        
        this.filterConfig = filterConfig;
        log.debug("Enabling InvokerFilter...");

        servletContext.set(filterConfig.getServletContext());
        load();
    }

    /**
     * Locks to prevent request processing while mapping is added.
     *
     * Finds all classes annotated with ServletConfig and maps the class to
     * the url specified in the annotation.  Wildcard mappings are allowed in
     * the form of *.extension or /some/path/*
     */
    private static void load() {
        
        try {
            write.lock();

            LinkedList<URL> urlList = new LinkedList<URL>();
            urlList.addAll(Arrays.asList(ClasspathUrlFinder.findResourceBases("")));
            urlList.addAll(Arrays.asList(WarUrlFinder.findWebInfLibClasspaths(servletContext.get())));
            
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
                
                // Look for any classes annotated with @ServletConfig
                Set<String> classNames = annotationIndex.get(ServletConfig.class.getName());
                
                if (classNames != null) {
                    for (String c : classNames) {
                        try {
                            
                            Class<? extends HttpServlet> klass = (Class<? extends HttpServlet>)Class.forName(c);
                            
                            if (klass.isAnnotationPresent(ServletConfig.class)) {

                                ServletConfig config = (ServletConfig)klass.getAnnotation(ServletConfig.class);

                                // If the config specifies String mapppings...
                                if (config.mappings() != null) {
                                    for (String url : config.mappings()) {

                                        if (url.matches("(^\\*[^*]*?)|([^*]*?/\\*$)"))
                                            url = url.replaceAll("\\*", "");

                                        if (urlMap.putIfAbsent(url, klass) == null)
                                            log.debug("Mapping servlet " + klass.getName() + " to path " + url);
                                        else
                                            log.error("Unable to map servlet  " + klass.getName() + " to path " + url + ", path already mapped to " + urlMap.get(url).getName());

                                    }
                                }

                                // If the config specifies a regex mapping...
                                if (!empty(config.regex())) {
                                    regexMap.putIfAbsent(config.regex(), klass);
                                    log.debug("Mapping servlet " + klass.getName() + " to regex path " + config.regex());
                                }

                                // Create an instance of the servlet and init it
                                HttpServlet servlet = klass.newInstance();
                                servlet.init( new ServletConfigImpl(klass.getName(), servletContext.get(), config.initParams()) );

                                // Store a reference
                                servletMap.put(klass, new ServletMapping(servlet, config));
                            }

                        } catch (Exception e) {
                            log.error("Error registering servlet [name=" + c + "]", e);
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            log.error("Error loading urlMappings", e);
        } finally {
            write.unlock(); // ALWAYS Release the configure lock
        }
    }

    /**
     * Clears loaded configuration, used for dynamic reconfiguation.
     * Locks to prevent request processing while modifications are made.
     */
    public static void reload() {

        try {
            write.lock();

            // Clear the mappings
            urlMap.clear();
            regexMap.clear();

            // Destroy the servlets used in the mappings
            for (ServletMapping mapping : servletMap.values()) {
                mapping.servlet.destroy();
            }

            // Clear the ServletMappings
            servletMap.clear();

            load(); // Load it all up again

        } finally {
            write.unlock(); // ALWAYS unlock
        }
    }

    /**
     * Locks to prevent request processing while mapping is added.
     *
     * @param path The path to map a Servlet to
     * @param klass The Servlet to map
     * @return null if the servlet was mapped to the path, or a class if another servlet was already mapped to that path
     */
    public static Class<? extends HttpServlet> addServletMapping(String path, Class<? extends HttpServlet> klass) {

        try {
            write.lock();

            if (servletMap.get(klass) == null)
                throw new IllegalStateException("Illegal attempt to create a mapping to an unregistered servlet!");

            log.debug("Mapping servlet " + klass.getName() + " to path " + path);

            if (path.matches("(^\\*[^*]*?)|([^*]*?/\\*$)"))
                path = path.replaceAll("\\*","");

            return urlMap.putIfAbsent(path, klass);

        } finally {
            write.unlock(); // ALWAYS unlock
        }
    }

    /**
     * Configures the InvokerFilter, locking to prevent request processing
     * while configuration is set.
     * 
     * @param bypass A regex String used to specify known static paths (optional, but recommend for optimization purposes)
     * @param doBenchmark If true, each request will be benchmarked
     * @param nonSslPort Port to be used for non-SSL requests
     * @param sslPort Port to be used for SSL-requests
     */
    public static void configure(String bypass, boolean doBenchmark, Integer nonSslPort, Integer sslPort) {

        try {
            write.lock();

            // In case the user has let us know about paths that are guaranteed static
            bypassPath.set(bypass);

            // Can enable benchmarking globally
            benchmark.set(doBenchmark);

            // Set the SSL redirect port
            if (sslPort != null && sslPort > 0)
                sslRedirectPort.set(sslPort);

            // Set the SSL redirect port
            if (nonSslPort != null && nonSslPort > 0)
                nonSslRedirectPort.set(nonSslPort);

        } finally {
            write.unlock(); // ALWAYS unlock
        }
    }

    /**
     * Specifies a {@link UncaughtServletExceptionHandler} to which uncaught
     * exceptions thrown during request execution will be delegated.
     *
     * @param useh A defined UncaughtServletExceptionHandler
     */
    public static void registerUncaughtExceptionHandler(UncaughtServletExceptionHandler useh) {
        log.info("UncaughtExceptionHandler registered.");
        exceptionHandler.set(useh);
    }
}
