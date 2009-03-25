/*
 * Created on Sep 14, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.captcha.text;

import java.awt.image.BufferedImage;
import java.util.Properties;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface WordRenederer {
    /**
     * Render a word to a BufferedImage.
     *
     * @param word The word to be rendered.
     * @param width The width of the image to be created.
     * @param height The heigth of the image to be created.
     * @return The BufferedImage created from the word,
     */
    public abstract BufferedImage renderWord(String word, int width, int height, int offsetTop);
    /**
     * @param properties
     */
    public abstract void setProperties(Properties properties);
}