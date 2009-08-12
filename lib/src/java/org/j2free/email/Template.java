package org.j2free.email;

import net.jcip.annotations.Immutable;

/**
 * A wrapper for holding a template and it's corresponding
 * content-type.
 *
 * @author Ryan Wilson
 */
@Immutable
public class Template {

    public final String templateText;
    public final EmailService.ContentType contentType;

    public Template(String templateText, EmailService.ContentType contentType) {
        this.templateText = templateText;
        this.contentType = contentType;
    }
    
}
