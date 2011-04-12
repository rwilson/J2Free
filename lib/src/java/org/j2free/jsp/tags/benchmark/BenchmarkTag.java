/*
 * BenchmarkTag.java
 *
 * Created on October 3, 2008, 9:11 AM
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.jsp.tags.benchmark;

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author  Ryan Wilson
 * @version
 */

public class BenchmarkTag extends TagSupport {
    
    private final Log log = LogFactory.getLog(BenchmarkTag.class);
    
    /* Attributes */
    private boolean disable;
    private boolean comments;
    private boolean logs;
    private String  name;

    /**
     * 
     * @param disable
     */
    public void setDisable(boolean disable)
    {
        this.disable = disable;
    }

    /**
     * 
     * @param comments
     */
    public void setComments(boolean comments)
    {
        this.comments = comments;
    }
    
    /**
     * 
     * @param logs
     */
    public void setLogs(boolean logs)
    {
        this.logs = logs;
    }
    
    /**
     * 
     * @param name
     */
    public void setName(String name)
    {
        this.name = name;
    }
    
    /* Instance fields */
    private long tick;
    
    /**
     *
     * @return
     * @throws JspException
     */
    @Override
    public int doStartTag() throws JspException {

        if (!disable) {
            tick = System.currentTimeMillis();
        }

        return EVAL_BODY_INCLUDE;
    }

    /**
     *
     * @return
     * @throws JspException
     */
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
