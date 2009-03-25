/*
 * Created on Sep 15, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.captcha.obscurity;

import java.awt.image.BufferedImage;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface CaptchaEngine  {
    
    public abstract BufferedImage getDistortedImage(BufferedImage image, boolean noiseEnabled);
    
}
