/*
 * EmailService.java
 *
 * Created on November 2, 2007, 9:42 AM
 *
 */

package org.j2free.util;

import java.io.UnsupportedEncodingException;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
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
    
    private static final ConcurrentMap<String,EmailService> instances = new ConcurrentHashMap<String,EmailService>(100,0.8f,100);
    
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
    
    private Map<String,String> templates;
    private final LinkedBlockingQueue<MimeMessage> requests;
    
    private boolean dummy;
    
    private Session session;

    /**
     * Creates a new <code>EmailService</code> using the provided session. Dummy mode is
     * disabled.
     *
     * @param session The session to use
     */
    public EmailService(Session session) {
        this(session,false);
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

        requests     = new LinkedBlockingQueue<MimeMessage>();
        templates    = Collections.synchronizedMap(new HashMap<String,String>());
        
        this.dummy = dummy;
        
        Runnable service = new Runnable() {
            public void run() {
                try {

                    /**
                     * NOTE: This is NOT busy waiting because requests.remove() blocks
                     *       until a request exists using wait() and notifyAll()
                     */
                    while (true)
                        send(requests.take());

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
        dummy = true;
    }

    /**
     *
     * @param key The key to store the template under
     * @param template The template for an e-mail
     */
    public void registerTemplate(String key, String template) {
        templates.put(key,template);
    }

    /**
     * Sends a plain text e-mail
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.MessagingException if there is an unexpected error sending the message
     */
    public void sendPlain(Pair<String,String> from, String to, String subject, String body)
        throws AddressException, MessagingException {
        sendPlain(from,to,subject,body,NO_CC);
    }
    
    /**
     * Sends a plain text e-mail
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.MessagingException if there is an unexpected error sending the message
     */
    public void sendPlain(Pair<String,String> from, String to, String subject, String body, boolean ccSender) 
        throws AddressException, MessagingException {
        try {
            send(new InternetAddress(from.getFirst(),from.getSecond()),to,subject,body,ContentType.PLAIN,ccSender);
        } catch (UnsupportedEncodingException uee) {
            send(new InternetAddress(from.getFirst()),to,subject,body,ContentType.PLAIN,ccSender);
        }
    }
    
    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail)
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.MessagingException if there is an unexpected error sending the message
     */
    public void sendHTML(Pair<String,String> from, String to, String subject, String body)
        throws AddressException, MessagingException {
        sendHTML(from,to,subject,body,NO_CC);
    }

    /**
     * Sends a HTML e-mail (note, there is no text equivalent, this is not multipart e-mail)
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param body The body of the message
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.MessagingException if there is an unexpected error sending the message
     */
    public void sendHTML(Pair<String,String> from, String to, String subject, String body, boolean ccSender) 
        throws AddressException, MessagingException {
        try {
            send(new InternetAddress(from.getFirst(),from.getSecond()),to,subject,body,ContentType.HTML,ccSender);
        } catch (UnsupportedEncodingException uee) {
            send(new InternetAddress(from.getFirst()),to,subject,body,ContentType.HTML,ccSender);
        }
    }

    /**
     * Sends a HTML e-mail based on a template
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new Pair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.MessagingException if there is an unexpected error sending the message
     */
    public void sendTemplate(Pair<String,String> from, String to, String subject, String templateKey, Pair<String,String> ... params)
        throws AddressException, MessagingException {
        sendTemplate(from,to,subject,templateKey,NO_CC,params);
    }

    /**
     * Sends a HTML e-mail based on a template
     *
     * @param from The "from" e-mail address
     * @param to The recipient's e-mail address
     * @param subject The subject of the e-mail
     * @param templateKey The key to look up the e-mail template
     * @param ccSender If true, cc's the from address on the e-mail sent, otherwise does not.
     * @param params An array of pairs of dynamic attributes for the template; i.e. for a template with dynamic section "body", <code>new Pair&lt;String,String&gt;("body",body);</code>
     * @throws javax.mail.internet.AddressException if the FROM or TO address is invalid
     * @throws javax.mail.MessagingException if there is an unexpected error sending the message
     */
    public void sendTemplate(Pair<String,String> from, String to, String subject, String templateKey, boolean ccSender, Pair<String,String> ... params) 
        throws AddressException, MessagingException {
        
        String body = templates.get(templateKey);
        
        for (Pair<String,String> param : params)
            body = body.replace("${" + param.getFirst() + "}",param.getSecond());
        
        try {
            send(new InternetAddress(from.getFirst(),from.getSecond()),to,subject,body,ContentType.HTML,ccSender);
        } catch (UnsupportedEncodingException uee) {
            send(new InternetAddress(from.getFirst()),to,subject,body,ContentType.HTML,ccSender);
        }
    }

    private void send(InternetAddress from, String recipients, String subject, String body, ContentType contentType, boolean ccSender) {
        
        if (dummy) {
            try {
                log.info("Skipping sending e-mail, dummy mode on. Would have sent: ['from:'" + from.getAddress() + "','subject':'" + subject + "','body':'" + body + "','ccSender':'" + ccSender + "','contentType':'" + contentType + "']");
            } catch(Exception e) { }
            return;
        }
        
        try {

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
            
            requests.offer(message);
            
        } catch (AddressException e) {
            log.error("Error creating MimeMessage",e);
        } catch (MessagingException e) {
            log.error("Error sending email",e);
        }
    }
    
    private void send(MimeMessage message) {
        try {
            Transport.send(message);
        } catch (MessagingException e) {
            log.error("Error sending email",e);
        }
    }
    
}