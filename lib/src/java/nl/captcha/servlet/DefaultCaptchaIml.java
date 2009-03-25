/*
 * Created on Jul 10, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.captcha.servlet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Random;

import nl.captcha.obscurity.BackgroundProducer;
import nl.captcha.obscurity.GimpyEngine;
import nl.captcha.text.TextProducer;
import nl.captcha.text.WordRenederer;
import nl.captcha.util.Helper;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import nl.captcha.obscurity.imp.WaterRiple;
import nl.captcha.sandbox.FishEyeGimpyImp;

/**
 * @author testvoogd@hotmail.com //logged in as Administrator
 *
 * Source and binaries subject to copyright.
 *
 * @modified by ryanwilson.m@gmail.com
 */
public class DefaultCaptchaIml implements CaptchaProducer  {
    
    private Properties props = null;
    
    private boolean drawBorder  = true;
    private int     borderWidth = 1;
    private Color   borderColor = Color.black;
     
    private WordRenederer      wordRenderer        =  null;
    private GimpyEngine        gimpyEngine         = null;
    private BackgroundProducer backgroundGenerator = null;
    private TextProducer       textProducer        = null;
    
    private int imageWidth  = 200;
    private int imageHeight = 50;
    
    private boolean drawNoise = true;

    public DefaultCaptchaIml(Properties props) {
        this.props = props;
        if (this.props != null) {
            
            String tempProperty;
            
            //doing some init stuff.
            tempProperty = props.getProperty(Constants.SIMPLE_CAPTCHA_BORDER_ENABLED);
            if (tempProperty != null) try { this.drawBorder = Boolean.parseBoolean(tempProperty); } catch (Exception e) { }
            
            if (drawBorder) {
                borderColor = Helper.getColor(this.props.getProperty(Constants.SIMPLE_CAPTCHA_BORDER_COLOR), Color.black);
                borderWidth = Helper.getIntegerFromString(props,Constants.SIMPLE_CAPTCHA_BORDER_WIDTH);
                if (borderWidth == 0) borderWidth = 1;
            }
            
            this.gimpyEngine         = (GimpyEngine)        Helper.ThingFactory.loadImpl(Helper.ThingFactory.OBSIMP, props);
            this.backgroundGenerator = (BackgroundProducer) Helper.ThingFactory.loadImpl(Helper.ThingFactory.BGIMP, props);
            this.wordRenderer        = (WordRenederer)      Helper.ThingFactory.loadImpl(Helper.ThingFactory.WRDREN, props);
            this.textProducer        = (TextProducer)       Helper.ThingFactory.loadImpl(Helper.ThingFactory.TXTPRDO, props);
            
            tempProperty = props.getProperty(Constants.SIMPLE_CAPTCHA_IMAGE_WIDTH);
            if (tempProperty != null) { 
                try { 
                    this.imageWidth = Integer.parseInt(tempProperty); 
                } catch (Exception e) { 
                    if (tempProperty.equals("auto")) {
                        this.imageWidth = -1;
                    }
                } 
            }
            
            tempProperty = props.getProperty(Constants.SIMPLE_CAPTCHA_IMAGE_HEIGHT);
            if (tempProperty != null) try { this.imageHeight = Integer.parseInt(tempProperty); } catch (Exception e) { }
            
            // The FishEye noise is a grid, instead of a slash mark across the image
            if (gimpyEngine instanceof FishEyeGimpyImp) {
                tempProperty = props.getProperty(Constants.SIMPLE_CAPTCHA_FISH_EYE_GRID_ENABLED);
                if (tempProperty != null) try { this.drawNoise = Boolean.parseBoolean(tempProperty); } catch (Exception e) { }
            } else {
                tempProperty = props.getProperty(Constants.SIMPLE_CAPTCHA_NOISE_ENABLED);
                if (tempProperty != null) try { this.drawNoise = Boolean.parseBoolean(tempProperty); } catch (Exception e) { }
            }
        }
    }
    
    /**
     * Create an image which have witten a distorted text, text given
     * as parameter. The result image is put on the output stream
     *
     * @param stream the OutputStrea where the image is written
     * @param text the distorted characters written on imagage
     * @throws IOException if an error occurs during the image written on
     * output stream.
     */
    public void createImage(OutputStream stream, String text)
    throws IOException {
        
        //create an JPEG encoder
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(stream);
        
        BufferedImage image;
        
        //put the text on the image
        if (gimpyEngine instanceof WaterRiple)
            image = wordRenderer.renderWord(text,imageWidth,imageHeight,-5);
        else
            image = wordRenderer.renderWord(text,imageWidth,imageHeight,0);
        
        this.imageWidth = image.getWidth();
        
        //create a new distorted (wound version of) the image
        gimpyEngine.setProperties(props);
        image = gimpyEngine.getDistortedImage(image,this.drawNoise);
        
        //add a background to the image
        image = this.backgroundGenerator.addBackground(image);
        
        //get the graphics of the image
        Graphics2D graphics = image.createGraphics();
        
        if (drawBorder) drawBox(graphics);
        
        //encode the image to jpeg format
        JPEGEncodeParam param =  encoder.getDefaultJPEGEncodeParam(image);
        param.setQuality(1f,true);
        encoder.encode(image,param);
        
        //encoder.encode(image);
    }
    
    /**
     * Rotate an image from it's center.
     *
     * @param The image to be rotated.
     * @return The rotated image.
     */
    private static BufferedImage rotate(BufferedImage image) {
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        //create a clean transparent image
        BufferedImage transform =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2Dx = (Graphics2D)transform.getGraphics();
        AffineTransform xform = g2Dx.getTransform();
        g2Dx.setBackground(Color.white);
        g2Dx.setColor(Color.white);
        int xRot = width / 2;
        int yRot = height / 2;
        
        Random rand = new Random();
        
        // generate an angle between 5 and -5 degrees.
        int angle = rand.nextInt(5) + 2;
        
        int ori = rand.nextInt(2);
        
        if (ori <  1 ) angle = angle * -1;
        
        //rotate the image
        xform.rotate(Math.toRadians(angle), xRot, yRot);
        
        g2Dx.setTransform(xform);
        g2Dx.drawImage(image, 0, 0, null,  null);
        
        return transform;
    }
    
    private void drawBox(Graphics2D graphics) {
        
        //#d0 a0 3f;
        
        graphics.setColor(this.borderColor);
        
        if (this.borderWidth != 1) {
            BasicStroke stroke = new BasicStroke((float)borderWidth);
            graphics.setStroke(stroke);
        }
        
        Line2D d2 =  new Line2D.Double((double)0,(double)0,(double)0,(double)imageWidth );
        graphics.draw(d2);
        
        Line2D d3 =  new Line2D.Double((double)0,(double)0,(double)imageWidth,(double)0);
        graphics.draw(d3);
        
        d3 =  new Line2D.Double((double)0,(double)imageHeight-1,(double)imageWidth,(double)imageHeight-1);
        graphics.draw(d3);
        
        d3 =  new Line2D.Double((double)imageWidth-1,(double)imageHeight-1,(double)imageWidth-1,(double)0);
        
        graphics.draw(d3);
        
    }

    /* (non-Javadoc)
     * @see nl.captcha.servlet.CaptchaProducer#setBackGroundImageProducer(nl.captcha.obscurity.BackgroundProducer)
     */
    public void setBackGroundImageProducer(BackgroundProducer background) {
        this.backgroundGenerator = background;
    }
    
    /* (non-Javadoc)
     * @see nl.captcha.servlet.CaptchaProducer#setObscurificator()
     */
    public void setObscurificator() {
        // TODO Auto-generated method stub
    }
    
    /**
     * @return
     */
    public Properties getProperties() {
        return props;
    }
    
    /**
     * @param properties
     */
    public void setProperties(Properties properties) {
        props = properties;
    }
    
    /* (non-Javadoc)
     * @see nl.captcha.servlet.CaptchaProducer#setObscurificator(nl.captcha.obscurity.GimpyEngine)
     */
    public void setObscurificator(GimpyEngine engine) {
        this.gimpyEngine = engine;
        
    }
    
    /* (non-Javadoc)
     * @see nl.captcha.servlet.CaptchaProducer#setTextProducer(nl.captcha.text.TextProducer)
     */
    public void setTextProducer(TextProducer textP) {
        this.textProducer = textP;
        
    }
    
    public String createText(){
        String capText = textProducer.getText();
        return capText;
    }
    
    /**
     * @param renederer
     */
    public void setWordRenderer(WordRenederer renederer) {
        wordRenderer = renederer;
    }
    
}
