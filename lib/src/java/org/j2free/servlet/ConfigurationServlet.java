/*
 * ConfigurationServlet.java
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
package org.j2free.servlet;

import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.Constructor;

import java.net.InetAddress;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.cache.FragmentCache;
import org.j2free.email.EmailService;
import org.j2free.email.EmailService.ContentType;
import org.j2free.email.ErrorReporter;
import org.j2free.email.Template;
import org.j2free.http.QueuedHttpCallService;
import org.j2free.jsp.tags.FragmentCacheTag;
import org.j2free.servlet.filter.InvokerFilter;
import org.j2free.util.Global;
import org.j2free.util.KeyValuePair;
import org.j2free.util.Priority;

import static org.j2free.util.Constants.*;


/**
 * <tt>ConfigurationServlet</tt> should be loaded first by specifying
 * &lt;load-on-startup&gt;1&lt;/load-on-startup&gt; in web.xml.  This
 * servlet sets all context init params as context attributes.
 *
 * Additionally, <tt>ConfigurationServlet</tt> will look for a config
 * properties file in the default location, or a custom location if
 * the context init-param <tt>config-file</tt> is specified and, if
 * found, will set context-attributes for all properties.
 *
 * If a <tt>Configuration</tt> is found and successfully loaded, then
 * the following items will be configured accordingly:
 *  - <tt>RunMode</tt>
 *  - <tt>InvokerFilter</tt>
 *  - <tt>StaticJspServlet</tt>
 *  - <tt>LogoutServlet</tt>
 *  - <tt>ProxyServlet</tt>
 *  - <tt>J2FreeAdminServlet</tt>
 *  - <tt>FragmentCacheTag</tt>
 *  - <tt>FragmentCleaner</tt>
 *  - <tt>Task ScheduledExecutorService</tt>
 *  - <tt>EmailService</tt>
 *  - <tt>ErrorReporter</tt>
 *  - <tt>Spymemcached</tt>
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
 * @author ryan
 */
public class ConfigurationServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(ConfigurationServlet.class);

    @Override
    public void init() throws ServletException {
        super.init();

        ServletContext context = getServletContext();

        // Get the configuration file
        String configPath = (String)context.getInitParameter(INIT_PARAM_CONFIG_PATH);

        // Use the default path if it wasn't specified
        if (StringUtils.isBlank(configPath))
            configPath = DEFAULT_CONFIG_PATH;

        context.setAttribute(CONTEXT_ATTR_CONFIG_PATH, configPath);
        
        try {

            // Load the configuration
            Configuration config = new PropertiesConfiguration(configPath);

            // Save it where we can get at it later
            context.setAttribute(CONTEXT_ATTR_CONFIG, config);
            Global.put(CONTEXT_ATTR_CONFIG, config);

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

            // Set application context attributes for all config properties
            String prop, value;
            Iterator itr = config.getKeys();
            while (itr.hasNext()) {

                prop  = (String)itr.next();
                value = config.getString(prop);
                value = (value.equals("localhost") ? localhost : value);

                log.info("Setting application context attribute: [name=" + prop + ",value=" + value + "]");
                context.setAttribute(prop,value);
            }

            // Run Mode configuration
            String runMode = config.getString(PROP_RUNMODE);
            try {
                RUN_MODE = RunMode.valueOf(runMode);
            } catch (Exception e) {
                log.warn("Error setting runmode, invalid value: " + runMode);
            }

            context.setAttribute("devMode",RUN_MODE != RunMode.PRODUCTION);

            // InvokerFilter
            InvokerFilter.configure(
                    config.getString(PROP_INVOKER_BYPASSPATH,EMPTY),
                    config.getBoolean(PROP_INVOKER_BENCHMARK, false),
                    config.getInteger(PROP_LOCALPORT, null),
                    config.getInteger(PROP_LOCALPORT_SSL, null)
                );

            // StaticJspServlet
            if (config.getBoolean(PROP_STATICJSP_ON, false)) {

                String staticJspDir  = config == null ? DEFAULT_STATICJSP_DIR : config.getString(PROP_STATICJSP_DIR,DEFAULT_STATICJSP_DIR);
                String staticJspPath = config == null ? DEFAULT_STATICJSP_PATH : config.getString(PROP_STATICJSP_PATH,DEFAULT_STATICJSP_PATH);

                StaticJspServlet.directory.set(staticJspDir);

                Set<String> staticJsps = context.getResourcePaths(staticJspDir);
                if (staticJsps != null && !staticJsps.isEmpty()) {
                    for (String jsp : staticJsps) {
                        jsp = staticJspPath + jsp.replace(staticJspDir, EMPTY).replaceAll("\\.jsp$", EMPTY);
                        InvokerFilter.addServletMapping(jsp, StaticJspServlet.class);
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
                addServletMapping(config, PROP_SERVLET_ADMIN_PATH, DEFAULT_ADMIN_PATH, J2FreeAdminServlet.class);

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
            }

            // For Task execution
            if (config.getBoolean(PROP_TASK_EXECUTOR_ON,false)) {

                int threads = config.getInt(PROP_TASK_EXECUTOR_THREADS,DEFAULT_TASK_EXECUTOR_THREADS);

                ScheduledExecutorService taskExecutor;
                if (threads == 1)
                    taskExecutor = Executors.newSingleThreadScheduledExecutor();
                else
                    taskExecutor = Executors.newScheduledThreadPool(threads);
                
                context.setAttribute(CONTEXT_ATTR_TASK_MANAGER,taskExecutor);
                Global.put(CONTEXT_ATTR_TASK_MANAGER, taskExecutor);
            }

            // Email Service
            if (config.getBoolean(PROP_MAIL_SERVICE_ON, false)) {

                // Get the SMTP properties
                Properties props = System.getProperties();
                props.put(PROP_SMTP_HOST,config.getString(PROP_SMTP_HOST));
                props.put(PROP_SMTP_PORT,config.getString(PROP_SMTP_PORT));
                props.put(PROP_SMTP_AUTH,config.getString(PROP_SMTP_AUTH));

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
                EmailService.init(session, headers);

                // Set whether we actually send the e-mails
                EmailService.setDummyMode(
                        config.getBoolean(PROP_MAIL_DUMMY_MODE, false)
                    );

                // Set the failure policy
                String policy = config.getString(PROP_MAIL_ERROR_POLICY);
                if (policy != null) {

                    if (policy.equals(VALUE_MAIL_POLICY_DISCARD)) {

                        EmailService.setErrorPolicy(new EmailService.DiscardPolicy());

                    } else if (policy.equals(VALUE_MAIL_POLICY_REQUEUE)) {

                        Priority priority = null;
                        try {
                            priority = Priority.valueOf(config.getString(PROP_MAIL_REQUEUE_PRIORITY));
                        } catch (Exception e) {
                            log.warn("Error reading requeue policy priority: " + config.getString(PROP_MAIL_REQUEUE_PRIORITY,"") + ", using default");
                        }

                        if (priority == null) {
                            EmailService.setErrorPolicy(new EmailService.RequeuePolicy());
                        } else {
                            EmailService.setErrorPolicy(new EmailService.RequeuePolicy(priority));
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
                                    EmailService.registerTemplate(key, template, key.equals(defaultTemplate));
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

            // ErrorReporter
            if (config.getBoolean(PROP_ERROR_REPORTER_ENABLED, false)) {
                // Initialize the error reporting service
                ErrorReporter.init(
                        config.getString(PROP_ERROR_REPORTER_TO), 
                        new KeyValuePair<String,String>(config.getString(PROP_ERROR_REPORTER_FROM), "J2Free ErrorReporter")
                    );
            }

            // QueuedHttpCallService
            if (config.getBoolean(PROP_HTTP_SRVC_ON, false)) {
                int cpus = Runtime.getRuntime().availableProcessors();
                QueuedHttpCallService.enable(
                        config.getInt(PROP_HTTP_SRVC_MAX_POOL, cpus + 1),
                        DEFAULT_HTTP_SRVC_THREAD_IDLE, // In our thread pool, this won't ever happen
                        config.getInt(PROP_HTTP_SRVC_CONNECT_TOUT, DEFAULT_HTTP_SRVC_CONNECT_TOUT),
                        config.getInt(PROP_HTTP_SRVE_SOCKET_TOUT, DEFAULT_HTTP_SRVE_SOCKET_TOUT)
                    );
            }

            // Spymemcached Client
            if (config.getBoolean(PROP_SPYMEMCACHED_ON,false)) {
                String addresses = config.getString(PROP_SPYMEMCACHED_ADDRESSES);
                if (addresses == null) {
                    log.error("Error configuring spymemcached; enabled but no addresses!");
                } else {
                    try {
                        MemcachedClient client = new MemcachedClient(
                                                    //new BinaryConnectionFactory(),
                                                    AddrUtil.getAddresses(addresses)
                                                );
                        context.setAttribute(CONTEXT_ATTR_SPYMEMCACHED, client);
                        Global.put(CONTEXT_ATTR_SPYMEMCACHED, client);
                        log.info("Spymemcached client created, connected to " + addresses);
                    } catch (IOException ioe) {
                        log.error("Error creating memcached client [addresses=" + addresses + "]", ioe);
                    }
                }
            }

        } catch (ConfigurationException ce) {
            log.error("Error loading configuration",ce);
        }
        
    }

    private void addServletMapping(Configuration config, String pathProp, String defaultPath, Class<? extends HttpServlet> servletClass) {

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
