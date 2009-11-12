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

    private boolean closeTx;

    public RequireController() {
        closeTx = false;
    }

    @Override
    public int doStartTag() throws JspException {

        closeTx = false;

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        Controller controller = null;

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

    @Override
    public int doEndTag() throws JspException {

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        Controller controller = Controller.get(false);

        if (closeTx && controller != null) {
            try {
                Controller.release(controller);
                request.removeAttribute(Controller.ATTRIBUTE_KEY);
            } catch (Exception se) {
                throw new JspException(se);
            }
        }

        return EVAL_PAGE;
    }

    @Override
    public void release() {
        closeTx = false;
        super.release();
    }
}
