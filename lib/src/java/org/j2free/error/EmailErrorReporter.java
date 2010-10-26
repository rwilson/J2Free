/*
 * EmailErrorReporter.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.error;

import org.j2free.email.*;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.j2free.util.KeyValuePair;

import static org.j2free.util.ServletUtils.*;

/**
 * Utitility class for static calls to send error reports.
 *
 * @author Ryan Wilson
 */
public class EmailErrorReporter
{
    private final Log log = LogFactory.getLog(EmailErrorReporter.class);

    private final String to;
    private final KeyValuePair<String,String> from;

    public EmailErrorReporter(String to, KeyValuePair<String, String> from)
    {
        this.to = to;
        this.from = from;
    }

    public void send(HttpServletRequest request, Throwable throwable)
    {
        send("JamLegend Error Report " + new Date().toString(), describeRequest(request) + "\n\nStack Trace:\n" + throwableToString(throwable));
    }

    public void send(String subject, String body, Throwable throwable)
    {
        send(subject, body + "\n\nStack Trace:\n" + throwableToString(throwable));
    }

    /**
     * @param subject E-mail subject
     * @param message E-mail body
     * @throws IllegalStateException if the EmailErrorReporter has not been correctly initialized
     *         or if EmailService is not enabled.
     */
    public void send(String subject, String message)
    {
        try
        {
            SimpleEmailService.sendPlain(from, to, subject, message);
        } 
        catch (Exception e0)
        {
            log.error("Error sending error report email. Content follows:\n\n" + message);
        }
    }
}
