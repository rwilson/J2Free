/*
 * Created on Sep 15, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.captcha.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import nl.captcha.obscurity.BackgroundProducer;
import nl.captcha.obscurity.GimpyEngine;
import nl.captcha.text.TextProducer;
import nl.captcha.text.WordRenederer;

/**
 * @author Administrator
 *
 * Classes implementing this interface will be responsible for
 * creating the base
 */
public interface CaptchaProducer {
	/**
	 * Create an image which have witten a distorted text, text given 
	 * as parameter. The result image is put on the output stream
	 * 
	 * @param stream the OutputStrea where the image is written
	 * @param text the distorted characters written on imagage
	 * @throws IOException if an error occurs during the image written on
	 * output stream.
	 */
	public abstract void createImage(OutputStream stream, String text)
		throws IOException;
	/* (non-Javadoc)
	 * @see nl.captcha.servlet.CaptchaProducer#setBackGroundImageProducer(nl.captcha.obscurity.BackgroundProducer)
	 */
	public abstract void setBackGroundImageProducer(BackgroundProducer background);
	/* (non-Javadoc)
	 * @see nl.captcha.servlet.CaptchaProducer#setObscurificator()
	 */
	public abstract void setObscurificator(GimpyEngine engine);
	/**
	 * @param properties
	 */
	public abstract void setProperties(Properties properties);
	
	public abstract void setTextProducer(TextProducer textP);
	
	public abstract String createText();
	
	public abstract void  setWordRenderer(WordRenederer renederer);
}