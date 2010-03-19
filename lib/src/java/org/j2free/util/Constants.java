/*
 * Constants.java
 *
 * Created on April 5, 2008, 1:44 AM
 *
 * Copyright (c) 2008 FooBrew, Inc.
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

package org.j2free.util;

/**
 * @author Ryan Wilson 
 */
public class Constants {

    // Run modes
    public static enum RunMode {
        LOCAL,       // Development with no network access
        DEVELOPMENT, // Enables network access
        PRODUCTION;

        // TODO Add Staging

        public int getOrdinal() {
            return ordinal();
        }
    };

    // run mode
    public static volatile RunMode RUN_MODE = RunMode.PRODUCTION;

    // Context init-params
    public static final String INIT_PARAM_CONFIG_PATH    = "config-file";
    public static final String CONTEXT_ATTR_CONFIG_PATH  = "config-file";

    // Context attributes
    public static final String CONTEXT_ATTR_CONFIG       = "j2free-config";
    public static final String CONTEXT_ATTR_TASK_MANAGER = "j2free-task-manager";
    public static final String CONTEXT_ATTR_SPYMEMCACHED = "j2free-spymemcached-client";

    // Useful constants
    public static final byte NULL_BYTE   = 0x0;
    public static final String UTF_8     = "UTF-8";
    public static final String UTF_16    = "UTF-16";
    public static final String EMPTY     = "";
    public static final String SPACE     = " ";

    // For validating e-mails
    public static final String EMAIL_REGEX                 = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:[A-Za-z]{2}|edu|com|org|net|gov|biz|info|name|aero|biz|info|jobs|museum)$";

    // For dynamic reconfiguration
    public static final String PROP_RECONFIG_ENABLED       = "dynamic-reconfig.enabled";
    public static final String PROP_RECONFIG_INTERVAL      = "dynamic-reconfig.interval";

    // For setting the application context value of "localhost"
    public static final String PROP_LOCALHOST              = "local.host";
    public static final String PROP_LOCALPORT              = "local.port";
    public static final String PROP_LOCALPORT_SSL          = "local.port.ssl";

    // SMTP Properties
    public static final String PROP_SMTP_HOST              = "mail.smtp.host";
    public static final String PROP_SMTP_PORT              = "mail.smtp.port";
    public static final String PROP_SMTP_AUTH              = "mail.smtp.auth";
    public static final String PROP_SMTP_USER              = "mail.smtp.user";
    public static final String PROP_SMTP_PASS              = "mail.smtp.pass";
    public static final String PROP_MAIL_HEADER_PREFIX     = "mail.header.";

    // EmailService config
    public static final String PROP_MAIL_SERVICE_ON        = "mail.service.enabled";
    public static final String PROP_MAIL_ERROR_POLICY      = "mail.service.error-policy";
    public static final String PROP_MAIL_REQUEUE_PRIORITY  = "mail.service.error-policy.requeue.priority";

    public static final String VALUE_MAIL_POLICY_DISCARD   = "discard";
    public static final String VALUE_MAIL_POLICY_REQUEUE   = "requeue";

    public static final String PROP_MAIL_DUMMY_MODE        = "mail.service.dummy.enabled";
    public static final String PROP_MAIL_TEMPLATE_DIR      = "mail.service.template.dir";
    public static final String PROP_MAIL_DEFAULT_TEMPLATE  = "mail.service.template.default";

    public static final String PROP_RUNMODE                = "run-mode";

    // ErrorReporter config
    public static final String PROP_ERROR_REPORTER_TO      = "error.reporter.to";
    public static final String PROP_ERROR_REPORTER_FROM    = "error.reporter.from";

    // Task Execution config
    public static final String PROP_TASK_EXECUTOR_ON       = "task.executor.enabled";
    public static final String PROP_TASK_EXECUTOR_THREADS  = "task.executor.threads";

    // Http Call Service
    public static final String PROP_HTTP_SRVC_ON           = "http.service.enabled";
    public static final String PROP_HTTP_SRVC_CORE_POOL    = "http.service.core-threads";
    public static final String PROP_HTTP_SRVC_MAX_POOL     = "http.service.max-threads";
    public static final String PROP_HTTP_SRVC_POOL_IDLE    = "http.service.thread-idle";
    public static final String PROP_HTTP_SRVC_CONNECT_TOUT = "http.client.connect.timeout";
    public static final String PROP_HTTP_SRVE_SOCKET_TOUT  = "http.client.socket.timeout";

    public static final String PROP_DECAY_EPOCH            = "decay.epoch";

    // Spymemcached Config
    public static final String PROP_SPYMEMCACHED_ON           = "spymemcached.enabled";
    public static final String PROP_SPYMEMCACHED_ADDRESSES    = "spymemcached.addresses";

    // Servlet Config
    public static final String PROP_STATICJSP_ON              = "servlet.static-jsp.enabled";
    public static final String PROP_STATICJSP_DIR             = "servlet.static-jsp.dir";
    public static final String PROP_STATICJSP_PATH            = "servlet.static-jsp.path";
    
    public static final String PROP_SERVLET_LOGOUT_ON         = "servlet.logout.enabled";
    public static final String PROP_SERVLET_LOGOUT_PATH       = "servlet.logout.path";

    public static final String PROP_SERVLET_PROXY_ON          = "servlet.proxy.enabled";
    public static final String PROP_SERVLET_PROXY_PATH        = "servlet.proxy.path";

    public static final String PROP_SERVLET_ADMIN_ON          = "servlet.admin.enabled";
    public static final String PROP_SERVLET_ADMIN_PATH        = "servlet.admin.path";

    // Default config file location
    public static final String DEFAULT_CONFIG_PATH            = "config.xml";

    // Dynamic reconfig default interval
    public static final int DEFAULT_RECONFIG_INTERVAL         = 300;

    // EmailService defaults
    public static final String DEFAULT_EMAIL_TEMPLATE_DIR     = "/WEB-INF/email-templates/";

    public static final int  DEFAULT_MAIL_SERVICE_MAXPOOL     = 10;
    public static final int  DEFAULT_MAIL_SERVICE_COREPOOL    = DEFAULT_MAIL_SERVICE_MAXPOOL / 2;
    public static final long DEFAULT_MAIL_SERVICE_KEEPALIVE   = 180;

    // Http Service defaults
    public static final long DEFAULT_HTTP_SRVC_THREAD_IDLE    = 300;
    public static final int  DEFAULT_HTTP_SRVC_CONNECT_TOUT   = 30;
    public static final int  DEFAULT_HTTP_SRVE_SOCKET_TOUT    = 30;

    // Servlet defaults
    // Can use , to separate multiple paths
    public static final String DEFAULT_STATICJSP_PATH         = "/";
    public static final String DEFAULT_LOGOUT_PATH            = "/logout";
    public static final String DEFAULT_PROXY_PATH             = "/proxy/*";
    public static final String DEFAULT_SECURE_PATH            = "/secure/*";
    public static final String DEFAULT_ADMIN_PATH             = "/j2free,/j2free/*";

    public static final String DEFAULT_STATICJSP_DIR          = "/WEB-INF/static-jsps/";

    // Task execution defaults
    public static final int DEFAULT_TASK_EXECUTOR_THREADS     = 3;

    // Fragment defaults
    public static final long DEFAULT_FRAGMENT_CLEANER_INTERVAL =  15 * 60; // In Seconds
}
