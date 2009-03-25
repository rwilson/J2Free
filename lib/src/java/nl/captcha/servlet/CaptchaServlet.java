package nl.captcha.servlet;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.captcha.util.Helper;
import nl.captcha.servlet.CaptchaProducer;

/**
 * @version 	0.1
 * @author		testvoogd@hotmail.com
 */
public class CaptchaServlet extends HttpServlet implements Servlet {
    
    private Properties props = null;
    
    private CaptchaProducer captchaProducer = null;
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        
        // this key can be read from any controller to check wether user
        // is a computer or human..
        String capText = captchaProducer.createText();
        req.getSession().setAttribute(Constants.SIMPLE_CAPCHA_SESSION_KEY, capText);
        
        String simpleC =(String) req.getSession().getAttribute(Constants.SIMPLE_CAPCHA_SESSION_KEY);
        
        // notice we don't store the captext in the producer. This is because
        // the thing is not thread safe and we do use the producer as an instance
        // variable in the servlet.
        resp.setContentType("image/jpeg");
        captchaProducer.createImage(resp.getOutputStream(), capText);
        
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
        // init method should be thread safe so no
        // worries here...
        props = new Properties();
        Enumeration en = conf.getInitParameterNames();
        while (en.hasMoreElements()) {
            String key = (String)en.nextElement();
            String value = conf.getInitParameter(key);
            props.put(key, value);
        }
        
        this.captchaProducer = (CaptchaProducer) Helper.ThingFactory.loadImpl(Helper.ThingFactory.CPROD, props);
    }
    
}
