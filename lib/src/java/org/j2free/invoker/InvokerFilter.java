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
package org.j2free.invoker;

import com.reardencommerce.kernel.collections.shared.evictable.ConcurrentLinkedHashMap;
import com.reardencommerce.kernel.collections.shared.evictable.ConcurrentLinkedHashMap.EvictionPolicy;

import java.io.IOException;

import java.net.URL;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.annotations.ServletConfig;
import org.j2free.annotations.FilterConfig;
import org.j2free.annotations.ServletConfig.SSLOption;

import org.j2free.util.Constants;
import org.j2free.util.UncaughtServletExceptionHandler;

import static org.j2free.util.ServletUtils.*;
import static org.j2free.util.Constants.*;

import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.scannotation.WarUrlFinder;

/**
 * InvokerFilter will try to use the fastest method to resolve
 * a URL to a Servlet, which in most cases will be O(1).  "Most"
 * means either a Servlet that was mapped with a non-regex,
 * non-wildcard URL, or a Servlet that was mapped with a regex or
 * wildcard URL and was accessed somewhat recently.
 *
 * InvokerFilter will, however, attempt to contain its memory
 * usage, preferring to resolve infrequently access URLs only
 * when requested.
 *
 * @author  Ryan Wilson
 */
public final class InvokerFilter implements Filter
{
    private static final Log log = LogFactory.getLog(InvokerFilter.class);

    // Convenience class for referencing static resources
    private static final class StaticResource extends HttpServlet { }

    // Properties
    public static class Property
    {
        public static final String BENCHMARK_REQS   = "filter.invoker.benchmark.enabled";
        public static final String BYPASS_PATH      = "filter.invoker.bypass.path";
        public static final String MAX_SERVLET_USES = "filter.invoker.servlet-max-uses";
    }

    // Paths to set different cache settings
    private static final String SHORT_CACHE_REGEX           = ".*?\\.(jpg|gif|png|jpeg)";
    private static final String LONG_CACHE_REGEX            = ".*?\\.(swf|js|css|flv)";
    private static final String CAPTCHA_PATH                = "captcha.jpg";

    private static final String SHORT_CACHE_VAL             = "max-age=21600";
    private static final String LONG_CACHE_VAL              = "max-age=31536000";

    private static final String HEADER_PRAGMA               = "Pragma";
    private static final String HEADER_CACHE_CONTROL        = "Cache-Control";

    private static final String PRAGMA_VAL                  = "cache";

    private static final AtomicBoolean benchmark            = new AtomicBoolean(false);
    private static final AtomicInteger sslRedirectPort      = new AtomicInteger(-1);
    private static final AtomicInteger nonSslRedirectPort   = new AtomicInteger(-1);
    private static final AtomicInteger maxServletUses       = new AtomicInteger(1000);
    private static final AtomicReference<String> bypassPath = new AtomicReference(EMPTY);

    // For error handling, a handler and redirect location
    private static final AtomicReference<UncaughtServletExceptionHandler> exceptionHandler
            = new AtomicReference(null);

    // Maps URLs to classes
    private static final ConcurrentMap<String, Class<? extends HttpServlet>> urlMap
            = new ConcurrentHashMap(500, 0.8f, 50);

    // Maps the whole URL of resolved partial URLs to classes
    private static final ConcurrentLinkedHashMap<String, Class<? extends HttpServlet>> partialsMap
            = ConcurrentLinkedHashMap.create(EvictionPolicy.LRU, 10000, 50);

    // A map of regex's to test mapping against
    private static final ConcurrentMap<String, Class<? extends HttpServlet>> regexMap
            = new ConcurrentHashMap();

    // A map of HttpServlet classes to mapping for that class
    private static final ConcurrentHashMap<Class<? extends HttpServlet>, ServletMapping> servletMap
            = new ConcurrentHashMap();

    // Class for resolving filters at various paths and depths
    private static final ConcurrentSkipListSet<FilterMapping> filters = new ConcurrentSkipListSet();

    // Used to queue requests during (re)configuration.  Fairness is enabled,
    // otherwise writes could wait indefinitely because of the high contention
    // on this filter.
    private static final ReadWriteLock lock  = new ReentrantReadWriteLock(true);
    private static final Lock          read  = lock.readLock();
    private static final Lock          write = lock.writeLock();

    /**
     * Required impls
     */
    public void init(javax.servlet.FilterConfig fc)
    { }
    
    public void destroy()
    { }

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
            throws IOException, ServletException
    {
        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        // Get the path after the context-path (final so we can't accidentally fuck with it)
        final String path = request.getRequestURI().substring(request.getContextPath().length());

        // Benchmark vars
        final long start   = System.currentTimeMillis();  // start time

        long resolve = 0,                           // time after figuring out what to do
             process = 0,                           // time after processing
             finish  = 0;                           // finish time

        // Set cache-control based on content
        if (Constants.RUN_MODE.compareTo(RunMode.PRODUCTION) == -1) // dev mode
        {
            response.setHeader(HEADER_PRAGMA, "no-cache");
            response.setHeader(HEADER_CACHE_CONTROL, "no-store");
        } 
        else if (path.matches(SHORT_CACHE_REGEX) && !path.contains(CAPTCHA_PATH))
        {
            response.setHeader(HEADER_PRAGMA, PRAGMA_VAL);
            response.setHeader(HEADER_CACHE_CONTROL, SHORT_CACHE_VAL);
        } 
        else if (path.matches(LONG_CACHE_REGEX))
        {
            response.setHeader(HEADER_PRAGMA, PRAGMA_VAL);
            response.setHeader(HEADER_CACHE_CONTROL, LONG_CACHE_VAL);
        }

        try
        {
            // This will block requests during configuration
            read.lock();

            if (log.isTraceEnabled())
                log.trace("InvokerFilter for path: " + path);

            // Try to get an explicit mapping from the whole URL to a class
            Class<? extends HttpServlet> klass = urlMap.get(path);

            // Try to a fast lookup using the whole URL for a previously resolved partial URL.
            if (klass == null) klass = partialsMap.get(path);

            /**
             * If we don't already have an exact match for this path,
             * try to break it down.
             *
             * Certain extensions are known to be mapped in web.xml,
             * known to never be dynamic resources (e.g. .swf), or
             * were discovered earlier to be static content, so don't
             * process those.
             */
            if (klass == null && !path.matches(bypassPath.get()))
            {
                // (1) Look for *.ext wildcard matches
                String partial;

                int index = path.lastIndexOf(".");          // If the path contains a "." then check for the *.ext patterns
                if (index != -1)
                {
                    partial = "*" + path.substring(index); // gives us the *.<THE_EXTENSION>
                    klass = urlMap.get(partial);
                }

                // (2) Check any regex mapping against the path
                if (klass == null)
                {
                    // @TODO this iteration could be coslty... perhaps it would be more efficient to have
                    //       constructed one long regex of all the possible regex mapping with
                    String regex;
                    Iterator<String> itr = regexMap.keySet().iterator();
                    while (itr.hasNext())
                    {
                        regex = itr.next();
                        if (path.matches(regex))
                        {
                            // Sweet, we have a match, but make sure we actually get the klass
                            // since it could have been altered since we got the iterator (though, unlikely)
                            klass = regexMap.get(regex);
                            if (klass != null)
                            {
                                // Even better, we got the klass, so move on
                                break;
                            }
                        }
                    }
                }

                // (3) Check for possible /something/* patterns
                if (klass == null)
                {
                    // start with the full path
                    partial = path; 

                    // Start with most specific and move down to just /*
                    while ((index = partial.lastIndexOf("/")) > 0)
                    {
                        // Chop off everything past the last "/" and add the "*"
                        partial = partial.substring(0, index + 1) + "*"; // if we had /first/second, we'd get /first/*

                        if (log.isTraceEnabled()) log.trace("Trying wildcard partial resource: " + partial);

                        klass = urlMap.get(partial);

                        // if we found a match, or if we made it to the simplest form and didn't find anything, get out
                        if (klass != null || partial.equals("/*"))
                            break;

                        // Otherwise, let's try the next chunk, so chop the ending "/*" off
                        partial = partial.substring(0, index); // If we had /first/second to start, we'd get /first

                        if (log.isTraceEnabled()) log.trace("Next partial: " + partial);
                    }
                }

                // (4) If we found a class in any way, register it with the currentPath for faster future lookups
                //     UNLESS the config for that klass prohibits it (e.g. a servlet mapped to /user/* in an app
                //     with millions of users could increase the size of urlMap well beyond what is optimal, so
                //     if a servlet knows that is a possibility, it can specify to not save direct mapping when
                //     the servlet was found via a partial mapping)
                if (klass != null)
                {
                    if (log.isTraceEnabled()) log.trace("Matched path " + path + " to " + klass.getName());
                    
                    // Make sure the ServletConfig supports direct lookups before storing the resolved path
                    if (servletMap.get(klass).config.preferDirectLookups())
                    {
                        partialsMap.putIfAbsent(path, klass);
                    }
                }
            }

            // If we didn't find it, then just pass it on
            if (klass == null)
            {
                if (log.isTraceEnabled()) log.trace("Dynamic resource not found for path: " + path);

                // Save this path in the staticSet so we don't have to look it up next time
                urlMap.putIfAbsent(path, StaticResource.class);

                resolve = System.currentTimeMillis();
                chain.doFilter(req, resp);
                process  = System.currentTimeMillis();
            } 
            else if (klass == StaticResource.class)
            {
                // If it's known to be static, then pass it on
                if (log.isTraceEnabled())
                    log.trace("Processing known static path: " + path);

                resolve = System.currentTimeMillis();
                chain.doFilter(req, resp);
                process  = System.currentTimeMillis();
            } 
            else
            {
                ServletMapping mapping = servletMap.get(klass);

                // If the klass requires SSL, make sure we're on an SSL connection
                SSLOption sslOpt = mapping.config.ssl();
                boolean   isSsl  = request.isSecure();
                
                if (sslOpt == SSLOption.REQUIRE && !isSsl)
                {
                    if (log.isDebugEnabled()) log.debug("Redirecting over SSL: " + path);
                    redirectOverSSL(request, response, sslRedirectPort.get());
                    return;
                } 
                else if (sslOpt == SSLOption.DENY && isSsl)
                {
                    if (log.isDebugEnabled()) log.debug("Redirecting off SSL: " + path);
                    redirectOverNonSSL(request, response, nonSslRedirectPort.get());
                    return;
                }

                try
                {
                    if (log.isTraceEnabled()) log.trace("Dynamic resource found, servicing with " + klass.getName());

                    // Get the time after finding the resource
                    resolve = System.currentTimeMillis();

                    // Service the end-point on the chain
                    if (log.isTraceEnabled())
                    {
                        log.trace("ServiceChain [path=" + path +", endPoint=" + mapping.getName() + "]");
                    }
                    
                    new ServiceChain(filters.iterator(), path, mapping).service(request, response);

                    // Get the time after running
                    process = System.currentTimeMillis();

                    if (request.getParameter("benchmark") != null)
                    {
                        log.info(klass.getName() + " execution time: " + (process - start));
                    }
                } 
                catch (Exception e)
                {
                    process = System.currentTimeMillis();

                    UncaughtServletExceptionHandler uceh = exceptionHandler.get();
                    if (uceh != null)
                    {
                        uceh.handleException(req, resp, e);
                    }
                    else
                        throw new ServletException(e);
                } 
                finally
                {
                    int maxUses = mapping.config.maxUses() < 0 ? maxServletUses.get() : mapping.config.maxUses();
                    if (maxUses > 0)
                    {
                        long instanceUses = mapping.incrementUses(); // only increment if servlet reloading is enabled
                        if (instanceUses >= maxUses)
                        {
                            try
                            {
                                HttpServlet newInstance = klass.newInstance();          // Create a new instance
                                newInstance.init(mapping.servlet.getServletConfig());   // Copy over the javax.servlet.ServletConfig

                                ServletMapping newMapping = new ServletMapping(newInstance, mapping.config);  // new instance but old config

                                if (log.isTraceEnabled())
                                {
                                    if (servletMap.replace(klass, mapping, newMapping))
                                    {
                                        log.trace("Successfully replaced old " + klass.getSimpleName() + " with a new instance");
                                    }
                                    else
                                    {
                                        log.trace("Failed to replace old " + klass.getSimpleName() + " with a new instance");
                                    }
                                }
                                else
                                {
                                    // if we're not tracing, don't bother checking the result, because
                                    // either (a) it succeeded and the new servlet is in place, or
                                    // (b) it failed meaning another thread beat us to it.
                                    servletMap.replace(klass, mapping, newMapping);
                                }

                                // In either case, the old serlvet is no longer in use, but DON'T
                                // destroy it yet, since it may be in the process of serving other
                                // requests. By removing the mapping to it, it should be garbage
                                // collected.
                            }
                            catch (Exception e)
                            {
                                log.error("Error replacing " + klass.getSimpleName() + " instance after " + instanceUses + " uses!", e);
                            }
                        }
                    }
                }
            }

            finish = System.currentTimeMillis();

            if (benchmark.get())
            {
                log.info(
                    String.format(
                        "[path=%s, find=%d, run=%d, finish=%d]",
                        path, (resolve - start), (process - resolve), (finish - process)
                    )
                );
            }
        } 
        finally
        {
            read.unlock(); // Make sure to release the read lock
        }
    }

    /**
     * Locks to prevent request processing while mapping is added.
     *
     * Finds all classes annotated with ServletConfig and maps the class to
     * the url specified in the annotation.  Wildcard mapping are allowed in
     * the form of *.extension or /some/path/*
     *
     * @param context an active ServletContext
     */
    public static void load(final ServletContext context)
    {
        log.debug("Scanning resources...");
        try
        {
            write.lock();

            LinkedList<URL> urlList = new LinkedList<URL>();
            urlList.addAll(Arrays.asList(ClasspathUrlFinder.findResourceBases(EMPTY)));
            urlList.addAll(Arrays.asList(WarUrlFinder.findWebInfLibClasspaths(context)));
            
            URL[] urls = new URL[urlList.size()];
            urls = urlList.toArray(urls);

            AnnotationDB annoDB = new AnnotationDB();
            annoDB.setScanClassAnnotations(true);
            annoDB.setScanFieldAnnotations(false);
            annoDB.setScanMethodAnnotations(false);
            annoDB.setScanParameterAnnotations(false);
            annoDB.scanArchives(urls);

            HashMap<String, Set<String>> annotationIndex = (HashMap<String, Set<String>>) annoDB.getAnnotationIndex();
            if (annotationIndex != null && !annotationIndex.isEmpty())
            {
                //-----------------------------------------------------------
                // Look for any classes annotated with @ServletConfig
                Set<String> classNames = annotationIndex.get(ServletConfig.class.getName());
                
                if (classNames != null)
                {
                    for (String c : classNames)
                    {
                        try
                        {
                            final Class<? extends HttpServlet> klass = (Class<? extends HttpServlet>)Class.forName(c);
                            
                            if (klass.isAnnotationPresent(ServletConfig.class))
                            {
                                final ServletConfig config = (ServletConfig)klass.getAnnotation(ServletConfig.class);

                                // If the config specifies String mapppings...
                                if (config.mappings() != null)
                                {
                                    for (String url : config.mappings())
                                    {
                                        // Leave the asterisk, we'll add it when matching...
                                        //if (url.matches("(^\\*[^*]*?)|([^*]*?/\\*$)"))
                                        //    url = url.replace("*", EMPTY);

                                        if (urlMap.putIfAbsent(url, klass) == null)
                                        {
                                            if (log.isDebugEnabled()) log.debug("Mapping servlet " + klass.getName() + " to path " + url);
                                        } 
                                        else
                                        {
                                            log.error("Unable to map servlet  " + klass.getName() + " to path " + url + ", path already mapped to " + urlMap.get(url).getName());
                                        }
                                    }
                                }

                                // If the config specifies a regex mapping...
                                if (!empty(config.regex()))
                                {
                                    regexMap.putIfAbsent(config.regex(), klass);
                                    if (log.isDebugEnabled())
                                        log.debug("Mapping servlet " + klass.getName() + " to regex path " + config.regex());
                                }

                                // Create an instance of the servlet and init it
                                HttpServlet servlet = klass.newInstance();
                                servlet.init(
                                        new javax.servlet.ServletConfig()
                                        {
                                            public String getServletName()
                                            {
                                                return klass.getName();
                                            }

                                            public ServletContext getServletContext()
                                            {
                                                return context;
                                            }

                                            public String getInitParameter(String name)
                                            {
                                                return null;
                                            }

                                            public Enumeration getInitParameterNames()
                                            {
                                                return null;
                                            }
                                        }
                                    );

                                // Store a reference
                                servletMap.put(klass, new ServletMapping(servlet, config));
                            }

                        } 
                        catch (Exception e)
                        {
                            log.error("Error registering servlet [name=" + c + "]", e);
                        }
                    }
                }

                //-----------------------------------------------------------
                // Look for any classes annotated with @FiltersConfig
                classNames = annotationIndex.get(FilterConfig.class.getName());
                if (classNames != null)
                {
                    for (String c : classNames)
                    {
                        try
                        {
                            final Class<? extends Filter> klass = (Class<? extends Filter>)Class.forName(c);

                            if (klass.isAnnotationPresent(FilterConfig.class))
                            {
                                final FilterConfig config = (FilterConfig)klass.getAnnotation(FilterConfig.class);

                                // Create an instance of the servlet and init it
                                Filter filter = klass.newInstance();
                                filter.init(
                                        new javax.servlet.FilterConfig()
                                        {
                                            public String getFilterName()
                                            {
                                                return klass.getName();
                                            }

                                            public ServletContext getServletContext()
                                            {
                                                return context;
                                            }

                                            public String getInitParameter(String namw)
                                            {
                                                return null;
                                            }

                                            public Enumeration getInitParameterNames()
                                            {
                                                return null;
                                            }
                                        }
                                    );

                                if (log.isDebugEnabled())
                                    log.debug("Mapping filter " + klass.getName() + " to path " + config.mapping());
                                    
                                // Store a reference
                                filters.add( new FilterMapping(filter, config) );
                            }
                        }
                        catch (Exception e)
                        {
                            log.error("Error registering servlet [name=" + c + "]", e);
                        }
                    }
                }
            }
        } 
        catch (IOException e)
        {
            log.error("Error loading urlMappings", e);
        }
        finally
        {
            write.unlock(); // ALWAYS Release the configure lock
        }
    }

    /**
     * Clears loaded configuration, used for dynamic reconfiguation.
     * Locks to prevent request processing while modifications are made.
     */
    public static void reset()
    {
        try
        {
            write.lock();

            // Destroy the servlets used in the mapping
            for (ServletMapping mapping : servletMap.values())
            {
                mapping.servlet.destroy();
            }

            urlMap.clear();
            partialsMap.clear();
            regexMap.clear();
            servletMap.clear();
            filters.clear();

        } 
        finally
        {
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
    public static Class<? extends HttpServlet> addServletMapping(String path, Class<? extends HttpServlet> klass)
    {
        try
        {
            write.lock();

            if (servletMap.get(klass) == null)
                throw new IllegalStateException("Illegal attempt to create a mapping to an unregistered servlet!");

            if (log.isDebugEnabled())
                log.debug("Mapping servlet " + klass.getName() + " to path " + path);

            // Leave the asterisk, we'll add it when matching...
            //if (path.matches("(^\\*[^*]*?)|([^*]*?/\\*$)"))
            //    path = path.replace("*", EMPTY);

            return urlMap.putIfAbsent(path, klass);
        } 
        finally
        {
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
    public static void configure(Configuration config)
    {
        try
        {
            write.lock();

            // In case the user has let us know about paths that are guaranteed static
            bypassPath.set( config.getString(Property.BYPASS_PATH, EMPTY) );

            // Can enable benchmarking globally
            benchmark.set( config.getBoolean(Property.BENCHMARK_REQS, false) );

            // Set the SSL redirect port
            int val = config.getInt(Constants.PROP_LOCALPORT_SSL, -1);
            if (val > 0)
                sslRedirectPort.set(val);

            // Set the SSL redirect port
            val = config.getInt(Constants.PROP_LOCALPORT, -1);
            if (val > 0)
                nonSslRedirectPort.set(val);

            // Set the reload threshold for servlets
            maxServletUses.set( config.getInt(Property.MAX_SERVLET_USES, 1000) );
        } 
        finally
        {
            write.unlock(); // ALWAYS unlock
        }
    }

    /**
     * Specifies a {@link UncaughtServletExceptionHandler} to which uncaught
     * exceptions thrown during request execution will be delegated.
     *
     * @param useh A defined UncaughtServletExceptionHandler
     */
    public static void registerUncaughtExceptionHandler(UncaughtServletExceptionHandler handler)
    {
        log.info("UncaughtExceptionHandler registered.");
        exceptionHandler.set(handler);
    }
}