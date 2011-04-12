/*
 * PatternReplace.java
 *
 * Created on November 21, 2008, 12:01 AM
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

    /**
     * 
     * @param pattern
     */
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    /**
     * 
     * @param replace
     */
    public void setReplace(String replace)
    {
        this.replace = replace;
    }

    /**
     *
     * @return
     * @throws JspException
     */
    @Override
    public int doStartTag() throws JspException {
        return EVAL_BODY_BUFFERED;
    }

    /**
     *
     * @return
     * @throws JspException
     */
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

    /**
     *
     * @return
     * @throws JspException
     */
    @Override
    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }
}
