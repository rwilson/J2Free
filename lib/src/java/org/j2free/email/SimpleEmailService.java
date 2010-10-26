/*
 * SimpleEmailService.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.email;

import java.util.List;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.j2free.email.EmailService.ErrorPolicy;
import org.j2free.util.KeyValuePair;
import org.j2free.util.Priority;
import org.j2free.util.PriorityReference;

/**
 * Static wrapper around a single EmailService
 *
 * @author Ryan Wilson
 */
public class SimpleEmailService
{
    private final Log log = LogFactory.getLog(SimpleEmailService.class);

    private static final AtomicReference<EmailService> instance = new AtomicReference(null);

    /**
     * Initializes this service to use the specified session
     * @param session
     */
    public static void init(Session session)
    {
        instance.set(new EmailService(session));
    }

    /**
     * @return true if this service is enabled, meaning that <tt>init</tt>
     *         has been called with a non-null session, otherwise false
     */
    public static boolean isEnabled()
    {
        return instance.get() != null;
    }

    /**
     * @throws an IllegalStateException if init(session) has not been called
     */
    public static void ensureInitialized()
    {
        if (!isEnabled())
            throw new IllegalStateException("SimpleEmailService has not been initialized!");
    }

    /**
     * Sets the policy for handling errors when sending e-mails
     *
     * @param policy The error policy to use
     * @see {@link ErrorPolicy}
     */
    public static void setErrorPolicy(ErrorPolicy policy)
    {
        ensureInitialized();
        instance.get().setErrorPolicy(policy);
    }

    /**
     * Sets the global headers to be used on every e-mail
     *
     * @param globalHseaders Message headers to be applied to each e-mail
     */
    public static void setGlobalHeaders(List<KeyValuePair<String,String>> globalHeaders)
    {
        ensureInitialized();
        instance.get().setGlobalHeaders(globalHeaders);
    }

    /**
     * Enables or disables "dummy" mode
     *
     * @param isDummy
     */
    public static void setDummyMode(boolean isDummy)
    {
        ensureInitialized();
        instance.get().setDummyMode(isDummy);
    }

    /**
     * @return true if "dummy" mode is enabled (meaning that e-mails will be accepted
     *         but not sent), otheriwse false
     */
    public static boolean isDummy()
    {
        ensureInitialized();
        return instance.get().isDummy();
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
    public static boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException
    {
        ensureInitialized();
        EmailService service = instance.get();
        boolean shutdown = service.shutdown(timeout, unit);
        if (shutdown)
        {
            instance.compareAndSet(service, null); // so it shows up as not enabled after shutdown
        }
        return shutdown;
    }

    /**
     * Shutdown the service immediately, cancelling remaining tasks
     */
    public static void shutdownNow()
    {
        ensureInitialized();
        EmailService service = instance.get();
        service.shutdownNow();
        instance.compareAndSet(service, null); // so it shows up as not enabled after shutdown
    }

    /**
     * This will register templates that are NOT the default
     *
     * @param key The key to store the template under
     * @param template The template for an e-mail
     */
    public static void registerTemplate(String key, Template template)
    {
        ensureInitialized();
        instance.get().registerTemplate(key, template);
    }

    /**
     *
     * @param key The key to store the template under
     * @param template The template for an e-mail
     * @param isDefault Whether this should be the default template when sendTemplate
     *        is called using the version without a template parameter
     */
    public static void registerTemplate(String key, Template template, boolean isDefault)
    {
        ensureInitialized();
        instance.get().registerTemplate(key, template, isDefault);
    }

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
    public static void sendPlain(KeyValuePair<String,String> from, String to, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendPlain(from, to, subject, body);
    }

    /**
     * Sends a plain text e-mail at the default priority
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendPlain(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendPlain(from, recipients, subject, body);
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
    public static void sendPlain(KeyValuePair<String,String> from, String to, String subject, String body, Priority priority)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendPlain(from, to, subject, body, priority);
    }

    /**
     * Sends a plain text e-mail
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendPlain(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body, Priority priority)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendPlain(from, recipients, subject, body, priority);
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
    public static void sendPlain(KeyValuePair<String,String> from, String to, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendPlain(from, to, subject, body, priority, ccSender);
    }

    /**
     * Sends a plain text e-mail
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendPlain(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendPlain(from, recipients, subject, body, priority, ccSender);
    }

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
    public static void sendHTML(KeyValuePair<String,String> from, String to, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendHTML(from, to, subject, body);
    }

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail) at the default priority
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendHTML(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendHTML(from, recipients, subject, body);
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
    public static void sendHTML(KeyValuePair<String,String> from, String to, String subject, String body, Priority priority)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendHTML(from, to, subject, body, priority);
    }

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail)
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendHTML(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body, Priority priority)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendHTML(from, recipients, subject, body, priority);
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
    public static void sendHTML(KeyValuePair<String,String> from, String to, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendHTML(from, to, subject, body, priority, ccSender);
    }

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail)
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendHTML(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendHTML(from, recipients, subject, body, priority, ccSender);
    }

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
    public static void sendTemplate(KeyValuePair<String,String> from, String to, String subject, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, to, subject, params);
    }

    /**
     * Sends a HTML e-mail based on the default template at the default priority
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the default template has not been set, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, recipients, subject, params);
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
    public static void sendTemplate(KeyValuePair<String,String> from, String to, String subject, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, to, subject, priority, params);
    }

    /**
     * Sends a HTML e-mail based on the default template
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param priority The priority this message should take if there are other queued messages
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the default template has not been set, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, recipients, subject, priority, params);
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
    public static void sendTemplate(KeyValuePair<String,String> from, String to, String subject, String templateKey, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, to, subject, templateKey, params);
    }

    /**
     * Sends a HTML e-mail based on a template at the defualt priority
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the specified template has not been registered, or there are unreplaced tokens 
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String templateKey, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, recipients, subject, templateKey, params);
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
    public static void sendTemplate(KeyValuePair<String,String> from, String to, String subject, String templateKey, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, to, subject, priority, true, params);
    }

    /**
     * Sends a HTML e-mail based on a template
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param priority The priority this message should take if there are other queued messages
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the specified template has not been registered, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String templateKey, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, recipients, subject, priority, true, params);
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
    public static void sendTemplate(KeyValuePair<String,String> from, String to, String subject, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, to, subject, priority, ccSender, params);
    }

    /**
     * Sends a HTML e-mail based on the default template
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the default template has not been set, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, recipients, subject, priority, ccSender, params);
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
    public static void sendTemplate(KeyValuePair<String,String> from, String to, String subject, String templateKey, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, to, subject, templateKey, priority, ccSender, params);
    }

    /**
     * Sends a HTML e-mail based on a template
     *
     * @param from The "from" e-mail address
     * @param to The recipients
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param priority The priority this message should take if there are other queued messages
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws TemplateException if the specified template has not been registered, or there are unreplaced tokens
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public static void sendTemplate(KeyValuePair<String,String> from, InternetAddress[] recipients, String subject, String templateKey, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, TemplateException, RejectedExecutionException
    {
        ensureInitialized();
        instance.get().sendTemplate(from, recipients, subject, templateKey, priority, ccSender, params);
    }

    /**
     * Implements {@link ErrorPolicy} to discard failed messages writing only a message to the log.
     */
    public static class DiscardPolicy implements ErrorPolicy
    {
        public void handleException(PriorityReference<MimeMessage> message, Throwable t)
        {
            LogFactory.getLog(getClass()).error("Error sending e-mail", t);
        }

        @Override
        public String toString()
        {
            return "Discard";
        }
    }

    /**
     * Implements {@link ErrorPolicy} to requeue the message at the specified {@link Priority}.
     *
     * Note: this policy includes a high risk of livelock if the requeue priority is above default.
     * The message will be requeued and could ascend immediately to the front of the queue.  If it
     * does, and continues to fail, EmailService will make little or no progress on the queue.
     *
     * WARNING: this policy does NOT take into account a max tries, so messages will be retried
     *          infinitely. Yes, that is a flaw.
     */
    public static class RequeuePolicy implements ErrorPolicy
    {
        private final Priority priority;

        /**
         * Equivalent to <pre>new RequeuePolicy(null)</pre>
         */
        public RequeuePolicy()
        {
            this(null);
        }

        /**
         * @param priority The priority to be used for requeued messages, null
         *        indicates the original priority should be used.
         */
        public RequeuePolicy(Priority priority)
        {
            this.priority = priority;
        }

        public void handleException(PriorityReference<MimeMessage> message, Throwable t)
        {
            LogFactory.getLog(getClass()).warn("Error sending message, requeuing...");
            if (isEnabled())
            {
                instance.get().enqueue(message.get(), priority == null ? message.getPriority() : priority);
            }
        }

        @Override
        public String toString()
        {
            return "Requeue [priority=" + (priority == null ? "original" : priority) + "]";
        }
    }
}