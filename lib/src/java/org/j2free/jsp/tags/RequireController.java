/*
 * RequireController.java
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.j2free.jpa.Controller;

/**
 * @author  Ryan Wilson
 */
public class RequireController extends TagSupport {

    private boolean closeTx;

    // Just used to hold the reference b/t start and end tags
    private Controller controller;

    /**
     * 
     */
    public RequireController()
    {
        closeTx = false;
    }

    /**
     *
     * @return
     * @throws JspException
     */
    @Override
    public int doStartTag() throws JspException {

        // defaults
        closeTx = false;
        controller = null;

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        try {
            
            /**
             * We need to know whether a controller already exists so we can
             * set closeTx appropriately.  So, first try to get a controller
             * with "create" false.
             */
            controller = Controller.get(false);

            /**
             * If the controller is null, then set closeTx to true and then
             * get a controller using "create" and "begin" true.
             */
            if (controller == null) {
                closeTx = true;
                controller = Controller.get();
                request.setAttribute(Controller.ATTRIBUTE_KEY, controller);
            }
            
        } catch (Exception e) { 
            // No need to do anything here, since controller will be null
            // and SKIP_BODY will be returned below.
        }

        if (controller != null) {
            return EVAL_BODY_INCLUDE;
        } else {
            return SKIP_BODY;
        }
    }

    /**
     *
     * @return
     * @throws JspException
     */
    @Override
    public int doEndTag() throws JspException {

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        if (closeTx && controller != null) {
            try {
                Controller.release();
                request.removeAttribute(Controller.ATTRIBUTE_KEY);
            } catch (Exception se) {
                throw new JspException(se);
            }
        }

        return EVAL_PAGE;
    }

    /**
     *
     */
    @Override
    public void release() {
        closeTx = false;
        controller = null;
        super.release();
    }
}
