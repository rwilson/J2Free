/*
 * BenchmarkTag.java
 *
 * Created on October 3, 2008, 9:11 AM
 */

package org.j2free.jsp.tags.benchmark;

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author  ryan
 * @version
 */

public class BenchmarkTag extends TagSupport {
    
    private final Log log = LogFactory.getLog(BenchmarkTag.class);
    
    /* Attributes */
    private boolean disable;
    private boolean comments;
    private boolean logs;
    private String  name;

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

    public void setComments(boolean comments) {
        this.comments = comments;
    }
    
    public void setLogs(boolean logs) {
        this.logs = logs;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    /* Instance fields */
    private long tick;
    
    @Override
    public int doStartTag() throws JspException {

        if (!disable) {
            tick = System.currentTimeMillis();
        }

        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {

        if (disable) {
            return EVAL_PAGE;
        }

        tick = System.currentTimeMillis() - tick;
        
        String result = (name != null && !"".equals(name)) ? name + ": " + tick + "ms" : tick + "ms";
        
        if (comments) {
            try {
                pageContext.getOut().println("<!-- " + result + " -->");
            } catch (IOException ex) {
                log.warn("Error writing benchmark output to page: " + result);
            }
        }
        
        if (logs) {
            log.info(result);
        }
        
        return EVAL_PAGE;
    }
}
