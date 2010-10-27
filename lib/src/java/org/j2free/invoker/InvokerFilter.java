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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

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
import net.jcip.annotations.GuardedBy;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.annotations.ServletConfig;
import org.j2free.annotations.FilterConfig;
import org.j2free.annotations.ServletConfig.SSLOption;
import org.j2free.servlet.EntityAdminServlet;
import org.j2free.servlet.LogoutServlet;
import org.j2free.servlet.ProxyServlet;
import org.j2free.servlet.StaticJspServlet;

import org.j2free.util.Constants;
import org.j2free.util.UncaughtServletExceptionHandler;

import static org.j2free.util.ServletUtils.*;
import static org.j2free.util.Constants.*;
import org.j2free.util.Global;

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
    // Convenience class for referencing static resources
    private final class StaticResource extends HttpServlet { }

    // Properties
    public static class Property
    {
        public static final String BENCHMARK_REQS   = "filter.invoker.benchmark.enabled";
        public static final String BYPASS_PATH      = "filter.invoker.bypass.path";
        public static final String MAX_SERVLET_USES = "filter.invoker.servlet-max-uses";
        public static final String DEFAULT_SSL_OPT  = "filter.invoker.default-ssl-option";
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

    @GuardedBy("lock") 
    private boolean benchmark = false;

    @GuardedBy("lock") 
    private int sslRedirectPort = -1;
    
    @GuardedBy("lock")
    private int nonSslRedirectPort = -1;

    @GuardedBy("lock")
    private int maxServletUses = 1000;
    
    @GuardedBy("lock")
    private String bypassPath = EMPTY;

    @GuardedBy("lock")
    private SSLOption defaultSSLOption = SSLOption.UNSPECIFIED;

    @GuardedBy("lock")
    private RequestExaminer requestExaminer = new RequestExaminerImpl();

    // For error handling, a handler and redirect location
    private UncaughtServletExceptionHandler uncaughtExceptionHandler = null;

    // Maps URLs to classes
    private final ConcurrentMap<String, Class<? extends HttpServlet>> urlMap
            = new ConcurrentHashMap(500, 0.8f, 50);

    // Maps the whole URL of resolved partial URLs to classes
    private final ConcurrentLinkedHashMap<String, Class<? extends HttpServlet>> partialsMap
            = ConcurrentLinkedHashMap.create(EvictionPolicy.LRU, 10000, 50);

    // A map of regex's to test mapping against
    private final ConcurrentMap<String, Class<? extends HttpServlet>> regexMap
            = new ConcurrentHashMap();

    // A map of HttpServlet classes to mapping for that class
    private final ConcurrentHashMap<Class<? extends HttpServlet>, ServletMapping> servletMap
            = new ConcurrentHashMap();

    // Class for resolving filters at various paths and depths
    private final ConcurrentSkipListSet<FilterMapping> filters = new ConcurrentSkipListSet();

    // Used to queue requests during (re)configuration.  Fairness is enabled,
    // otherwise writes could wait indefinitely because of the high contention
    // on this filter.
    private final ReadWriteLock lock  = new ReentrantReadWriteLock(true);
    private final Lock          read  = lock.readLock();
    private final Lock          write = lock.writeLock();

    private final Log log = LogFactory.getLog(getClass());

    public InvokerFilter()
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
        HttpServletRequest  httpReq  = (HttpServletRequest) req;
        HttpServletResponse httpResp = (HttpServletResponse) resp;

        // Get the path after the context-path (final so we can't accidentally fuck with it)
        final String path = httpReq.getRequestURI().substring(httpReq.getContextPath().length());

        // Benchmark vars
        final long start   = System.currentTimeMillis();  // start time

        long resolve = 0,                           // time after figuring out what to do
             process = 0,                           // time after processing
             finish  = 0;                           // finish time

        // Set cache-control based on content
        if (Constants.RUN_MODE.compareTo(RunMode.PRODUCTION) == -1) // dev mode
        {
            httpResp.setHeader(HEADER_PRAGMA, "no-cache");
            httpResp.setHeader(HEADER_CACHE_CONTROL, "no-store");
        } 
        else if (path.matches(SHORT_CACHE_REGEX) && !path.contains(CAPTCHA_PATH))
        {
            httpResp.setHeader(HEADER_PRAGMA, PRAGMA_VAL);
            httpResp.setHeader(HEADER_CACHE_CONTROL, SHORT_CACHE_VAL);
        } 
        else if (path.matches(LONG_CACHE_REGEX))
        {
            httpResp.setHeader(HEADER_PRAGMA, PRAGMA_VAL);
            httpResp.setHeader(HEADER_CACHE_CONTROL, LONG_CACHE_VAL);
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
            if (klass == null && !path.matches(bypassPath))
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
                boolean isSsl = requestExaminer.isSSL(httpReq);

                SSLOption sslOpt = mapping.config.ssl();
                if (sslOpt == SSLOption.UNSPECIFIED)
                    sslOpt = defaultSSLOption;
                
                if (sslOpt == SSLOption.REQUIRE && !isSsl)
                {
                    if (log.isDebugEnabled()) log.debug("Redirecting over SSL: " + path + " [url=" + httpReq.getRequestURL() + "]");
                    redirectOverSSL(httpReq, httpResp, sslRedirectPort);
                    return;
                } 
                else if (sslOpt == SSLOption.DENY && isSsl)
                {
                    if (log.isDebugEnabled()) log.debug("Redirecting off SSL: " + path + " [url=" + httpReq.getRequestURL() + "]");
                    redirectOverNonSSL(httpReq, httpResp, nonSslRedirectPort);
                    return;
                }

                try
                {
                    if (log.isTraceEnabled()) log.trace("Dynamic resource found, servicing with " + klass.getName());

                    // Get the time after finding the resource
                    resolve = System.currentTimeMillis();

                    // Service the end-point on the chain
                    if (log.isTraceEnabled())
                        log.trace("ServiceChain [path=" + path +", endPoint=" + mapping.getName() + "]");
                    
                    new ServiceChain(filters.iterator(), path, mapping).service(httpReq, httpResp);

                    // Get the time after running
                    process = System.currentTimeMillis();

                    if (httpReq.getParameter("benchmark") != null)
                        log.info(klass.getName() + " execution time: " + (process - start));
                } 
                catch (Exception e)
                {
                    process = System.currentTimeMillis();

                    if (uncaughtExceptionHandler != null)
                        uncaughtExceptionHandler.handleException(req, resp, e);
                    else
                        throw new ServletException(e);
                } 
                finally
                {
                    // maxUses values less than 0 indicate the value is not set and the default should be used
                    // maxUses == 0 indicates the servlet should NOT be reloaded
                    int maxUses = mapping.config.maxUses() < 0 ? maxServletUses : mapping.config.maxUses();
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
                                        log.trace("Successfully replaced old " + klass.getSimpleName() + " with a new instance");
                                    else
                                        log.trace("Failed to replace old " + klass.getSimpleName() + " with a new instance");
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

            if (benchmark)
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
     *
     */
    public void init(javax.servlet.FilterConfig fc)
    {
        try
        {
            write.lock();

            Configuration config = (Configuration)Global.get(CONTEXT_ATTR_CONFIG);
            if (config != null) configure(config);

            // Use a custom exception handler, if there is one
            UncaughtServletExceptionHandler ueh =
                    (UncaughtServletExceptionHandler) Global.get(CONTEXT_ATTR_UNCAUGHT_EXCEPTION_HANDLER);
            if (ueh != null) uncaughtExceptionHandler = ueh;

            // Use a custom request examiner, if there is one
            RequestExaminer reqex = (RequestExaminer) Global.get(CONTEXT_ATTR_REQUEST_EXAMINER);
            if (reqex != null) requestExaminer = reqex;

            ServletContext context = fc.getServletContext();
            load(context);

            // StaticJspServlet
            if (config.getBoolean(PROP_STATICJSP_ON, false))
            {
                String staticJspDir  = config == null ? DEFAULT_STATICJSP_DIR : config.getString(PROP_STATICJSP_DIR,DEFAULT_STATICJSP_DIR);
                String staticJspPath = config == null ? DEFAULT_STATICJSP_PATH : config.getString(PROP_STATICJSP_PATH,DEFAULT_STATICJSP_PATH);

                StaticJspServlet.directory.set(staticJspDir);

                Set<String> staticJsps = context.getResourcePaths(staticJspDir);
                if (staticJsps != null && !staticJsps.isEmpty())
                {
                    for (String jsp : staticJsps)
                    {
                        if (jsp.endsWith(".jsp"))
                        {
                            jsp = staticJspPath + jsp.replace(staticJspDir, EMPTY).replaceAll("\\.jsp$", EMPTY);
                            addServletMapping(jsp, StaticJspServlet.class);
                        }
                    }
                }
            }

            // LogoutServlet
            if (config.getBoolean(PROP_SERVLET_LOGOUT_ON, false))
                addServletMapping(config, PROP_SERVLET_LOGOUT_PATH, DEFAULT_LOGOUT_PATH, LogoutServlet.class);

            // ProxyServlet
            if (config.getBoolean(PROP_SERVLET_PROXY_ON, false))
                addServletMapping(config, PROP_SERVLET_PROXY_PATH, DEFAULT_PROXY_PATH, ProxyServlet.class);

            // Admin Servlet
            if (config.getBoolean(PROP_SERVLET_ADMIN_ON, false))
                addServletMapping(config, PROP_SERVLET_ADMIN_PATH, DEFAULT_ADMIN_PATH, EntityAdminServlet.class);
        }
        finally
        {
            write.unlock();
        }
    }

    /**
     * Release resources when destroyed
     */
    public void destroy()
    {
        try
        {
            write.lock();

            // Destroy the servlets used in the mapping
            for (ServletMapping mapping : servletMap.values())
                mapping.servlet.destroy();

            for (FilterMapping mapping : filters)
                mapping.filter.destroy();

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
     * Configures the InvokerFilter, locking to prevent request processing
     * while configuration is set.
     */
    private void configure(Configuration config)
    {
        try
        {
            write.lock();

            // In case the user has let us know about paths that are guaranteed static
            bypassPath = config.getString(Property.BYPASS_PATH, EMPTY);

            // Can enable benchmarking globally
            benchmark = config.getBoolean(Property.BENCHMARK_REQS, false);

            // Set the SSL redirect port
            int val = config.getInt(Constants.PROP_LOCALPORT_SSL, -1);
            if (val > 0)
                sslRedirectPort = val;

            // Set the SSL redirect port
            val = config.getInt(Constants.PROP_LOCALPORT, -1);
            if (val > 0)
                nonSslRedirectPort = val;

            // Set the reload threshold for servlets
            maxServletUses = config.getInt(Property.MAX_SERVLET_USES, 1000);

            // The default SSL option
            String defSSLStr = config.getString(Property.DEFAULT_SSL_OPT, null);
            if (defSSLStr != null)
            {
                try
                {
                    defaultSSLOption = SSLOption.valueOf(defSSLStr);
                }
                catch (Exception e)
                {
                    log.error("Error setting default SSLOption for value: " + defSSLStr);
                }
            }
        }
        finally
        {
            write.unlock(); // ALWAYS unlock
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
    public void load(final ServletContext context)
    {
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
                                servlet.init(new ServletConfigImpl(klass.getName(), context));

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
                                filter.init(new FilterConfigImpl(klass.getName(), context));

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
     * Locks to prevent request processing while mapping is added.
     *
     * @param path The path to map a Servlet to
     * @param klass The Servlet to map
     * @return null if the servlet was mapped to the path, or a class if another servlet was already mapped to that path
     */
    public Class<? extends HttpServlet> addServletMapping(String path, Class<? extends HttpServlet> klass)
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
     * Internal method for adding a servlet config
     *
     * @param config
     * @param pathProp
     * @param defaultPath
     * @param servletClass
     */
    private synchronized void addServletMapping(Configuration config, String pathProp, String defaultPath, Class<? extends HttpServlet> servletClass)
    {
        String path;
        Class  oldKlass;

        Iterator itr;

        List paths = config.getList(pathProp);

        if (paths == null)
        {
            oldKlass = addServletMapping(defaultPath, servletClass);
            if (oldKlass != null)
                log.error("Error mapping " + servletClass.getSimpleName() + ", " + oldKlass.getSimpleName() + " was alread mapped to " + defaultPath);
        }
        else
        {
            itr = paths.iterator();
            while (itr.hasNext())
            {
                path = (String)itr.next();
                oldKlass = addServletMapping(path, servletClass);
                if (oldKlass != null)
                    log.error("Error mapping " + servletClass.getSimpleName() + ", " + oldKlass.getSimpleName() + " was alread mapped to " + path);
            }
        }
    }
}