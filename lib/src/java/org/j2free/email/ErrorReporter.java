/*
 * ErrorReporter.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.email;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
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
public class ErrorReporter {

    private static final Log log = LogFactory.getLog(ErrorReporter.class);

    private static final AtomicReference<String> TO
            = new AtomicReference<String>(null);

    private static final AtomicReference<KeyValuePair<String,String>> FROM
            = new AtomicReference<KeyValuePair<String,String>>(null);

    public static void init(String to, KeyValuePair<String, String> from) {
        TO.set(to);
        FROM.set(from);
    }

    public static void send(HttpServletRequest request, Throwable throwable) {
        send("JamLegend Error Report " + new Date().toString(), describeRequest(request) + "\n\nStack Trace:\n" + throwableToString(throwable));
    }

    public static void send(String subject, String body, Throwable throwable) {
        send(subject, body + "\n\nStack Trace:\n" + throwableToString(throwable));
    }

    /**
     * @param subject E-mail subject
     * @param message E-mail body
     * @throws IllegalStateException if the ErrorReporter has not been correctly initialized
     *         or if EmailService is not enabled.
     */
    public static void send(String subject, String message) throws IllegalStateException {

        if (TO.get() == null || FROM.get() == null)
            throw new IllegalStateException("ErrorReporter has not been properly initialized!");

        try {
            EmailService.sendPlain(
                FROM.get(),
                TO.get(),
                subject,
                message
            );
        } catch (Exception e0) {
            log.error("Error sending error report email. Content follows:\n\n" + message);
        }
    }
}
