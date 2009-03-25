/*
 * Created on Sep 14, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.captcha.text.imp;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Properties;
import java.util.Random;

import nl.captcha.servlet.Constants;
import nl.captcha.text.WordRenederer;
import nl.captcha.util.Helper;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DefaultWordRenderer implements WordRenederer {
    
    private Properties props = null;
    
    public DefaultWordRenderer(Properties props) {
        this.props =props;
    }
    
    public DefaultWordRenderer() {
        
    }
    
    /**
     * Render a word to a BufferedImage.
     *
     * @param word The word to be rendered.
     * @param width The width of the image to be created.
     * @param height The heigth of the image to be created.
     * @return The BufferedImage created from the word,
     */
    public  BufferedImage renderWord(String word, int width, int height, int offsetTop) {

        boolean autoSizeWidth = width < 0;
        
        BufferedImage image = new BufferedImage(autoSizeWidth ? 1000 : width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = image.createGraphics();
        
        // This puts guides at 50% V and H
        /*
        graphics.setColor(Color.black);
        graphics.drawLine(width/2,0,width/2,height);
        graphics.drawLine(0,height/2,width,height/2);
        */
        
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        hints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        
        graphics.setRenderingHints(hints);
        
        Font[] fonts = Helper.getFonts(props);
        
        Random generator = new Random();
        
        char[] characters = word.toCharArray();
        
        Color fontColor = Helper.getColor(props.getProperty(Constants.SIMPLE_CAPTCHA_TEXTPRODUCER_FONT_COLOR),Color.black);
        graphics.setColor(fontColor);
        
        FontRenderContext frc = graphics.getFontRenderContext();
        
        double wordWidth   = 0d;
        
        Font[] fontChoices = new Font[characters.length];
        
        // Do everyting but drawing the first time through
        // The whole point of this loop is to calculate the width of the word
        for (int i = 0; i < characters.length; i++) {
            
            char[] itchar  = new char[]{ characters[i] };
            
            int choiceFont = generator.nextInt(fonts.length);
            
            Font font = fonts[choiceFont];
            fontChoices[i] = font;
            graphics.setFont(font);
            
            GlyphVector glyphVector = font.createGlyphVector(frc, itchar);
            
            wordWidth += glyphVector.getVisualBounds().getWidth() + 2;
        }
        
        // set X position for the first letter to be the vertical center minus half the word width
        // to create equal padding on the left and right.
        // If the configuration provides for a word length to large for the image size, the word will
        // still be positioned equally out of the picture.
        int x = autoSizeWidth ? 10 : (width / 2) - (int)(wordWidth / 2);
        int y;
        
        // This time we're going to draw
        for (int i = 0; i < characters.length; i++) {
            
            char[] itchar  = new char[]{ characters[i] };
            
            Font font = fontChoices[i];
            graphics.setFont(font);
            
            GlyphVector glyphVector  = font.createGlyphVector(frc, itchar);
            Rectangle2D visualBounds = glyphVector.getVisualBounds();
            
            // set default Y
            y = (height / 2) + (int)(visualBounds.getHeight() / 2);
            
            // Make sure the char is in the image
            if (y + visualBounds.getHeight() > height) y = height - 10;
            
            graphics.drawChars(itchar,0,itchar.length,x,y + offsetTop);

            x += (int)visualBounds.getWidth() + 2;
        }

        return autoSizeWidth ? image.getSubimage(0,0,(int)wordWidth+20,height) : image;
    }
    
    /**
     * @param properties
     */
    public void setProperties(Properties properties) {
        props = properties;
    }
    
}
