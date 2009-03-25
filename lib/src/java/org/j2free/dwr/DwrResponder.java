/*
 * DwrResponder.java
 *
 * Created on September 16, 2007, 1:50 PM
 *
 */

package org.j2free.dwr;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

/**
 *
 * @author ryan
 */
public class DwrResponder {
    
    protected static final Log LOG = LogFactory.getLog(DwrResponder.class);
    
    protected static boolean isLoggedIn() {
        WebContext ctx = WebContextFactory.get();
        
        HttpServletRequest request = ctx.getHttpServletRequest();
        
        return (request.getRemoteUser() != null);
    }
    
    protected static boolean isUserInRole(String role) {
        WebContext ctx = WebContextFactory.get();
        
        HttpServletRequest request = ctx.getHttpServletRequest();
        
        return (request.isUserInRole(role));
    }
    
    protected static String getRemoteUser() {
        return WebContextFactory.get().getHttpServletRequest().getRemoteUser();
    }
    
    protected static String getCaptchaCorrect() {
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpSession session = request.getSession(true);
        return (String)session.getAttribute(nl.captcha.servlet.Constants.SIMPLE_CAPCHA_SESSION_KEY);
    }
}