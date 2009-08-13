/*
 * EmailService.java
 *
 * Created on November 2, 2007, 9:42 AM
 *
 */

package org.j2free.email;

import java.io.UnsupportedEncodingException;

import java.util.Date;
import java.util.List;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

import net.spy.memcached.CASMutation;
import net.spy.memcached.CASMutator;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.util.Constants;
import org.j2free.util.HtmlFilter;
import org.j2free.util.KeyValuePair;
import org.j2free.util.PausableThreadPoolExecutor;
import org.j2free.util.Priority;
import org.j2free.util.PriorityFuture;
import org.j2free.util.PriorityReference;

/**
 * A service for sending e-mails that provides support for "instances"
 * encapsulating session properties, including authorization, as well
 * as templating and priority queuing of e-mails to send.
 *
 * @author Ryan Wilson
 */
@ThreadSafe
public final class EmailService {
    
    private static Log log = LogFactory.getLog(EmailService.class);
    
    private static final boolean NO_CC = false;

    public static enum ContentType {
        PLAIN("text/plain"),
        HTML("text/html");

        public final String value;

        ContentType(String value) {
            this.value = value;
        }

        public static ContentType valueOfExt(String ext) {
            if (ext.equals("txt")) {
                return PLAIN;
            } else if (ext.matches("htm?")) {
                return HTML;
            }

            throw new IllegalArgumentException("Unknown extension: " + ext);
        }
    };
    
    /***************************************************************************
     *
     *  static methods
     *
     */

    private static final ConcurrentMap<String,EmailService> instances = new ConcurrentHashMap<String,EmailService>();
    
    public static void registerInstance(String key, EmailService em) {
        instances.putIfAbsent(key,em);
    }
    
    public static EmailService getInstance(String key) {
        return instances.get(key);
    }

    private static final ReentrantLock lock    = new ReentrantLock();
    private static final Condition initialized = lock.newCondition();

    private static final AtomicBoolean dummy   = new AtomicBoolean(false);

    private static final AtomicBoolean initing = new AtomicBoolean(false);
    private static final AtomicBoolean running = new AtomicBoolean(false);

    public static void enableDummyMode() {
        log.info("Dummy mode enabled...");
        dummy.set(true);
    }

    public static void disableDummyMode() {
        log.info("Dummy mode disabled...");
        dummy.set(false);
    }
    
    private static volatile PausableThreadPoolExecutor executor;

    public static void initialize() {
        initialize(
            Constants.DEFAULT_MAIL_SERVICE_COREPOOL,
            Constants.DEFAULT_MAIL_SERVICE_MAXPOOL,
            Constants.DEFAULT_MAIL_SERVICE_KEEPALIVE
        );
    }

    /**
     * Uses non-blocking algorithm to avoid synchronizing.  This isn't really necessary to
     * be non-blocking, but it was a bit of an exercise in thinking about thread-safe CAS
     * algorithms and, done right, certainly can't hurt.
     *
     * @param corePoolSize @see java.util.concurrent.ThreadPoolExecutor
     * @param maxPoolSize @see java.util.concurrent.ThreadPoolExecutor
     * @param threadKeepAlive  @see java.util.concurrent.ThreadPoolExecutor
     */
    public static void initialize(int corePoolSize, int maxPoolSize, long threadKeepAlive) {

        if (initing.get() || running.get())
            return;

        // IF we don't succeed the CAS, just return since another thread must have done
        // it for us.
        if (!initing.compareAndSet(false, true))
            return;

        log.info("Initializing EmailService [core=" + corePoolSize + ",max=" + maxPoolSize + ",keep-alive=" + threadKeepAlive + "]");

        executor = new PausableThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                threadKeepAlive,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<Runnable>()
            );

        running.set(true);

        lock.lock();
        try {
            initialized.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public static void reconfigure(int corePoolSize, int maxPoolSize, long threadKeepAlive, TimeUnit unit) {
        log.info("Reconfiguring EmailService [core=" + corePoolSize + ",max=" + maxPoolSize + ",keep-alive=" + threadKeepAlive + "]");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaximumPoolSize(maxPoolSize);
        executor.setKeepAliveTime(threadKeepAlive, unit);
    }

    /**
     * Pauses the <tt>EmailService</tt> executor that actually sends the e-mails.  New messages
     * will still be accepted, but there is a risk of resource exhaustion if the service
     * is paused for too long.
     */
    public static void pause() {
        executor.pause();
        log.info("EmailService paused.");
    }

    /**
     * @return true if the <tt>EmailService</tt> is paused, otherwise false
     */
    public static boolean isPaused() {
        return executor.isPaused();
    }

    /**
     * Unpauses the <tt>EmailService</tt> executor to resume sending e-mails.
     */
    public static void unpause() {
        executor.unpause();
        log.info("EmailService unpaused.");
    }

    public static boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException {
        if (!running.get())
            throw new IllegalStateException("Illegal call to shutdown when the executor is not running.");

        log.info("EmailService shutting down...");
        executor.shutdown();
        return executor.awaitTermination(timeout, unit);
    }

    public static List<Runnable> shutdownNow() {
        if (running.get())
            throw new IllegalStateException("Illegal call to shutdown when the executor is not running.");

        log.info("EmailService shutting down...");
        return executor.shutdownNow();
    }

    private static class EmailSendTask implements Runnable {

        private final PriorityReference<MimeMessage> message;

        public EmailSendTask(PriorityReference<MimeMessage> message) {
            this.message = message;
        }

        public void run() {
            try {
                
                MimeMessage mime = message.get();

                if (dummy.get()) {
                    try {
                        log.info("Skipping send [dummy=true, priority=" + message.getPriority() + ", subject=" + mime.getSubject() + "]");
                        return;
                    } catch(Exception e) { }
                }
                
                Transport.send(mime);
                
            } catch (Exception e) {
                errorPolicy.get().handleException(message,e);
            }
        }
    }

    /**************************************************************************
     *
     * Policies for when a send fails
     *
     */
    private static final AtomicReference<ErrorPolicy> errorPolicy = new AtomicReference<ErrorPolicy>(new DiscardPolicy());

    public static void setErrorPolicy(ErrorPolicy policy) {
        log.info("EmailService configured to use error-policy: " + policy);
        errorPolicy.set(policy);
    }

    /**
     * Requeues the message.  If <tt>priority</tt> is null, at the original priority,
     * otherwise at <tt>priority</tt>.
     *
     * This method is exposed to allow subclasses of {@link ErrorPolicy} to requeue a message
     * without direct access to the executor or the prive {@link EmailSendTask}
     *
     * @param message
     */
    public static void requeue(PriorityReference<MimeMessage> message, Priority priority) {
        EmailSendTask task    = new EmailSendTask(message);
        PriorityFuture future = new PriorityFuture(Executors.callable(task), priority == null ? message.getPriority() : priority);
        executor.execute(future);
    }

    public static interface ErrorPolicy {
        public void handleException(PriorityReference<MimeMessage> message, Throwable t);
    }

    /**
     * Implements {@link ErrorPolicy} to discard failed messages writing only a message to the log.
     */
    public static final class DiscardPolicy implements ErrorPolicy {
        public void handleException(PriorityReference<MimeMessage> message, Throwable t) {
            log.error("Error sending e-mail", t);
        }

        @Override
        public String toString() {
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
     */
    public static final class RequeuePolicy implements ErrorPolicy {

        private final Priority priority;

        public RequeuePolicy() {
            this(null);
        }

        public RequeuePolicy(Priority priority) {
            this.priority = priority;
        }

        public void handleException(PriorityReference<MimeMessage> message, Throwable t) {
            log.warn("Error sending message, requeuing...");
            EmailService.requeue(message, priority);
        }

        @Override
        public String toString() {
            return "Requeue [priority=" + (priority == null ? "original" : priority) + "]";
        }
    }

    /**
     * Implements {@link ErrorPolicy} to requeue the message at the
     * specified {@link Priority} then pause the executor for the
     * <tt>interval</tt> amount of {@link TimeUnit}
     */
    public static final class RequeueAndPause implements ErrorPolicy {

        private final Priority priority;
        private final long     interval;
        private final TimeUnit unit;

        public RequeueAndPause() {
            this(null, Constants.DEFAULT_MAIL_RQAP_INTERVAL,TimeUnit.SECONDS);
        }

        public RequeueAndPause(Priority priority, long interval, TimeUnit unit) {
            this.priority = priority;
            this.interval = interval;
            this.unit     = unit;
        }

        public void handleException(PriorityReference<MimeMessage> message, Throwable t) {

            // Pause the executor
            executor.pause();

            // Requeue the message
            EmailService.requeue(message, priority);

            log.warn("Error sending message, requeuing, then pausing for " + interval + " " + unit);
            
            // Schedule a task to unpause the executor
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            service.schedule(
                    new Runnable() {
                        public void run() {
                            executor.unpause();
                        }
                    },
                    interval,
                    unit
                );
        }


        @Override
        public String toString() {
            return "RequeueAndPause [priority=" + (priority == null ? "original" : priority) + ", interval=" + interval + ", unit=" + unit + "]";
        }
    }
    
    /***************************************************************************
     *
     *  Instance Implementation
     *
     */

    private final ConcurrentMap<String, Template> templates;
    private final AtomicReference<String> defaultTemplate;

    private final MemcachedClient memcached;

    private final Session session;

    /**
     * Creates a new <code>EmailService</code> using the provided session
     * that stores messages in memory.
     *
     * @param session The session to use
     */
    public EmailService(Session session) {
        this(session, null);
    }

    /**
     * Creates a new <code>EmailService</code> using the provided session
     * that stores messages in memcached.
     *
     * @param session The session to use
     * @param memcached a live {@link MemcachedClient}
     */
    public EmailService(Session session, MemcachedClient memcached) {

        this.session    = session;
        this.memcached  = memcached;
        
        templates       = new ConcurrentHashMap<String,Template>();
        defaultTemplate = new AtomicReference<String>(Constants.EMPTY);
        
    }

    /**
     * This will register templates that are NOT the default
     *
     * @param key The key to store the template under
     * @param template The template for an e-mail
     */
    public void registerTemplate(String key, Template template) {
        registerTemplate(key,template,false);
    }

    /**
     *
     * @param key The key to store the template under
     * @param template The template for an e-mail
     * @param isDefault Whether this should be the default template when sendTemplate
     *        is called using the version without a template parameter
     */
    public void registerTemplate(String key, Template template, boolean isDefault) {
        log.info("Registering template: " + key + (isDefault ? " [DEFAULT]" : ""));
        templates.put(key, template);

        if (isDefault)
            defaultTemplate.set(key);
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
    public void sendPlain(KeyValuePair<String,String> from, String to, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException {
        
        sendPlain(from,to,subject,body,Priority.DEFAULT,NO_CC);
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
            throws AddressException, MessagingException, RejectedExecutionException {
        sendPlain(from,to,subject,body,priority,NO_CC);
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
            throws AddressException, MessagingException, RejectedExecutionException {
        try {
            send(new InternetAddress(from.key,from.value),to,subject,body,ContentType.PLAIN,priority,ccSender);
        } catch (UnsupportedEncodingException uee) {
            send(new InternetAddress(from.key),to,subject,body,ContentType.PLAIN,priority,ccSender);
        }
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
    public void sendHTML(KeyValuePair<String,String> from, String to, String subject, String body)
            throws AddressException, MessagingException, RejectedExecutionException {
        sendHTML(from,to,subject,body,Priority.DEFAULT,NO_CC);
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
            throws AddressException, MessagingException, RejectedExecutionException {
        sendHTML(from,to,subject,body,priority,NO_CC);
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
            throws AddressException, MessagingException, RejectedExecutionException {
        try {
            send(new InternetAddress(from.key,from.value),to,subject,body,ContentType.HTML,priority,ccSender);
        } catch (UnsupportedEncodingException uee) {
            send(new InternetAddress(from.key),to,subject,body,ContentType.HTML,priority,ccSender);
        }
    }

    /**
     * Sends a HTML e-mail based on the default template at the default priority
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new KeyValuePair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.IllegalArgumentException if the default template has not been set
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException, RejectedExecutionException {
        sendTemplate(from,to,subject,null,Priority.DEFAULT,NO_CC,params);
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
     * @throws javax.mail.IllegalArgumentException if the default template has not been set
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException, RejectedExecutionException {
        sendTemplate(from,to,subject,null,priority,NO_CC,params);
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
     * @throws javax.mail.IllegalArgumentException if the template does not exist
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, String templateKey, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException, RejectedExecutionException {
        sendTemplate(from,to,subject,templateKey,Priority.DEFAULT,NO_CC,params);
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
     * @throws javax.mail.IllegalArgumentException if the template does not exist
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, String templateKey, Priority priority, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException, RejectedExecutionException {
        sendTemplate(from,to,subject,templateKey,priority,NO_CC,params);
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
     * @throws javax.mail.IllegalArgumentException if the default template has not been set
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException, RejectedExecutionException {
        sendTemplate(from,to,subject,null,priority, ccSender,params);
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
     * @throws javax.mail.IllegalArgumentException if the template does not exist
     * @throws RejectedExecutionException if <tt>EmailService</tt> has already been shutdown
     */
    public void sendTemplate(KeyValuePair<String,String> from, String to, String subject, String templateKey, Priority priority, boolean ccSender, KeyValuePair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException, RejectedExecutionException {

        if (templateKey == null)
            templateKey = defaultTemplate.get();

        if (templateKey == null)
            throw new IllegalArgumentException("Template argument null but no default template is set!");

        Template template = templates.get(templateKey);

        if (template == null)
            throw new IllegalArgumentException("template: " + templateKey + " not found");
        
        String body = template.templateText;

        for (KeyValuePair<String,String> param : params)
            body = body.replace("${" + param.key + "}", param.value);

        try {
            send(new InternetAddress(from.key, from.value),to,subject,body,template.contentType,priority,ccSender);
        } catch (UnsupportedEncodingException uee) {
            send(new InternetAddress(from.key),to,subject,body,template.contentType,priority,ccSender);
        }
    }

    private void send(InternetAddress from, String recipients, String subject, String body, ContentType contentType, Priority priority, boolean ccSender)
            throws AddressException, MessagingException, RejectedExecutionException {

        final MimeMessage message = new MimeMessage(session);
        
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients, false));

        // CC the sender if they want, never CC the default from address
        if (ccSender)
            message.addRecipient(Message.RecipientType.CC,from);

        message.setReplyTo(new InternetAddress[] { from });

        message.setSubject(subject);
        message.setSentDate(new Date());

        if (contentType == ContentType.PLAIN) {

            // Just set the body as plain text
            message.setText(body, "UTF-8");
            message.setHeader("Content-Transfer-Encoding", "quoted-printable");

        } else {

            MimeMultipart multipart = new MimeMultipart("alternative");

            // Create the text part
            MimeBodyPart  text = new MimeBodyPart();
            text.setText(new HtmlFilter().filterForEmail(body), "UTF-8");
            text.setHeader("Content-Transfer-Encoding", "quoted-printable");

            // Add the text part
            multipart.addBodyPart(text);

            // Create the HTML portion
            MimeBodyPart  html = new MimeBodyPart();
            html.setContent(body,contentType.value);

            // Add the HTML portion
            multipart.addBodyPart(html);

            // set the message content
            message.setContent(multipart);

        }

        if (memcached != null) {

            final SerializableMessage sm = new SerializableMessage(from, recipients, subject, body, contentType.name(), ccSender);

            CASMutator<Map<String,SerializableMessage>> mutator = new CASMutator<Map<String,SerializableMessage>>(memcached, new SerializingTranscoder());

            CASMutation<Map<String,SerializableMessage>> mutation = new CASMutation<Map<String,SerializableMessage>>() {
                public Map<String,SerializableMessage> getNewValue(Map<String,SerializableMessage> current) {
                    current.put(message.getMessageID(), sm);
                    return current;
                }
            };
        }

        EmailSendTask task    = new EmailSendTask(new PriorityReference<MimeMessage>(message, priority));
        PriorityFuture future = new PriorityFuture(Executors.callable(task), priority);

        // Do this way down here to give another thread the chance to finishing initializing the executor
        if (!running.get())
            throw new IllegalStateException("EmailService has not been initialized!");

        // This will let these threads wait until the executor is initialized since it might be in the process
        lock.lock();
        try {
            while (executor == null)
                initialized.awaitUninterruptibly();
        } finally {
            lock.unlock();
        }
        
        executor.execute(future);
    }
}