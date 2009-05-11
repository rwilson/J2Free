/*
 * EmailService.java
 *
 * Created on November 2, 2007, 9:42 AM
 *
 */

package org.j2free.email;

import java.io.UnsupportedEncodingException;

import java.util.Date;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.util.Constants;
import org.j2free.util.HtmlFilter;
import org.j2free.util.Pair;
import org.j2free.util.Priority;
import org.j2free.util.PriorityReference;

/**
 *
 * @author ryan
 */
public class EmailService {
    
    private static Log log = LogFactory.getLog(EmailService.class);
    
    private static final String CONTENT_TYPE_PLAIN = "text/plain";
    private static final String CONTENT_TYPE_HTML  = "text/html";
    
    private static final boolean CC_SENDER = true;
    private static final boolean NO_CC     = false;

    private static enum ContentType {
        PLAIN,
        HTML
    };
    
    /***************************************************************************
     *
     *  static convenience methods
     *
     */
    
    private static final ConcurrentMap<String,EmailService> instances = new ConcurrentHashMap<String,EmailService>();
    
    public static void registerInstance(String key, EmailService em) {
        instances.putIfAbsent(key,em);
    }
    
    public static EmailService getInstance(String key) {
        return instances.get(key);
    }
    
    /***************************************************************************
     *
     *  Instance Implementation
     *
     */
    
    private final ConcurrentMap<String,String> templates;
    private final PriorityBlockingQueue<PriorityReference<MimeMessage>> requests;

    private AtomicReference<String> defaultTemplate;

    private AtomicBoolean dummy;
    
    private Session session;

    /**
     * Creates a new <code>EmailService</code> using the provided session
     * setting dummy mode enabled for any RunMode except PRODUCTION.
     *
     * @param session The session to use
     */
    public EmailService(Session session) {
        this(session,Constants.RUN_MODE != Constants.RunMode.PRODUCTION);
    }

    /**
     * Creates a new <code>EmailService</code> using the provided session.
     *
     * @param session The session to use
     * @param dummy If true, dummy mode is enabled, otherwise false.  In dummy mode,
     *        the EmailService will not attempt to send e-mails and will dump what it would
     *        have sent to the log instead.
     */
    public EmailService(Session session, boolean dummy) {
        
        this.session = session;

        requests  = new PriorityBlockingQueue<PriorityReference<MimeMessage>>();
        templates = new ConcurrentHashMap<String,String>();
        
        defaultTemplate = new AtomicReference<String>(null);
        
        this.dummy = new AtomicBoolean(dummy);
        
        Runnable service = new Runnable() {
            public void run() {
                try {

                    PriorityReference<MimeMessage> message;
                    while (true) {
                        message = requests.take();

                        try {
                            Transport.send(message.get());
                        } catch (AuthenticationFailedException afe) {
                            log.warn("Authentication failed, re-queueing message...");
                            requests.put(message);
                        } catch (MessagingException me) {

                        }
                    }

                } catch (InterruptedException ie) {
                    log.debug("Emailer service interrupted, re-interrupting to exit...");
                    Thread.currentThread().interrupt();
                }
            }
        };
        new Thread(service).start();
    }

    /**
     *  If dummy mode is enabled, the mailer will not attempt to send any e-mails,
     *  but instead write to the log what it would have e-mailed.
     */
    public void enableDummy() {
        dummy.set(true);
    }

    /**
     * This will register templates that are NOT the default
     *
     * @param key The key to store the template under
     * @param template The template for an e-mail
     */
    public void registerTemplate(String key, String template) {
        registerTemplate(key,template,false);
    }

    /**
     *
     * @param key The key to store the template under
     * @param template The template for an e-mail
     * @param isDefault Whether this should be the default template when sendTemplate
     *        is called using the version without a template parameter
     */
    public void registerTemplate(String key, String template, boolean isDefault) {
        log.info("Registering template: " + key + (isDefault ? " [DEFAULT]" : ""));
        templates.put(key,template);

        if (isDefault) {
            defaultTemplate.set(key);
        }
    }

    /**
     * Sends a plain text e-mail at the default priority
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     */
    public void sendPlain(Pair<String,String> from, String to, String subject, String body) 
            throws AddressException, MessagingException {
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
     */
    public void sendPlain(Pair<String,String> from, String to, String subject, String body, Priority priority)
            throws AddressException, MessagingException {
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
     */
    public void sendPlain(Pair<String,String> from, String to, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException {
        try {
            send(new InternetAddress(from.getFirst(),from.getSecond()),to,subject,body,ContentType.PLAIN,priority,ccSender);
        } catch (UnsupportedEncodingException uee) {
            send(new InternetAddress(from.getFirst()),to,subject,body,ContentType.PLAIN,priority,ccSender);
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
     */
    public void sendHTML(Pair<String,String> from, String to, String subject, String body) 
            throws AddressException, MessagingException {
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
     */
    public void sendHTML(Pair<String,String> from, String to, String subject, String body, Priority priority)
            throws AddressException, MessagingException {
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
     */
    public void sendHTML(Pair<String,String> from, String to, String subject, String body, Priority priority, boolean ccSender)
            throws AddressException, MessagingException {
        try {
            send(new InternetAddress(from.getFirst(),from.getSecond()),to,subject,body,ContentType.HTML,priority,ccSender);
        } catch (UnsupportedEncodingException uee) {
            send(new InternetAddress(from.getFirst()),to,subject,body,ContentType.HTML,priority,ccSender);
        }
    }

    /**
     * Sends a HTML e-mail based on the default template at the default priority
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new Pair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.IllegalArgumentException if the default template has not been set
     */
    public void sendTemplate(Pair<String,String> from, String to, String subject, Pair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException {
        sendTemplate(from,to,subject,defaultTemplate.get(),Priority.DEFAULT,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on the default template
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param priority The priority this message should take if there are other queued messages
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new Pair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.IllegalArgumentException if the default template has not been set
     */
    public void sendTemplate(Pair<String,String> from, String to, String subject, Priority priority, Pair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException {
        sendTemplate(from,to,subject,defaultTemplate.get(),priority,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on a template at the defualt priority
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new Pair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.IllegalArgumentException if the template does not exist
     */
    public void sendTemplate(Pair<String,String> from, String to, String subject, String templateKey, Pair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException {
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
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new Pair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.IllegalArgumentException if the template does not exist
     */
    public void sendTemplate(Pair<String,String> from, String to, String subject, String templateKey, Priority priority, Pair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException {
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
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new Pair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.IllegalArgumentException if the default template has not been set
     */
    public void sendTemplate(Pair<String,String> from, String to, String subject, Priority priority, boolean ccSender, Pair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException {
        sendTemplate(from,to,subject,defaultTemplate.get(),priority, ccSender,params);
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
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new Pair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.IllegalArgumentException if the template does not exist
     */
    public void sendTemplate(Pair<String,String> from, String to, String subject, String templateKey, Priority priority, boolean ccSender, Pair<String,String> ... params)
            throws AddressException, MessagingException, IllegalArgumentException {
        
        String body = templates.get(templateKey);

        if (body == null)
            throw new IllegalArgumentException("template: " + templateKey + " not found");
        
        for (Pair<String,String> param : params)
            body = body.replace("${" + param.getFirst() + "}",param.getSecond());
        
        try {
            send(new InternetAddress(from.getFirst(),from.getSecond()),to,subject,body,ContentType.HTML,priority,ccSender);
        } catch (UnsupportedEncodingException uee) {
            send(new InternetAddress(from.getFirst()),to,subject,body,ContentType.HTML,priority,ccSender);
        }
    }

    private void send(InternetAddress from, String recipients, String subject, String body, ContentType contentType, Priority priority, boolean ccSender)
            throws AddressException, MessagingException {
        
        if (dummy.get()) {
            try {
                log.info("Skipping sending e-mail, dummy mode on. Would have sent: ['from:'" + from.getAddress() + "','subject':'" + subject + "','body':'" + body + "','ccSender':'" + ccSender + "','contentType':'" + contentType + "']");
            } catch(Exception e) { }
            return;
        }
        
        MimeMessage message = new MimeMessage(session);
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients, false));

        // CC the sender if they want, never CC the default from address
        if (ccSender)
            message.addRecipient(Message.RecipientType.CC,from);

        message.setSubject(subject);
        message.setSentDate(new Date());

        if (contentType == ContentType.PLAIN) {

            // Just set the body as plain text
            message.setContent(body, CONTENT_TYPE_PLAIN);

        } else {

            MimeMultipart multipart = new MimeMultipart("alternative");

            // Create the text part
            MimeBodyPart  text = new MimeBodyPart();
            text.setContent(new HtmlFilter().filterForEmail(body), CONTENT_TYPE_PLAIN);

            // Add the text part
            multipart.addBodyPart(text);

            // Create the HTML portion
            MimeBodyPart  html = new MimeBodyPart();
            html.setContent(body,CONTENT_TYPE_HTML);

            // Add the HTML portion
            multipart.addBodyPart(html);

            // set the message content
            message.setContent(multipart);

        }

        requests.offer(new PriorityReference(message,priority));
    }
    
}