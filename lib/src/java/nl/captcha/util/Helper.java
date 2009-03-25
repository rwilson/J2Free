/*
 * Created on Sep 10, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.captcha.util;

import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.StringTokenizer;

import nl.captcha.obscurity.BackgroundProducer;
import nl.captcha.obscurity.GimpyEngine;
import nl.captcha.obscurity.NoiseProducer;
import nl.captcha.obscurity.imp.DefaultBackgroundImp;
import nl.captcha.obscurity.imp.DefaultNoiseImp;
import nl.captcha.obscurity.imp.WaterRiple;
import nl.captcha.servlet.CaptchaProducer;
import nl.captcha.servlet.Constants;
import nl.captcha.servlet.DefaultCaptchaIml;
import nl.captcha.text.TextProducer;
import nl.captcha.text.WordRenederer;
import nl.captcha.text.imp.DefaultTextCreator;
import nl.captcha.text.imp.DefaultWordRenderer;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Helper {
    
    
    private static Font[] defaultFonts =  new Font[]{
        new Font("Arial", Font.BOLD, 40),
        new Font("Courier", Font.BOLD, 40)
    };
    
    public static Font[] getFonts(Properties props) {
        
        
        if (props == null)return Helper.defaultFonts;
        
        String fontArr = props.getProperty(Constants.SIMPLE_CAPTCHA_TEXTPRODUCER_FONT_FAMILY);
        if (fontArr == null)return Helper.defaultFonts;
        int fontsize = Helper.getIntegerFromString(props,Constants.SIMPLE_CAPTCHA_TEXTPRODUCER_FONT_SIZE );
        if (fontsize < 8 ) fontsize = 40;
        Font[] fonts = null;
        try {
            
            StringTokenizer tokeniz = new StringTokenizer(fontArr, ",");
            fonts = new Font[tokeniz.countTokens()];
            int cnt = 0;
            while (tokeniz.hasMoreElements()){
                String fontStr = tokeniz.nextToken();
                Font itf = new Font(fontStr,Font.BOLD,fontsize);
                fonts[cnt] = itf;
                cnt++;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        
        if (fonts == null) {
            return Helper.defaultFonts;
        } else {
            return fonts;
        }
        
        
    }
    
    
    
    public static int getIntegerFromString(Properties props, String key){
        int ret= 0;
        if (props == null) return ret;
        String val = props.getProperty(key);
        if (val == null || val.equals("")) return ret;
        
        try {
            ret = Integer.parseInt(val);
        }catch (Exception e) {
            // TODO: handle exception
        }
        return ret;
    }
    
    private static Color getRGBColor(String rgbalpha){
        
        Color color = null;
        
        try {
            
            StringTokenizer tok = new StringTokenizer(rgbalpha, ",");
            
            if (tok.countTokens() < 3  ) {
                return null;
            }
            
            int red   = Integer.parseInt( (String)tok.nextElement() );
            int green = Integer.parseInt( (String)tok.nextElement() );
            int blue  = Integer.parseInt( (String)tok.nextElement() );
            
            if (tok.countTokens() == 1 ) {
                int a = Integer.parseInt( (String)tok.nextElement() );
                color = new Color(red,green,blue,a);
            } else {
                color = new Color(red,green,blue);
            }
            
        } catch (Exception e) { }
        
        return color;
    }
    
    public static Color getColor(String colorString, Color defaultColor){
        
        Color color = null;
        
        try {
            if (colorString != null && !colorString.equals("")) {
                if (colorString.indexOf(",") > 0 ) {
                    color = Helper.getRGBColor(colorString);
                } else if (colorString.indexOf("0x") != -1) {
                    color = Color.decode(colorString);
                } else {
                    Field field = Class.forName("java.awt.Color").getField(colorString);
                    color = (Color)field.get(null);
                }
                
            }
        } catch (Exception e) { 
            e.printStackTrace();
        }
        
        if (color == null) color = defaultColor;
        
        return color == null ? Color.black : color;
    }
    
    public final static class ThingFactory{
        
        private String defaultNoiceImpcl = "nl.captcha.obscurity.DefaultNoiseImp";
        public final static int NOICEIMP = 1;
        public final static int OBSIMP = 2;
        public final static int BGIMP = 3;
        public final static int WRDREN = 4;
        public final static int TXTPRDO = 5;
        public final static int CPROD = 6;
        
        
        public static Object loadImpl(int type, Properties props) {
            switch (type) {
                case NOICEIMP :
                    String nimp = props.getProperty(Constants.SIMPLE_CAPTCHA_NOISE_IMP);
                    if (nimp == null ) return new DefaultNoiseImp(props);
                    try {
                        NoiseProducer nop = (NoiseProducer)Class.forName(nimp).newInstance();
                        nop.setProperties(props);
                        return nop;
                        
                    }catch (Exception e) {
                        System.out.println(e.getMessage());
                        return new DefaultNoiseImp(props);
                    }
                    
                case OBSIMP:
                    String obs = props.getProperty(Constants.SIMPLE_CAPTCHA_OBSCURIFICATOR);
                    if (obs == null)return new WaterRiple(props);
                    try {
                        GimpyEngine gimp = (GimpyEngine) Class.forName(obs).newInstance();
                        gimp.setProperties(props);
                        return gimp;
                    }catch (Exception e) {
                        System.out.print(e.getMessage());
                        return new WaterRiple(props);
                    }
                case BGIMP:
                    String bg = props.getProperty(Constants.SIMPLE_CAPTCHA_BACKGROUND_IMP);
                    if (bg == null)return new DefaultBackgroundImp(props);
                    try {
                        BackgroundProducer gimp = (BackgroundProducer) Class.forName(bg).newInstance();
                        gimp.setProperties(props);
                        return gimp;
                    }catch (Exception e) {
                        System.out.println(e.getMessage());
                        return new DefaultBackgroundImp(props);
                    }
                    
                case WRDREN:
                    String wr = props.getProperty(Constants.SIMPLE_CAPTCHA_WORDRENERER);
                    if (wr == null)return new DefaultWordRenderer(props);
                    try {
                        WordRenederer ren = (WordRenederer) Class.forName(wr).newInstance();
                        ren.setProperties(props);
                        return ren;
                    }catch (Exception e) {
                        System.out.println(e.getMessage());
                        return new DefaultWordRenderer(props);
                    }
                case TXTPRDO:
                    String txp = props.getProperty(Constants.SIMPLE_CAPCHA_TEXTPRODUCER);
                    if (txp == null)return new DefaultTextCreator(props);
                    try {
                        TextProducer txpP = (TextProducer) Class.forName(txp).newInstance();
                        txpP.setProperties(props);
                        return txpP;
                    }catch (Exception e) {
                        System.out.println(e.getMessage());
                        return new DefaultTextCreator(props);
                    }
                case CPROD:
                    String cp = props.getProperty(Constants.SIMPLE_CAPTCHA_PRODUCER);
                    if (cp == null) return new DefaultCaptchaIml(props);
                    try {
                        CaptchaProducer p = (CaptchaProducer)Class.forName(cp).newInstance();
                        p.setProperties(props);
                    }catch (Exception e) {
                        System.out.println(e.getMessage());
                        return new DefaultCaptchaIml(props);
                    }
                    
                    
                default :
                    break;
            }
            return null;
        }
    }
    
}
