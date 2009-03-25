package nl.captcha.servlet;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.AttributedString;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Random;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.captcha.text.*;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * @version 	0.1
 * @author		testvoogd@hotmail.com
 */
public class Captcha extends HttpServlet implements Servlet {

	
	private final static String SIMPLE_CAPCHA_SESSION_KEY = "SIMPLE_CAPCHA_SESSION_KEY";
	
	public final static String SIMPLE_CAPCHA_TEXTPRODUCER = "cap.text.producer";
	public final static String SIMPLE_CAPTCHA_TEXTPRODUCER_CHARR = "cap.char.arr";
	public final static String SIMPLE_CAPTCHA_TEXTPRODUCER_CHARRL = "cap.char.arr.l";
	public final static String SIMPLE_CAPTCHA_TEXTPRODUCER_FONTA = "cap.font.arr";
	public final static String SIMPLE_CAPTCHA_TEXTPRODUCER_FONTS = "cap.font.size";
	public final static String SIMPLE_CAPTCHA_TEXTPRODUCER_FONTC = "cap.font.color";
	
	
	public final static String SIMPLE_CAPTCHA_PRODUCER = "cap.producer";
	public final static String SIMPLE_CAPTCHA_OBSCURIFICATOR = "cap.obscurificator";
	public final static String SIMPLE_CAPTCHA_BOX = "cap.border";
	public final static String SIMPLE_CAPTCHA_BOX_C = "cap.border.c";
	public final static String SIMPLE_CAPTCHA_BOX_TH = "cap.border.th";

	
	private Properties props = null;
	private TextProducer textProducer = null;
	private CaptchaProducer captcha = null;

	

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
			
		 
			
		// this key can be read from any controller to check wether user
		// is a computer or human..
		String capText = textProducer.getText();
		req.getSession().setAttribute(Captcha.SIMPLE_CAPCHA_SESSION_KEY, capText);
	

		CaptchaProducer p = new DefaultCaptchaIml(props);
		
		p.createImage(resp.getOutputStream(), capText);

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
		
		if (props.containsKey(SIMPLE_CAPCHA_TEXTPRODUCER)) {
			String producer = props.getProperty(SIMPLE_CAPCHA_TEXTPRODUCER);
			if (producer != null && !producer.equals("")) {
				try {
					textProducer = (TextProducer) Class.forName(producer).newInstance();
					textProducer.setProperties(props);
				}catch (Exception e) {}
			}
			if (textProducer == null) {
				textProducer = new DefaultTextCreator();
				textProducer.setProperties(props);
			}
		}
		
		if (props.containsKey(SIMPLE_CAPTCHA_PRODUCER)) {
			String producer = props.getProperty(SIMPLE_CAPTCHA_PRODUCER);
			if (producer != null && !producer.equals("")) {
				try {
					captcha = (CaptchaProducer) Class.forName(producer).newInstance();
					captcha.setProperties(props);
				}catch (Exception e) {}
			}
			if (captcha == null) {
				captcha = new DefaultCaptchaIml(props);
			}
		}
		
		
		
	}
	
 

}
