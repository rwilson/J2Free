/*
 * SerializableMessage.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.email;

import java.io.Serializable;
import javax.mail.internet.InternetAddress;
import net.jcip.annotations.Immutable;

/**
 *
 * @author Ryan Wilson
 */
@Immutable
public class SerializableMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String fromAddress;
    public final String fromPersonal;

    public final String recipients;
    public final String subject;
    public final String body;
    public final String contentType;

    public final boolean ccSender;

    public SerializableMessage(InternetAddress from,
                               String recipients,
                               String subject,
                               String body,
                               String contentType,
                               boolean ccSender) {

        this(from.getAddress(), from.getPersonal(), recipients, subject, body, contentType, ccSender);
    }

    public SerializableMessage(String fromAddress, 
                               String fromPersonal,
                               String recipients,
                               String subject,
                               String body,
                               String contentType,
                               boolean ccSender) {
        
        this.fromAddress = fromAddress;
        this.fromPersonal = fromPersonal;
        this.recipients = recipients;
        this.subject = subject;
        this.body = body;
        this.contentType = contentType;
        this.ccSender = ccSender;
    }

    
}
