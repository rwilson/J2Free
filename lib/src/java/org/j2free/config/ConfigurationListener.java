/*
 * ConfigurationListener.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
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
package org.j2free.config;

import org.j2free.servlet.*;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.net.InetAddress;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.cache.FragmentCache;
import org.j2free.email.EmailService.ContentType;
import org.j2free.email.ErrorReporter;
import org.j2free.email.SimpleEmailService;
import org.j2free.email.Template;
import org.j2free.http.SimpleHttpService;
import org.j2free.jsp.tags.FragmentCacheTag;
import org.j2free.invoker.InvokerFilter;
import org.j2free.util.Global;
import org.j2free.util.KeyValuePair;
import org.j2free.util.Priority;

import org.j2free.util.concurrent.ConcurrentHashSet;

import static org.j2free.util.Constants.*;


/**
 * If a <tt>Configuration</tt> is found and successfully loaded, then
 * the following items will be configured accordingly:
 *  - <tt>RunMode</tt>
 *  - <tt>InvokerFilter</tt>
 *  - <tt>StaticJspServlet</tt>
 *  - <tt>LogoutServlet</tt>
 *  - <tt>ProxyServlet</tt>
 *  - <tt>EntityAdminServlet</tt>
 *  - <tt>Fragment Caching</tt>
 *  - <tt>Task ScheduledExecutorService</tt>
 *  - <tt>SimpleEmailService</tt>
 *  - <tt>SimpleHttpService</tt>
 *  - <tt>ErrorReporter</tt>
 *  - <tt>Spymemcached</tt>
 *  - <tt>Dynamic Reconfiguration</tt>
 *
 * Lacking a <tt>Configuration</tt> default values will be used, and
 * the above items will not be initialized.
 *
 * Finally, this method of configuration can be ignore entirely by
 * removing the <tt>servlet-mapping</tt>.
 *
 * @see <tt>org.j2free.util.Constants</tt> for a list of configurable
 * properties.
 *
 * @author Ryan Wilson
 */
public class ConfigurationListener implements ServletContextListener {

    private final Log log = LogFactory.getLog(ConfigurationListener.class);

    private Runnable        reconfigTask;
    private ScheduledFuture reconfigFuture;

    // Stores what properties from the config file are saved in the ServletContext,
    // so that they can be removed before reconfiguration
    private final ConcurrentHashSet<String> loadedConfigPropKeys = new ConcurrentHashSet();

    protected ServletContext context;

    public synchronized void contextInitialized(ServletContextEvent event) {

        context = event.getServletContext();
        
        // Get the configuration file
        String configPathTemp = (String)context.getInitParameter(INIT_PARAM_CONFIG_PATH);

        // Use the default path if it wasn't specified
        if (StringUtils.isBlank(configPathTemp))
            configPathTemp = DEFAULT_CONFIG_PATH;

        // Finalize the config path (needs to be final for inner-Runnable below)
        final String configPath = configPathTemp;
        context.setAttribute(CONTEXT_ATTR_CONFIG_PATH, configPath);

        try {
            // Load the configuration
            final PropertiesConfiguration config = new PropertiesConfiguration(configPath);
            config.setAutoSave(false);

            // Save the config where we can get at it later
            context.setAttribute(CONTEXT_ATTR_CONFIG, config);
            Global.put(CONTEXT_ATTR_CONFIG, config);

            // Configure the app
            configure(config);

            // Dynamic reconfiguration settings (only modifiable on initial load)
            /*
            if (config.getBoolean(PROP_RECONFIG_ENABLED, false)) {

                // Dynamic reconfig requires task-executor
                ScheduledExecutorService executor = (ScheduledExecutorService)Global.get(CONTEXT_ATTR_TASK_MANAGER);
                
                if (executor == null) {
                    log.warn("Task execution must be enabled to use dynamic reconfiguration!");
                } else {

                    // Create a task for reconfiguring the app
                    Runnable reconfigTask = new Runnable() {

                        private long lastModified = config.getFile().lastModified();

                        public void run() {

                            if (config.getFile().lastModified() > lastModified) {             // See if the file has changed

                                log.info(configPath + " has changed, proceeding with reconfiguration.");

                                try {
                                    config.save(configPath + ".bkp");       // Save a backup of the current config before loading the changes
                                } catch (ConfigurationException ce) {
                                    log.error("Error backing up config file, cancelling reconfiguration", ce);
                                    return;
                                }

                                try {
                                    
                                    config.load(configPath);                // load the changes
                                    reconfigure(config);                    // try to reconfigure the app

                                    lastModified = config.getFile().lastModified();
                                    
                                } catch (ConfigurationException ce) {
                                    log.error("Error reconfiguration app, reverting to old config...", ce);
                                    try {
                                        config.load(configPath + ".bkp");   // reset the backup configuration
                                        reconfigure(config);                // reconfigure with the backup
                                    } catch (ConfigurationException cee) {
                                        log.fatal("Error loading backup configuration! Exiting...", cee);
                                        System.exit(1); // Don't have an active config right now, so gtfo
                                    }
                                }
                                
                            } else if (log.isTraceEnabled()) {
                                log.trace("Config file has not changed.");
                            }
                        }
                    };

                    // save a ref to the task
                    reconfigTaskRef.set(reconfigTask);

                    // Schedule the task
                    int interval = config.getInt(PROP_RECONFIG_INTERVAL, DEFAULT_RECONFIG_INTERVAL);
                    ScheduledFuture reconfigFuture = executor.scheduleWithFixedDelay(
                            reconfigTask,
                            interval,
                            interval,
                            TimeUnit.SECONDS
                        );

                    log.info(
                        String.format(
                            "Reconfiguation enabled, %s will be checked every %d seconds for changes",
                            configPath,
                            interval
                        )
                    );

                    // save a ref to the future
                    reconfigFutureRef.set(reconfigFuture);
                }
             }
             */

        } catch (ConfigurationException ce) {
            log.error("Error configuring app", ce);
        }
    }

    /**
     * Configures a J2Free application
     */
    private synchronized void configure(Configuration config) throws ConfigurationException {

        // Anything with the value "localhost" will be set to the IP if possible
        String localhost = config.getString(PROP_LOCALHOST, "localhost");
        if (localhost.equalsIgnoreCase("ip")) {
            try {
                localhost = InetAddress.getLocalHost().getHostAddress();
                log.info("localhost = " + localhost);
            } catch (Exception e) {
                log.warn("Error determining localhost", e);
                localhost = "localhost";
            }
        }

        context.setAttribute("localhost", localhost);
        loadedConfigPropKeys.add("localhost");

        // Set application context attributes for all config properties
        String prop, value;
        Iterator itr = config.getKeys();
        while (itr.hasNext()) {

            prop  = (String)itr.next();
            value = config.getString(prop);
            value = (value.equals("localhost") ? localhost : value);

            log.info("Setting application context attribute: [name=" + prop + ",value=" + value + "]");
            context.setAttribute(prop,value);
            loadedConfigPropKeys.add(prop);
        }

        // Run Mode configuration
        String runMode = config.getString(PROP_RUNMODE);
        try {
            RUN_MODE = RunMode.valueOf(runMode);
        } catch (Exception e) {
            log.warn("Error setting runmode, invalid value: " + runMode);
        }

        context.setAttribute("devMode", RUN_MODE != RunMode.PRODUCTION);
        loadedConfigPropKeys.add("devMode");

        // InvokerFilter
        InvokerFilter.configure(config);
        InvokerFilter.load(context);

        // StaticJspServlet
        if (config.getBoolean(PROP_STATICJSP_ON, false)) {

            String staticJspDir  = config == null ? DEFAULT_STATICJSP_DIR : config.getString(PROP_STATICJSP_DIR,DEFAULT_STATICJSP_DIR);
            String staticJspPath = config == null ? DEFAULT_STATICJSP_PATH : config.getString(PROP_STATICJSP_PATH,DEFAULT_STATICJSP_PATH);

            StaticJspServlet.directory.set(staticJspDir);

            Set<String> staticJsps = context.getResourcePaths(staticJspDir);
            if (staticJsps != null && !staticJsps.isEmpty()) {
                for (String jsp : staticJsps) {
                    if (jsp.endsWith(".jsp")) {
                        jsp = staticJspPath + jsp.replace(staticJspDir, EMPTY).replaceAll("\\.jsp$", EMPTY);
                        InvokerFilter.addServletMapping(jsp, StaticJspServlet.class);
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

        // Fragment Cache Configuration
        if (config.getBoolean(FragmentCache.Properties.ENABLED, false)) {

            log.info("Enabling fragment caching...");
            FragmentCacheTag.enable();

            // This is expected to be in seconds
            long temp = config.getLong(FragmentCache.Properties.REQUEST_TIMEOUT, -1l);
            if (temp != -1) {
                log.info("Setting FragmentCacheTag request timeout: " + temp);
                FragmentCacheTag.setRequestTimeout(temp);
            }

            // The property is in seconds, but WARNING_COMPUTE_DURATION does NOT use a TimeUnit, so it's in ms
            temp = config.getLong(FragmentCache.Properties.WARNING_DURATION, -1l);
            if (temp != -1) {
                log.info("Setting FragmentCacheTag warning duration: " + temp);
                FragmentCacheTag.setWarningComputeDuration(temp * 1000);
            }

            // Get the fragment cache names
            String[] cacheNames = config.getStringArray(FragmentCache.Properties.ENGINE_NAMES);
            for (String cacheName : cacheNames) {

                String cacheClassName = config.getString(
                                            String.format(FragmentCache.Properties.ENGINE_CLASS_TEMPLATE, cacheName)
                                        );
                try {

                    // Load up the class
                    Class<? extends FragmentCache> cacheClass = (Class<? extends FragmentCache>)Class.forName(cacheClassName);

                    // Look for a constructor that takes a config
                    Constructor<? extends FragmentCache> constructor = null;
                    try {
                        constructor = cacheClass.getConstructor(Configuration.class);
                    } catch (Exception e) { }

                    FragmentCache cache;

                    // If we found the configuration constructor, use it
                    if (constructor != null) {
                        cache = constructor.newInstance(config);
                    } else {
                        // otherwise use a default no-args constructor
                        log.warn("Could not find a " + cacheClass.getSimpleName() + " constructor that takes a Configuration, defaulting to no-args constructor");
                        cache = cacheClass.newInstance();
                    }

                    // register the cache with the FragmentCacheTag using the config strategy-name, or the engineName
                    // if a strategy-name is not specified
                    log.info("Registering FragmentCache strategy: [name=" + cacheName + ", class=" + cacheClass.getName() + "]");
                    FragmentCacheTag.registerStrategy(cacheName, cache);

                } catch (Exception e) {
                    log.error("Error enabling FragmentCache engine: " + cacheName, e);
                }
            }
            
        } else {
            // Have to call this here, because reconfiguration could turn
            // the cache off after it was previously enabled.
            FragmentCacheTag.disable();
        }

        // For Task execution
        ScheduledExecutorService taskExecutor;
        
        if (config.getBoolean(PROP_TASK_EXECUTOR_ON, false)) {

            int threads = config.getInt(PROP_TASK_EXECUTOR_THREADS,DEFAULT_TASK_EXECUTOR_THREADS);

            if (threads == 1)
                taskExecutor = Executors.newSingleThreadScheduledExecutor();
            else
                taskExecutor = Executors.newScheduledThreadPool(threads);

            context.setAttribute(CONTEXT_ATTR_TASK_MANAGER, taskExecutor);
            loadedConfigPropKeys.add(CONTEXT_ATTR_TASK_MANAGER);
            
            Global.put(CONTEXT_ATTR_TASK_MANAGER, taskExecutor);
            
        } else {

            // Not allowed to shutdown the taskExecutor if dynamic reconfig is enabled
            if (reconfigTask == null) {
                // Shutdown and remove references to the taskManager previously created
                taskExecutor = (ScheduledExecutorService)Global.get(CONTEXT_ATTR_TASK_MANAGER);
                if (taskExecutor != null) {
                    taskExecutor.shutdown(); // will block until all tasks complete
                    taskExecutor = null;
                    Global.remove(CONTEXT_ATTR_TASK_MANAGER);
                }
            } else {
                // We could just log a warning that you can't do this, but the user
                // might not see that, so we're going to refuse to reset a configuration
                // that cannot be loaded in whole successfully.
                throw new ConfigurationException("Cannot disable task execution service, dynamic reconfiguration is enabled!");
            }
        }

        // Email Service
        if (config.getBoolean(PROP_MAIL_SERVICE_ON, false)) {

            if (!SimpleEmailService.isEnabled()) {
                // Get the SMTP properties
                Properties props = System.getProperties();
                props.put(PROP_SMTP_HOST, config.getString(PROP_SMTP_HOST));
                props.put(PROP_SMTP_PORT, config.getString(PROP_SMTP_PORT));
                props.put(PROP_SMTP_AUTH, config.getString(PROP_SMTP_AUTH));

                Session session;

                if (config.getBoolean(PROP_SMTP_AUTH)) {

                    final String user = config.getString(PROP_SMTP_USER);
                    final String pass = config.getString(PROP_SMTP_PASS);

                    Authenticator auth = new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(user,pass);
                        }
                    };
                    session = Session.getInstance(props,auth);

                } else {
                    session = Session.getInstance(props);
                }

                // Get the global headers
                Iterator headerNames = config.getKeys(PROP_MAIL_HEADER_PREFIX);
                List<KeyValuePair<String,String>> headers = new LinkedList<KeyValuePair<String,String>>();

                String headerName;
                while (headerNames.hasNext()) {
                    headerName = (String)headerNames.next();
                    headers.add( new KeyValuePair<String,String>(headerName, config.getString(headerName)) );
                }

                // Initialize the service
                SimpleEmailService.init(session);
                SimpleEmailService.setGlobalHeaders(headers);

                // Set whether we actually send the e-mails
                SimpleEmailService.setDummyMode(
                        config.getBoolean(PROP_MAIL_DUMMY_MODE, false)
                    );

                // Set the failure policy
                String policy = config.getString(PROP_MAIL_ERROR_POLICY);
                if (policy != null) {

                    if (policy.equals(VALUE_MAIL_POLICY_DISCARD)) {

                        SimpleEmailService.setErrorPolicy(new SimpleEmailService.DiscardPolicy());

                    } else if (policy.equals(VALUE_MAIL_POLICY_REQUEUE)) {

                        Priority priority = null;
                        try {
                            priority = Priority.valueOf(config.getString(PROP_MAIL_REQUEUE_PRIORITY));
                        } catch (Exception e) {
                            log.warn("Error reading requeue policy priority: " + config.getString(PROP_MAIL_REQUEUE_PRIORITY,"") + ", using default");
                        }

                        if (priority == null) {
                            SimpleEmailService.setErrorPolicy(new SimpleEmailService.RequeuePolicy());
                        } else {
                            SimpleEmailService.setErrorPolicy(new SimpleEmailService.RequeuePolicy(priority));
                        }
                    }
                }

                // Parse templates
                String emailTemplateDir = config.getString(PROP_MAIL_TEMPLATE_DIR);

                // If the template
                if (StringUtils.isBlank(emailTemplateDir))
                    emailTemplateDir = DEFAULT_EMAIL_TEMPLATE_DIR;

                log.info("Looking for e-mail templates in: " + emailTemplateDir);
                Set<String> templates = context.getResourcePaths(emailTemplateDir);

                // E-mail templates
                if (templates != null && !templates.isEmpty()) {

                    log.info("Found " + templates.size() + " templates");

                    String key;
                    String defaultTemplate = config.getString(PROP_MAIL_DEFAULT_TEMPLATE,EMPTY);

                    InputStream in;
                    StringBuilder builder;
                    Scanner scanner;

                    try {

                        Template template;
                        String[] parts;

                        ContentType contentType;

                        for (String path : templates) {

                            path  = path.trim();
                            parts = path.split("\\.");

                            contentType = ContentType.valueOfExt(parts[1]);

                            try {
                                in = context.getResourceAsStream(path.trim());

                                if (in != null && in.available() > 0) {

                                    scanner = new Scanner(in);
                                    builder = new StringBuilder();

                                    while (scanner.hasNextLine()) {
                                        builder.append(scanner.nextLine());
                                        if (contentType == ContentType.PLAIN) {
                                            builder.append("\n");
                                        }
                                    }

                                    template = new Template(builder.toString(), contentType);

                                    key = parts[0].replace(emailTemplateDir, EMPTY);
                                    SimpleEmailService.registerTemplate(key, template, key.equals(defaultTemplate));
                                }

                            } catch (IOException ioe) {
                                log.error("Error loading e-mail template: " + path,ioe);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error loading e-mail templates",e);
                    }
                }
            }
        } else if (SimpleEmailService.isEnabled()) {
            boolean shutdown = false;
            try {
                shutdown = SimpleEmailService.shutdown(30, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                log.warn("Interrupted while shutting down SimpleEmailService");
            }
            if (!shutdown) {
                SimpleEmailService.shutdownNow();
            }
        }

        // ErrorReporter
        if (config.getBoolean(PROP_ERROR_REPORTER_ENABLED, false)) {
            // Initialize the error reporting service
            ErrorReporter.configure(
                    config.getString(PROP_ERROR_REPORTER_TO),
                    new KeyValuePair<String,String>(config.getString(PROP_ERROR_REPORTER_FROM), "J2Free ErrorReporter")
                );
        } else {
            // Setting the TO and FROM to null essentially disables it
            ErrorReporter.configure(null, null);
        }

        // QueuedHttpCallService
        if (config.getBoolean(PROP_HTTP_SRVC_ON, false)) {
            if (!SimpleHttpService.isEnabled()) { // Don't double init...
                int defaultThreadCount = Runtime.getRuntime().availableProcessors() + 1;  // threads to use if unspecified
                SimpleHttpService.init(
                        config.getInt(PROP_HTTP_SRVC_CORE_POOL, defaultThreadCount),
                        config.getInt(PROP_HTTP_SRVC_MAX_POOL, defaultThreadCount),
                        config.getLong(PROP_HTTP_SRVC_POOL_IDLE, DEFAULT_HTTP_SRVC_THREAD_IDLE),
                        config.getInt(PROP_HTTP_SRVC_CONNECT_TOUT, DEFAULT_HTTP_SRVC_CONNECT_TOUT),
                        config.getInt(PROP_HTTP_SRVE_SOCKET_TOUT, DEFAULT_HTTP_SRVE_SOCKET_TOUT)
                    );
            }
        } else if (SimpleHttpService.isEnabled()) {
            
            boolean shutdown = false;
            try {
                // Try to shutdown the service while letting currently waiting tasks complete
                shutdown = SimpleHttpService.shutdown(30, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                log.warn("Interrupted while waiting for SimpleHttpService to shutdown");
            }
            if (!shutdown) {
                // But if that doesn't finish in 60 seconds, just cut it off
                int count = SimpleHttpService.shutdownNow().size();
                log.warn("SimpleHttpService failed to shutdown in 60 seconds, so it was terminated with " + count + " tasks waiting");
            }
        }

        // Spymemcached Client
        if (config.getBoolean(PROP_SPYMEMCACHED_ON,false)) {
            String addresses = config.getString(PROP_SPYMEMCACHED_ADDRESSES);
            if (addresses == null) {
                log.error("Error configuring spymemcached; enabled but no addresses!");
            } else {

                try {
                    // Reflect our way to the constructor, this is all so that the
                    // spymemcached jar does not need to be included in a J2Free app
                    // unless it is actually to be used.
                    Class klass = Class.forName("net.spy.memcached.MemcachedClient");
                    Constructor constructor = klass.getConstructor(List.class);

                    klass = Class.forName("net.spy.memcached.AddrUtil");
                    Method method = klass.getMethod("getAddresses", String.class);

                    Object client = constructor.newInstance(method.invoke(null, addresses));

                    context.setAttribute(CONTEXT_ATTR_SPYMEMCACHED, client);
                    loadedConfigPropKeys.add(CONTEXT_ATTR_SPYMEMCACHED);
                    
                    Global.put(CONTEXT_ATTR_SPYMEMCACHED, client);

                    log.info("Spymemcached client created, connected to " + addresses);

                } catch (Exception e) {
                    log.error("Error creating memcached client [addresses=" + addresses + "]", e);
                }
            }
        } else {

            // If a spymemcached client was previous created
            Object client = Global.get(CONTEXT_ATTR_SPYMEMCACHED);
            if (client != null) {

                try {
                    // Reflect our way to the shutdown method
                    Class klass   = Class.forName("net.spy.memcached.MemcachedClient");
                    Method method = klass.getMethod("shutdown");

                    method.invoke(null, client); // and shut it down

                    log.info("Spymemcached client shutdown");

                } catch (Exception e) {
                    log.error("Error shutting down spymemcached client", e);
                }
                
                // Then remove any references
                Global.remove(CONTEXT_ATTR_SPYMEMCACHED);
                client = null;
            }
        }
    }

    public synchronized void contextDestroyed(ServletContextEvent event) {
        clearConfiguration();
    }

    /**
     * Configures a J2Free application
     */
    private synchronized void reconfigure(Configuration config) throws ConfigurationException {
        clearConfiguration();
        configure(config);
    }

    private synchronized void clearConfiguration() {

        // Remove all properties from the ServletContext that were set in the
        // previous configuration.
        for (String key : loadedConfigPropKeys) {
            context.removeAttribute(key);
        }
        loadedConfigPropKeys.clear();

        // InvokerFilter
        InvokerFilter.reset(); // this clears all mappings and rescans
    }

    /**
     * Internal method for adding a servlet config
     *
     * @param config
     * @param pathProp
     * @param defaultPath
     * @param servletClass
     */
    private synchronized void addServletMapping(Configuration config, String pathProp, String defaultPath, Class<? extends HttpServlet> servletClass) {

        String path;
        Class  oldKlass;

        Iterator itr;

        List paths = config.getList(pathProp);

        if (paths == null) {
            oldKlass = InvokerFilter.addServletMapping(defaultPath, servletClass);
            if (oldKlass != null)
                log.error("Error mapping " + servletClass.getSimpleName() + ", " + oldKlass.getSimpleName() + " was alread mapped to " + defaultPath);
        } else {
            itr = paths.iterator();
            while (itr.hasNext()) {
                path = (String)itr.next();
                oldKlass = InvokerFilter.addServletMapping(path, servletClass);
                if (oldKlass != null)
                    log.error("Error mapping " + servletClass.getSimpleName() + ", " + oldKlass.getSimpleName() + " was alread mapped to " + path);
            }
        }
    }
    
}
