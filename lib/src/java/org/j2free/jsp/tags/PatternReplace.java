/*
 * PatternReplace.java
 *
 * Created on November 21, 2008, 12:01 AM
 */
package org.j2free.jsp.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * Tag that will execute a replaceAll on the body
 * after evaluation.  Useful for formatting code
 * with more whitespace or line-breaks for readability
 * when editing but having that formatting removed
 * when the page is generated.
 *
 * @author  Ryan Wilson
 * @version
 */
public class PatternReplace extends BodyTagSupport {

    private String pattern;
    private String replace;

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setReplace(String replace) {
        this.replace = replace;
    }

    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doAfterBody() throws JspException {

        BodyContent body = getBodyContent();

        // Execute the replaceAll
        String content   = body.getString()
                               .replaceAll(pattern, replace);

        // Attempt to write the body to the page
        try {
            body.getEnclosingWriter().write(content);
        } catch (IOException e) {
            throw new JspException(e.getCause());
        }

        return SKIP_BODY;
    }

    @Override
    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }
}
