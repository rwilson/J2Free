/**
 * TextToJpegFactory.java
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.util;

import java.awt.*;
import java.awt.font.*;
import java.awt.image.*;
import java.io.*;
import com.sun.image.codec.jpeg.*;


/**
 *
 * @author ryan
 */
public class TextToJpegFactory
{
    
    private static final Color  BG_COLOR   = Color.white;
    private static final Color  FG_COLOR   = Color.black;
    private static final int    FONT_SIZE  = 12;
    private static final int    FONT_STYLE = Font.PLAIN;
    private static final String FONT_NAME  = "Trebuchet MS";
        
    private static BufferedImage getBufferedImage(String text, Color bgColor, Color fgColor, int fontSize, int fontStyle, String fontName)   {
        FontMetrics fm;
        BufferedImage img = new BufferedImage(1000, 200, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g2 = img.createGraphics();
        g2.setBackground(bgColor);
        g2.clearRect(0,0, 1000,200);
        g2.setColor(fgColor);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        
        Font font = new Font(fontName, fontStyle, fontSize);
        g2.setFont(font);
        
        fm = g2.getFontMetrics();
        
        float ascent = fm.getAscent();
        
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout tl = new TextLayout(text, font, frc);
        
        float descent = tl.getDescent();
        float advance = tl.getAdvance();

        tl.draw(g2, 0.0f, (float)ascent);
        
        BufferedImage img2 = img.getSubimage(0, 0, (int)advance, (int)(ascent+descent));

        g2.dispose();
        
        return img2;
    }
    
    /**
     * 
     * @param out
     * @param text
     * @throws IOException
     */
    public static void encodeJpeg(OutputStream out, String text) throws IOException
    {
        BufferedImage img = getBufferedImage(text,BG_COLOR,FG_COLOR,FONT_SIZE,FONT_STYLE,FONT_NAME);
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);
        param.setQuality(0.75f, true);
        encoder.encode(img, param);
    }
    
    /**
     *
     * @param out
     * @param text
     * @param bgColor
     * @param fgColor
     * @param fontSize
     * @param fontStyle
     * @param fontName
     * @throws IOException
     */
    public static void encodeJpeg(OutputStream out, String text, Color bgColor, Color fgColor, int fontSize, int fontStyle, String fontName)
        throws IOException {
        BufferedImage img = getBufferedImage(text,bgColor,fgColor,fontSize,fontStyle,fontName);
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);
        param.setQuality(0.75f, true);
        encoder.encode(img, param);
    }

}