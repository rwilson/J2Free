/*
 * Created on Sep 15, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.captcha.obscurity;

import java.awt.image.BufferedImage;
import java.util.Properties;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface NoiseProducer {
	public abstract void setProperties(Properties props);
	public abstract void makeNoise(
		BufferedImage image,
		float factorOne,
		float factorTwo,
		float factorThree,
		float factorFour);
}