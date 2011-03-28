/*
 * EmailService.java
 *
 * Copyright (c) 2011 FooBrew, Inc.
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
package org.j2free.email;

import java.io.UnsupportedEncodingException;

import java.util.Date;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.util.Constants;
import org.j2free.util.HtmlFilter;
import org.j2free.util.KeyValuePair;
import org.j2free.util.Priority;
import org.j2free.util.PriorityReference;
import org.j2free.util.ServletUtils;

/**
 * A service for sending e-mails that provides support for 
 * session properties, including authorization, templating,
 * and priority queuing.
 *
 * Currently the formation of a MimeMessage, including template
 * processing, is done in the calling thread; only the network
 * latency of sending is avoided by the queuing.  This could
 * be improved by moving the biz logic for constructing a
 * message into the task executed by the executor, thus allowing
 * the calling code to return more quickly.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public final class EmailService
{
    public static enum ContentType
    {
        PLAIN("text/plain"),
        HTML("text/html");

        private final String v;
        
        private ContentType(String v)
        {
            this.v = v;
        }

        public static ContentType valueOfExt(String ext)
        {
            if (ext.equals("txt")) {
                return PLAIN;
            } else if (ext.matches("htm?")) {
                return HTML;
            }

            throw new IllegalArgumentException("Unknown extension: " + ext);
        }

        @Override
        public String toString()
        {
            return v;
        }
    };
    
    private final Log log;

    private final boolean NO_CC = false;

    private final ExecutorService executor
            = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue());

    private final AtomicBoolean dummy   = new AtomicBoolean(false);

    private final ConcurrentMap<String, String> headers
            = new ConcurrentHashMap<String, String>();

    private final ConcurrentMap<String, Template> templates
            = new ConcurrentHashMap<String,Template>();

    private final AtomicReference<String> defaultTemplate
            = new AtomicReference<String>(Constants.EMPTY);

    private final Session session;

    private final AtomicReference<ErrorPolicy> errorPolicy = new AtomicReference<ErrorPolicy>(null);

    /**
     * Initialized this <code>EmailService</code> using the provided session
     * 
     * @param session The session to use
     */
    public EmailService(Session session)
    {
        if (session == null)
            throw new IllegalArgumentException("Error initializing EmailService, null session!");

        this.session = session;

        this.log = LogFactory.getLog(getClass());
    }

    /**
     * Sets the policy for handling errors when sending e-mails
     * 
     * @param policy The error policy to use
     * @see {@link ErrorPolicy}
     */
    public void setErrorPolicy(ErrorPolicy policy)
    {
        log.info("EmailService configured to use error-policy: " + policy);
        errorPolicy.set(policy);
    }

    /**
     * Sets the global headers to be used on every e-mail
     *
     * @param globalHseaders Message headers to be applied to each e-mail
     */
    public void setGlobalHeaders(List<KeyValuePair<String,String>> globalHeaders)
    {
        if (globalHeaders != null)
        {
            for (KeyValuePair<String,String> header : globalHeaders)
                headers.put(header.key, header.value);
        }
    }

    /**
     * Enables or disables "dummy" mode
     * 
     * @param isDummy
     */
    public void setDummyMode(boolean isDummy)
    {
        log.info(String.format("Dummy mode %s...", isDummy ? "enabled" : "disabled"));
        dummy.set(isDummy);
    }

    /**
     * @return true if "dummy" mode is enabled (meaning that e-mails will be accepted
     *         but not sent), otheriwse false
     */
    public boolean isDummy()
    {
        return dummy.get();
    }

    /**
     * Shutdown the service, first waiting the specified timeout for
     * previously accepted emails to be sent.  Note: new emails will
     * not be accepted after this method has been called.
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    public boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException
    {
        log.info("EmailService shutting down...");
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }

    /**
     * Shutdown the service immediately, cancelling remaining tasks
     */
    public void shutdown()
    {
        log.info("EmailService shutting down...");
        executor.shutdownNow();
    }


    // -------------------------------------------------------------------------- //
    //                                                                            //
    //                          TEMPLATE MANAGEMENT                               //
    //                                                                            //
    // -------------------------------------------------------------------------- //

    /**
     * This will register templates that are NOT the default
     *
     * @param key The key to store the template under
     * @param template The template for an e-mail
     */
    public void registerTemplate(String key, Template template)
    {
        registerTemplate(key,template,false);
    }

    /**
     *
     * @param key The key to store the template under
     * @param template The template for an e-mail
     * @param isDefault Whether this should be the default template when sendTemplate
     *        is called using the version without a template parameter
     */
    public void registerTemplate(String key, Template template, boolean isDefault)
    {
        log.info("Registering template: " + key + (isDefault ? " [DEFAULT]" : ""));
        templates.put(key, template);

        if (isDefault)
            defaultTemplate.set(key);
    }


    // -------------------------------------------------------------------------- //
    //                                                                            //
    //                          PLAIN TEXT MESSAGING                              //
    //                                                                            //
    // -------------------------------------------------------------------------- //

    /**
     * Sends a plain text e-mail at the default priority
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendPlain(KeyValuePair<String,String> from, String to, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        sendPlain(from,to,subject,body,Priority.DEFAULT,NO_CC);
    }
    
    /**
     * Sends a plain text e-mail at the default priority
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendPlain(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        sendPlain(from,recipients,subject,body,Priority.DEFAULT,NO_CC);
    }

    /**
     * Sends a plain text e-mail
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendPlain(KeyValuePair<String,String> from, String to, String subject, String body, Priority priority)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        sendPlain(from,to,subject,body,priority,NO_CC);
    }

    /**
     * Sends a plain text e-mail
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendPlain(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body, Priority priority)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        sendPlain(from,recipients,subject,body,priority,NO_CC);
    }

    /**
     * Sends a plain text e-mail
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendPlain(KeyValuePair<String,String> from, String to, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        try
        {
            send(new InternetAddress(from.key,from.value),to,subject,body,ContentType.PLAIN,priority,ccSender);
        } 
        catch (UnsupportedEncodingException uee)
        {
            send(new InternetAddress(from.key),to,subject,body,ContentType.PLAIN,priority,ccSender);
        }
    }
    
    /**
     * Sends a plain text e-mail
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendPlain(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        try
        {
            send(new InternetAddress(from.key,from.value),recipients,subject,body,ContentType.PLAIN,priority,ccSender);
        }
        catch (UnsupportedEncodingException uee)
        {
            send(new InternetAddress(from.key),recipients,subject,body,ContentType.PLAIN,priority,ccSender);
        }
    }

    
    // -------------------------------------------------------------------------- //
    //                                                                            //
    //                              HTML MESSAGING                                //
    //                                                                            //
    // -------------------------------------------------------------------------- //

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail) at the default priority
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendHTML(KeyValuePair<String,String> from, String to, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        sendHTML(from,to,subject,body,Priority.DEFAULT,NO_CC);
    }

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail) at the default priority
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendHTML(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        sendHTML(from,recipients,subject,body,Priority.DEFAULT,NO_CC);
    }

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail)
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendHTML(KeyValuePair<String,String> from, String to, String subject, String body, Priority priority)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        sendHTML(from,to,subject,body,priority,NO_CC);
    }

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail)
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendHTML(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body, Priority priority)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        sendHTML(from,recipients,subject,body,priority,NO_CC);
    }

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail)
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendHTML(KeyValuePair<String,String> from, String to, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        try
        {
            send(new InternetAddress(from.key,from.value),to,subject,body,ContentType.HTML,priority,ccSender);
        } 
        catch (UnsupportedEncodingException uee)
        {
            send(new InternetAddress(from.key),to,subject,body,ContentType.HTML,priority,ccSender);
        }
    }

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail)
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendHTML(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        try
        {
            send(new InternetAddress(from.key,from.value),recipients,subject,body,ContentType.HTML,priority,ccSender);
        }
        catch (UnsupportedEncodingException uee)
        {
            send(new InternetAddress(from.key),recipients,subject,body,ContentType.HTML,priority,ccSender);
        }
    }

    
    // -------------------------------------------------------------------------- //
    //                                                                            //
    //                          TEMPLATE MESSAGING                                //
    //                                                                            //
    // -------------------------------------------------------------------------- //

    /**
     * Sends a HTML e-mail based on the default template at the default priority
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the default template has not been set, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,to,subject,null,Priority.DEFAULT,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on the default template at the default priority
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the default template has not been set, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,recipients,subject,null,Priority.DEFAULT,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on the default template
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param priority The priority this message should take if there are other queued messages
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the default template has not been set, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,to,subject,null,priority,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on the default template
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param priority The priority this message should take if there are other queued messages
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the default template has not been set, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,recipients,subject,null,priority,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on a template at the defualt priority
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the specified template has not been registered, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, String templateKey, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,to,subject,templateKey,Priority.DEFAULT,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on a template at the defualt priority
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the specified template has not been registered, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String templateKey, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,recipients,subject,templateKey,Priority.DEFAULT,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on a template
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param priority The priority this message should take if there are other queued messages
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the specified template has not been registered, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, String templateKey, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,to,subject,templateKey,priority,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on a template
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param priority The priority this message should take if there are other queued messages
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the specified template has not been registered, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String templateKey, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,recipients,subject,templateKey,priority,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on the default template
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the default template has not been set, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,to,subject,null,priority, ccSender,params);
    }

    /**
     * Sends a HTML e-mail based on the default template
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the default template has not been set, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from,recipients,subject,null,priority, ccSender,params);
    }

    /**
     * Sends a HTML e-mail based on a template
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the specified template has not been registered, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, String templateKey, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        sendTemplate(from, InternetAddress.parse(to, false), subject, templateKey, priority, ccSender, params);
    }

    /**
     * Sends a HTML e-mail based on a template
     *
     * @param from The "from" e-mail address
     * @param recipients the message recipients
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the specified template has not been registered, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String templateKey, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, RejectedExecutionException, TemplateException
    {
        if (templateKey == null)
            templateKey = defaultTemplate.get();

        if (templateKey == null)
            throw new TemplateException("Template argument null but no default template is set!");

        Template template = templates.get(templateKey);

        if (template == null)
            throw new TemplateException("template: " + templateKey + " not found");
        
        String body = template.templateText;

        for (KeyValuePair<String,String> param : params)
            body = body.replace("${" + param.key + "}", param.value);

        Pattern pat = Pattern.compile("\\$\\{[-a-z_0-9/]+?\\}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pat.matcher(body);
        if (matcher.find())
        {
            TemplateException te = new TemplateException("Message contains unreplaced tokens");
            do
            {
                te.addToken(matcher.group());
            }
            while (matcher.find());
            throw te;
        }

        try
        {
            send(new InternetAddress(from.key, from.value), recipients, subject, body, template.contentType, priority, ccSender);
        } 
        catch (UnsupportedEncodingException uee)
        {
            send(new InternetAddress(from.key), recipients, subject, body, template.contentType, priority, ccSender);
        }
    }

    
    // -------------------------------------------------------------------------- //
    //                                                                            //
    //                          INTENRAL MESSAGING                                //
    //                                                                            //
    // -------------------------------------------------------------------------- //

    private void send(InternetAddress from, String recipients, String subject, String body, ContentType contentType, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        send(from, InternetAddress.parse(recipients, false), subject, body, contentType, priority, ccSender);
    }

    private void send(InternetAddress from, InternetAddress[] recipients, String subject, String body, ContentType contentType, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, recipients);

        for (Map.Entry<String,String> header : headers.entrySet())
            message.setHeader(header.getKey(), header.getValue());

        // CC the sender if they want
        if (ccSender)
            message.addRecipient(Message.RecipientType.CC,from);

        message.setReplyTo(new InternetAddress[] { from });

        message.setSubject(subject);
        message.setSentDate(new Date());

        if (contentType == ContentType.PLAIN)
        {
            // Just set the body as plain text
            message.setText(body, "UTF-8");
        } 
        else
        {
            MimeMultipart multipart = new MimeMultipart("alternative");

            // Create the text part
            MimeBodyPart  text = new MimeBodyPart();
            text.setText(new HtmlFilter().filterForEmail(body), "UTF-8");

            // Add the text part
            multipart.addBodyPart(text);

            // Create the HTML portion
            MimeBodyPart  html = new MimeBodyPart();
            html.setContent(body, ContentType.HTML.toString());

            // Add the HTML portion
            multipart.addBodyPart(html);

            // set the message content
            message.setContent(multipart);
        }
        enqueue(message, priority);
    }

    protected void enqueue(MimeMessage message, Priority priority)
    {
        executor.execute(
                new EmailSendTask(
                    new PriorityReference<MimeMessage>(message, priority)
                )
            );
    }
    
    /***************************************************************************
     *
     *  EmailSendTask definition
     *
     */
    private class EmailSendTask implements Runnable, Comparable<EmailSendTask>
    {
        private final long created;
        private final PriorityReference<MimeMessage> message;

        public EmailSendTask(PriorityReference<MimeMessage> message)
        {
            this.message = message;
            this.created = System.currentTimeMillis();
        }

        public void run()
        {
            try
            {
                MimeMessage mime = message.get();

                if (dummy.get())
                {
                    try
                    {
                        log.info(
                            String.format(
                                "Skipping send [dummy=true, priority=%s, recipients=%s, subject=%s]",
                                message.getPriority(),
                                ServletUtils.join(mime.getAllRecipients(), ","),
                                mime.getSubject()
                            )
                        );
                        return;
                    }
                    catch (Exception e) { }
                }
                else
                {
                    Transport.send(mime);
                }
            } 
            catch (Exception e)
            {
                ErrorPolicy policy = errorPolicy.get();
                if (policy != null)
                    policy.handleException(message, e);
                else
                    throw new RuntimeException("Error sending e-mail", e);
            }
        }

        public int compareTo(EmailSendTask other)
        {
            if (other == null)
                return 1;

            int cmp = this.message.getPriority().compareTo(other.message.getPriority());
            if (cmp != 0)
                return cmp;

            return Float.valueOf(Math.signum(other.created - this.created)).intValue();
        }
    }

    /***************************************************************************
     * Policy for when a send fails
     */
    public static interface ErrorPolicy
    {
        public void handleException(PriorityReference<MimeMessage> message, Throwable t);
    }
}