/*
 * ErrorLogFilter.java
 *
 * Created on March 26, 2008, 8:23 PM
 */

package org.j2free.servlet.filter;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.j2free.etc.Constants;
import org.j2free.etc.Emailer;
import org.j2free.etc.ServletUtils;

import static org.j2free.etc.ServletUtils.*;

/**
 *
 * @author  ryan
 * @version
 */

public class ErrorLogFilter implements Filter {
    
    private static Log log = LogFactory.getLog(ErrorLogFilter.class);
    
    /* Ignored User-Agents
        panscient.com
        Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)
        nutchsearch/Nutch-0.9 (Nutch Search 1.0; herceg_novi at yahoo dot com)
        Yahoo! Slurp
        Powerset
        Ask Jeeves/Teoma
        msnbot
        Mozilla/5.0 (compatible; MJ12bot/v1.2.1; http://www.majestic12.co.uk/bot.php?+)
        Mozilla/5.0 (Twiceler-0.9 http://www.cuill.com/twiceler/robot.html)
        Gigabot/3.0 (http://www.gigablast.com/spider.html)
        Mozilla/5.0 (compatible; attributor/1.13.2 +http://www.attributor.com)
        lwp-request/2.07
    */
    private static final String IGNORED_AGENTS = 
            ".*?(" +
                "(spider|robot|bot)\\.[a-z]*?|" + 
                "panscient\\.com|" +
                "Googlebot|" +
                "nutchsearch|" +
                "Yahoo! Slurp|" +
                "powerset|" +
                "Ask Jeeves/Teoma|" +
                "msnbot|" +
                "Twiceler|" + 
                "attributor\\.com|" +
            ").*?";

    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured.
    private FilterConfig filterConfig = null;
    
    public ErrorLogFilter() {
    }
    
    /**
     *
     * @param request The servlet request we are processing
     * @param result The servlet response we are creating
     * @param chain The filter chain we are processing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        
        Throwable problem = null;
        
        HttpServletRequest request   = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)resp;
        
        HttpSession session = request.getSession(true);
        
        try {
            
            String currentPath = request.getRequestURI().replaceFirst(
                request.getContextPath(), "");

            if (currentPath.matches(".*?\\.(jpg|gif|png|jpeg)") && !currentPath.contains("captcha.jpg")) {
                response.setHeader("Cache-Control","max-age=3600");
                response.setHeader("Pragma","cache");
            } else if (currentPath.matches(".*?\\.(swf|js|css)")) {
                response.setHeader("Cache-Control","max-age=31449600");
                response.setHeader("Pragma","cache");
            }

            log.debug("ErrorLogFilter begin...");
            chain.doFilter(req,resp);
            log.debug("ErrorLogFilter end...");
        
        } catch (Exception e) {
            /*
            if (Constants.RUN_MODE == Constants.RUN_MODE_PRODUCTION) {
                
                String userAgent = request.getHeader("User-Agent");
                
                if (userAgent != null && !userAgent.matches(IGNORED_AGENTS)) {
                    Emailer mailer = Emailer.getInstance();
                    String exceptionReport = describeRequest(request) + "\n\nStack Trace:\n" + ServletUtils.throwableToString(e);
                    try {
                        mailer.sendPlain("ryan@foobrew.com,arjun@foobrew.com","Exception in FilterChain " + new Date().toString(),exceptionReport);
                    } catch (Exception e0) {
                        log.fatal("Error sending Exception report email. Content follows:\n\n" + exceptionReport);
                    }
                }
                
            } else {
                log.debug("Exception in FilterChain: DEVELOPMENT MODE",e);
            }
            */
            problem = e;
        }
        
        if (problem != null) {
            if (problem instanceof ServletException) throw (ServletException)problem;
            if (problem instanceof IOException) throw (IOException)problem;
        }
    }
    
    public void destroy() {
    }
    
    /**
     * Init method for this filter
     *
     */
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }
}
