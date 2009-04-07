/*
 * MailChimpWebhooksServlet.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.j2free.jpa.ControllerServlet;

import static org.j2free.util.ServletUtils.*;

/**
 *
 * @author ryan
 */
public class MailChimpWebhooksServlet extends ControllerServlet {

    private static final String SUBSCRIBE      = "subscribe";
    private static final String UNSUBSCRIBE    = "unsubscribe";
    private static final String PROFILE_UPDATE = "profile";
    private static final String EMAIL_UPDATE   = "upemail";
    private static final String CLEANED_EMAILS = "cleaned";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String type    = getStringParameter(request,"type",null);
        String firedAt = getStringParameter(request,"fired_at",null);

        if (type.equals(SUBSCRIBE)) {
            // don't care
        } else if (type.equals(UNSUBSCRIBE)) {

        } else if (type.equals(PROFILE_UPDATE)) {
            // don't care
        } else if (type.equals(EMAIL_UPDATE)) {

        } else if (type.equals(CLEANED_EMAILS)) {
            
        }
    }

}
