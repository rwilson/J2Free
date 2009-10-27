/*
 * RequireController.java
 *
 * Created on November 21, 2008, 12:01 AM
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

    private static final String ATTRIBUTE = "controller";

    private Controller controller;
    private HttpServletRequest request;
    
    private boolean closeTx;

    public RequireController() {
        closeTx = false;
    }

    @Override
    public int doStartTag() throws JspException {

        closeTx = false;

        request = (HttpServletRequest) pageContext.getRequest();

        controller = (Controller) request.getAttribute(ATTRIBUTE);

        if (controller == null) {
            
            controller = Controller.get();
            closeTx = true;

            try {
                controller.begin();
            } catch (Exception e) {
                return SKIP_BODY;
            }
            request.setAttribute(ATTRIBUTE, controller);
        }

        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {

        try {
            if (closeTx) {
                Controller.release(controller);
                request.removeAttribute(ATTRIBUTE);
            }
        } catch (Exception se) {
            throw new JspException(se);
        }

        return EVAL_PAGE;
    }

    @Override
    public void release() {
        closeTx = false;
        super.release();
    }

}
