/*
 * Constants.java
 *
 * Created on April 5, 2008, 1:44 AM
 *
 * Copyright (c) 2008 Publi.us
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
 * @author Ryan Wilson (http://blog.augmentedfragments.com)
 */
public class Constants {

    // Run modes
    public static enum RunMode {
        LOCAL,       // Development with no network access
        DEVELOPMENT, // Enables network access
        PRODUCTION;

        public int getOrdinal() {
            return ordinal();
        }
    };


    // Context-attributes
    public static final String CONTEXT_ATTR_RUN_MODE           = "run-mode";
    public static final String CONTEXT_ATTR_EMAIL_TEMPLATE_DIR = "email-template-dir";

    // Default values for context-attributes
    public static final RunMode DEFAULT_RUN_MODE               = RunMode.PRODUCTION;
    
    public static final String DEFAULT_EMAIL_TEMPLATE_DIR      = "/WEB-INF/email-templates/";
    public static final String DEFAULT_STATIC_JSP_DIR          = "/WEB-INF/static-jsps/";
    public static final String DEFAULT_KNOWN_STATIC_PATH       = ".*?\\.(swf|flv)";

    // run mode
    public static volatile RunMode RUN_MODE = DEFAULT_RUN_MODE;

    // for time-based decay algorithms
    public static final long DECAY_EPOCH = 1134028003;

    // Useful constants
    public static final byte NULL_BYTE   = 0x0;
    public static final String UTF_16    = "UTF-16";
    public static final String EMPTY     = "";

    // JavaMail Properties
    public static final String SMTP_PROPERTY_HOST = "mail.smtp.host";
    public static final String SMTP_PROPERTY_PORT = "mail.smtp.port";
    public static final String SMTP_PROPERTY_AUTH = "mail.smtp.auth";

    // Static JSP location
    public static volatile String STATIC_JSP_CONTEXT_PATH = "/";
    public static volatile String STATIC_JSP_DIR          = DEFAULT_STATIC_JSP_DIR;
}
